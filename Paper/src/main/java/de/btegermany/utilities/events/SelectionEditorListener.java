package de.btegermany.utilities.events;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.regions.selector.Polygonal2DRegionSelector;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import de.btegermany.utilities.BTEGUtilities;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Handles poly point editing, cuboid expansion, and selection move/stack controls. */
public class SelectionEditorListener implements Listener, CommandExecutor, TabCompleter {

    private static final double MAX_DISTANCE = 6.0;
    private static final double MAX_POINT_DISTANCE_SQUARED = 0.65 * 0.65;
    private static final long MAX_PREVIEW_BLOCKS = 100_000;
    private final Map<UUID, EditState> editStates = new HashMap<>();

    @EventHandler
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        EditState active = editStates.get(player.getUniqueId());
        if (active != null) {
            event.setCancelled(true);
            flushScrollInputs(player, active);
            active.mode = switch (active.mode) {
                case MOVE -> {
                    Arrays.fill(active.move, 0);
                    Arrays.fill(active.copy, 0);
                    active.transforms.clear();
                    yield Mode.COPY;
                }
                case COPY -> {
                    Arrays.fill(active.copy, 0);
                    Arrays.fill(active.move, 0);
                    Arrays.fill(active.stack, 0);
                    active.transforms.clear();
                    yield Mode.STACK;
                }
                case STACK, NORMAL -> Mode.NORMAL;
            };
            if (active.mode == Mode.NORMAL) {
                stopEditing(player, active);
            } else {
                showMode(player, active);
                refreshPreview(player, active, true);
            }
            return;
        }

        LocalSession session = getSession(player);
        if (session == null) return;
        Region region = getSelection(session);
        if (region == null) return;
        if (!(region instanceof CuboidRegion) && !(region instanceof Polygonal2DRegion)) return;

        if (region instanceof CuboidRegion cuboid) {
            org.bukkit.block.Block target = player.getTargetBlockExact((int) MAX_DISTANCE);
            if (target != null && !cuboid.contains(BlockVector3.at(target.getX(), target.getY(), target.getZ()))) {
                expandCuboid(event, player, session, cuboid);
                if (event.isCancelled()) return;
            }
        } else {
            Polygonal2DRegion polyRegion = (Polygonal2DRegion) region;
            editPolyPoint(event, player, session, polyRegion);
            if (event.isCancelled()) return;
        }

        // Holding the standard WorldEdit wand enters the two-mode editor.
        if (player.getInventory().getItemInMainHand().getType() == Material.WOODEN_AXE) {
            event.setCancelled(true);
            EditState state = new EditState(session, player.getInventory().getHeldItemSlot());
            editStates.put(player.getUniqueId(), state);
            showMode(player, state);
            startPreview(player, state);
            return;
        }

        // Keep the point reorder and cuboid expansion shortcuts available when
        // the player is not holding the wand used to enter edit mode.
        if (region instanceof CuboidRegion cuboid) expandCuboid(event, player, session, cuboid);
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        EditState state = editStates.get(event.getPlayer().getUniqueId());
        if (state == null) return;
        event.setCancelled(true);
        if (state.mode != Mode.MOVE && state.mode != Mode.COPY) return;
        if (event.getPlayer().isSneaking()) {
            int axis = dominantAxis(event.getPlayer().getEyeLocation().getDirection());
            state.transforms.add(new TransformOp(false, axis));
        } else {
            state.transforms.add(new TransformOp(true, -1));
        }
        showMode(event.getPlayer(), state);
        refreshPreview(event.getPlayer(), state, true);
    }

    private static int calculateSlotDelta(int from, int to) {
        return Math.floorMod(to - from + 4, 9) - 4;
    }

    @EventHandler
    public void onHotbarChange(PlayerItemHeldEvent event) {
        EditState state = editStates.get(event.getPlayer().getUniqueId());
        if (state == null) return;
        event.setCancelled(true);
        int delta = calculateSlotDelta(event.getPreviousSlot(), event.getNewSlot());
        scheduleSlotReset(event.getPlayer(), state);
        if (delta == 0) return;

        int axis = dominantAxis(event.getPlayer().getEyeLocation().getDirection());
        int direction = -axisDirection(event.getPlayer().getEyeLocation().getDirection(), axis) * Integer.signum(delta);
        state.pendingScrollInputs.add(new ScrollInput(axis, direction, Math.abs(delta)));
        if (state.pendingScrollTask == null) {
            state.pendingScrollTask = Bukkit.getScheduler().runTaskLater(BTEGUtilities.getPlugin(),
                    () -> flushScrollInputs(event.getPlayer(), state), 3L);
        }
    }

    private void flushScrollInputs(Player player, EditState state) {
        if (state.pendingScrollTask != null) {
            state.pendingScrollTask.cancel();
            state.pendingScrollTask = null;
        }
        if (state.pendingScrollInputs.isEmpty()) return;

        int[][] counts = new int[3][2];
        int scrollAmount = 0;
        ScrollInput latest = state.pendingScrollInputs.get(state.pendingScrollInputs.size() - 1);
        for (ScrollInput input : state.pendingScrollInputs) {
            counts[input.axis][input.direction > 0 ? 1 : 0] += input.amount;
            scrollAmount += input.amount;
        }
        int bestAxis = latest.axis;
        int bestDirection = latest.direction;
        int bestCount = counts[bestAxis][bestDirection > 0 ? 1 : 0];
        for (int axis = 0; axis < 3; axis++) {
            for (int directionIndex = 0; directionIndex < 2; directionIndex++) {
                if (counts[axis][directionIndex] > bestCount) {
                    bestAxis = axis;
                    bestDirection = directionIndex == 1 ? 1 : -1;
                    bestCount = counts[axis][directionIndex];
                }
            }
        }
        state.pendingScrollInputs.clear();

        if (state.mode == Mode.MOVE) {
            state.move[bestAxis] = Math.clamp(state.move[bestAxis] + (long) bestDirection * scrollAmount, -64, 64);
        } else if (state.mode == Mode.COPY) {
            state.copy[bestAxis] = Math.clamp(state.copy[bestAxis] + (long) bestDirection * scrollAmount, -64, 64);
        } else {
            state.stack[bestAxis] = Math.clamp(state.stack[bestAxis] + (long) bestDirection * scrollAmount, -64, 64);
        }

        showMode(player, state);
        refreshPreview(player, state, true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEditClick(PlayerInteractEvent event) {
        EditState state = editStates.get(event.getPlayer().getUniqueId());
        if (state == null) return;
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK
                && action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        event.setCancelled(true);
        flushScrollInputs(event.getPlayer(), state);
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            if (state.mode == Mode.COPY) commitCopy(event.getPlayer(), state);
            else commit(event.getPlayer(), state);
        } else {
            stopEditing(event.getPlayer(), state);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        EditState state = editStates.get(event.getPlayer().getUniqueId());
        if (state != null) stopEditing(event.getPlayer(), state);
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }
        EditState state = editStates.get(player.getUniqueId());
        if (state == null) {
            player.sendMessage("§cEnter selection edit mode before setting an edit mask.");
            return true;
        }
        if (args.length == 0) {
            state.mask = null;
            state.maskExpression = null;
            state.acceptedCopies.clear();
            refreshPreview(player, state, true);
            player.sendMessage("§aSelection edit mask cleared.");
            return true;
        }

        String expression = String.join(" ", args);
        Region selection = getSelection(state.session);
        if (selection == null) {
            player.sendMessage("§cMake a selection before setting an edit mask.");
            return true;
        }
        try {
            ParserContext context = createParserContext(player, state.session, selection);
            state.mask = WorldEdit.getInstance().getMaskFactory().parseFromInput(expression, context);
            state.maskExpression = expression;
            state.acceptedCopies.clear();
            refreshPreview(player, state, true);
            player.sendMessage("§aSelection edit mask set to: §f" + expression);
        } catch (Exception exception) {
            player.sendMessage("§cInvalid FAWE mask: " + exception.getMessage());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NonNull CommandSender sender, @NonNull Command command, @NonNull String alias, String @NonNull [] args) {
        if (!(sender instanceof Player player)) return List.of();
        EditState state = editStates.get(player.getUniqueId());
        if (state == null) return List.of();
        Region selection = getSelection(state.session);
        if (selection == null) return List.of();
        try {
            String input = String.join(" ", args);
            return WorldEdit.getInstance().getMaskFactory()
                    .getSuggestions(input, createParserContext(player, state.session, selection));
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private ParserContext createParserContext(Player player, LocalSession session, Region selection) {
        ParserContext context = new ParserContext();
        var actor = BukkitAdapter.adapt(player);
        context.setActor(actor);
        context.setSession(session);
        context.setWorld(selection.getWorld());
        context.setExtent(selection.getWorld());
        context.setTryLegacy(actor.getLimit().ALLOW_LEGACY);
        return context;
    }

    private void editPolyPoint(PlayerSwapHandItemsEvent event, Player player, LocalSession session, Polygonal2DRegion region) {
        List<BlockVector2> points = new ArrayList<>(region.getPoints());
        if (points.isEmpty()) return;
        int selectedIndex = findLookedAtPoint(player, points, region.getMinimumPoint().y());
        if (selectedIndex < 0) return;
        event.setCancelled(true);
        if (selectedIndex == points.size() - 1) points.remove(selectedIndex);
        else Collections.rotate(points, -(selectedIndex + 1));
        session.setRegionSelector(region.getWorld(), new Polygonal2DRegionSelector(
                region.getWorld(), points, region.getMinimumPoint().y(), region.getMaximumPoint().y()));
        session.dispatchCUISelection(BukkitAdapter.adapt(player));
    }

    private void expandCuboid(PlayerSwapHandItemsEvent event, Player player, LocalSession session, CuboidRegion cuboid) {
        org.bukkit.block.Block target = player.getTargetBlockExact((int) MAX_DISTANCE);
        if (target == null) return;
        BlockVector3 targetPosition = BlockVector3.at(target.getX(), target.getY(), target.getZ());
        if (cuboid.contains(targetPosition)) return;

        try {
            CuboidRegionSelector selector = (CuboidRegionSelector) session.getRegionSelector(session.getSelectionWorld());
            BlockVector3[] expanded = expandEndpoints(selector.getPrimaryPosition(), cuboid.getPos2(), targetPosition);
            event.setCancelled(true);
            session.setRegionSelector(cuboid.getWorld(), new CuboidRegionSelector(cuboid.getWorld(), expanded[0], expanded[1]));
            session.dispatchCUISelection(BukkitAdapter.adapt(player));
        } catch (Exception ignored) { }
    }

    private void commit(Player player, EditState state) {
        stopEditing(player, state);
        try {
            Region sourceRegion = state.previewSelection;
            if (!(sourceRegion instanceof CuboidRegion) && !(sourceRegion instanceof Polygonal2DRegion)) {
                throw new IllegalStateException("The current selection is missing or is no longer a cuboid or polygon.");
            }
            sourceRegion = copyRegion(sourceRegion);
            BlockVector3 movement = BlockVector3.at(state.move[0], state.move[1], state.move[2]);
            BlockVector3 dimensions = sourceRegion.getDimensions();
            BlockVector3 moveDestination = sourceRegion.getMinimumPoint().add(movement);
            List<BlockVector3> matchingSourcePositions = state.mask == null
                    ? List.of() : getMatchingPositions(sourceRegion, state.mask);
            var editSession = state.session.createEditSession(BukkitAdapter.adapt(player));
            try (editSession) {
                BlockArrayClipboard clipboard = createClipboard(sourceRegion, state.mask);
                boolean transformed = hasBlockTransform(state);
                Map<BlockVector3, BlockData> rotatedBlocks = !transformed ? Map.of()
                        : captureBlocks(sourceRegion, state.mask);

                if (!movement.equals(BlockVector3.ZERO) || transformed) {
                    if (state.mask == null) {
                        editSession.setBlocks(sourceRegion, BlockTypes.AIR.getDefaultState());
                    } else {
                        for (BlockVector3 position : matchingSourcePositions)
                            editSession.setBlock(position, BlockTypes.AIR.getDefaultState());
                    }
                    if (!transformed) {
                        if (!movement.equals(BlockVector3.ZERO)) {
                            pasteAt(editSession, clipboard, moveDestination, state.mask != null);
                        }
                    } else {
                        for (Map.Entry<BlockVector3, BlockData> block : rotatedBlocks.entrySet()) {
                            BlockVector3 destination = transformPosition(block.getKey(), sourceRegion, state).add(movement);
                            editSession.setBlock(destination, BukkitAdapter.adapt(transformBlockData(block.getValue(), state)));
                        }
                    }
                }

                List<Integer> xOffsets = stackOffsets(state.stack[0], dimensions.x());
                List<Integer> yOffsets = stackOffsets(state.stack[1], dimensions.y());
                List<Integer> zOffsets = stackOffsets(state.stack[2], dimensions.z());
                for (int x : xOffsets) for (int y : yOffsets) for (int z : zOffsets) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    pasteAt(editSession, clipboard, moveDestination.add(x, y, z), state.mask != null);
                }
            }
            player.getServer().getScheduler().runTaskLater(BTEGUtilities.getPlugin(), () -> {
                state.session.remember(editSession);
                state.session.save();
            }, 5L);

            if (!movement.equals(BlockVector3.ZERO)) {
                RegionSelector selector = shiftedSelector(sourceRegion, movement);
                state.session.setRegionSelector(sourceRegion.getWorld(), selector);
                state.session.dispatchCUISelection(BukkitAdapter.adapt(player));
            }
        } catch (RuntimeException exception) {
            player.sendMessage("§cCould not apply the selection edit: " + exception.getMessage());
        }
    }

    private void startPreview(Player player, EditState state) {
        refreshPreview(player, state, true);
        state.selectionWatcher = Bukkit.getScheduler().runTaskTimer(BTEGUtilities.getPlugin(), () -> {
            if (editStates.get(player.getUniqueId()) != state) {
                if (state.selectionWatcher != null) state.selectionWatcher.cancel();
                return;
            }
            refreshPreview(player, state, false);
        }, 1L, 2L);
    }

    private void scheduleSlotReset(Player player, EditState state) {
        if (state.slotResetScheduled) return;
        state.slotResetScheduled = true;
        Bukkit.getScheduler().runTask(BTEGUtilities.getPlugin(), () -> {
            state.slotResetScheduled = false;
            if (editStates.get(player.getUniqueId()) != state || !player.isOnline()) return;
            player.getInventory().setHeldItemSlot(state.lockedSlot);
            player.updateInventory();
            state.lastVirtualSlot = state.lockedSlot;
        });
    }

    private void refreshPreview(Player player, EditState state, boolean force) {
        Region selection = state.suppressSelectionRefresh && state.previewSelection != null
                ? copyRegion(state.previewSelection) : getSelection(state.session);
        if (!(selection instanceof CuboidRegion) && !(selection instanceof Polygonal2DRegion)) {
            selection = null;
        }
        Object signature = selectionSignature(selection);
        if (!force && Objects.equals(signature, state.selectionSignature)) return;
        if (state.selectionSignature != null && !Objects.equals(signature, state.selectionSignature)) {
            state.acceptedCopies.clear();
        }
        state.selectionSignature = signature;
        state.previewSelection = selection == null ? null : copyRegion(selection);

        Object previewService = getPreviewService();
        if (previewService == null) return;

        Map<Location, BlockData> changes = Map.of();
        boolean exceededLimit = false;
        if (selection != null) {
            try {
                changes = createPreviewChanges(selection, state);
            } catch (PreviewLimitException exception) {
                exceededLimit = true;
            } catch (RuntimeException exception) {
                player.sendMessage("§cCould not evaluate the selection edit mask: " + exception.getMessage());
            }
        }
        try {
            if (state.previewHandle == null) {
                var show = previewService.getClass().getMethod("show", Player.class, Map.class);
                state.previewHandle = show.invoke(previewService, player, changes);
            } else {
                invokePreviewHandle(state.previewHandle, "update", changes);
            }
        } catch (ReflectiveOperationException exception) {
            BTEGUtilities.getPlugin().getLogger().warning("Could not update the SchematicBrush preview: " + exception.getMessage());
            closePreview(state);
        }
        if (exceededLimit) player.sendMessage("§cPreview is too large to display; the edit can still be applied.");
        else showMode(player, state);
    }

    private Map<Location, BlockData> createPreviewChanges(Region region, EditState state) {
        BlockVector3 baseOffset = switch (state.mode) {
            case MOVE, STACK -> BlockVector3.at(state.move[0], state.move[1], state.move[2]);
            case COPY -> BlockVector3.at(state.copy[0], state.copy[1], state.copy[2]);
            case NORMAL -> BlockVector3.ZERO;
        };
        if (state.mode == Mode.COPY && (baseOffset.equals(BlockVector3.ZERO) || state.acceptedCopies.contains(baseOffset))) {
            return Map.of();
        }

        BlockVector3 dimensions = region.getDimensions();
        List<Integer> xOffsets = state.mode == Mode.STACK ? stackOffsets(state.stack[0], dimensions.x()) : List.of(0);
        List<Integer> yOffsets = state.mode == Mode.STACK ? stackOffsets(state.stack[1], dimensions.y()) : List.of(0);
        List<Integer> zOffsets = state.mode == Mode.STACK ? stackOffsets(state.stack[2], dimensions.z()) : List.of(0);
        long translations = (long) xOffsets.size() * yOffsets.size() * zOffsets.size();
        long volume = region.getVolume();
        long previewBlockCount = volume * translations;
        if (translations > 1 && volume > MAX_PREVIEW_BLOCKS / translations) throw new PreviewLimitException();
        if (previewBlockCount > MAX_PREVIEW_BLOCKS) throw new PreviewLimitException();

        Map<BlockVector3, BlockData> source = new HashMap<>();
        for (BlockVector3 position : region) {
            BlockVector3 stablePosition = BlockVector3.at(position.x(), position.y(), position.z());
            if (state.mask != null && !state.mask.test(stablePosition)) continue;
            source.put(stablePosition, BukkitAdapter.adapt(region.getWorld().getBlock(stablePosition)));
        }

        org.bukkit.World world = BukkitAdapter.adapt(region.getWorld());
        Map<Location, BlockData> changes = new HashMap<>();
        BlockData air = Material.AIR.createBlockData();
        boolean moving = state.mode == Mode.MOVE || state.mode == Mode.STACK;
        if (moving && !baseOffset.equals(BlockVector3.ZERO)) {
            for (BlockVector3 position : source.keySet()) {
                changes.put(toLocation(world, position), air);
            }
        }

        for (int x : xOffsets) for (int y : yOffsets) for (int z : zOffsets) {
            if (x == 0 && y == 0 && z == 0 && baseOffset.equals(BlockVector3.ZERO)) continue;
            BlockVector3 offset = baseOffset.add(x, y, z);
            for (Map.Entry<BlockVector3, BlockData> block : source.entrySet()) {
                BlockVector3 position = block.getKey();
                BlockData data = block.getValue();
                if ((state.mode == Mode.MOVE || state.mode == Mode.COPY) && hasBlockTransform(state)) {
                    position = transformPosition(position, region, state);
                    data = transformBlockData(data, state);
                }
                changes.put(toLocation(world, position.add(offset)), data);
            }
        }
        return changes;
    }

    private Map<BlockVector3, BlockData> captureBlocks(Region region, Mask mask) {
        Map<BlockVector3, BlockData> blocks = new HashMap<>();
        for (BlockVector3 position : region) {
            BlockVector3 stablePosition = BlockVector3.at(position.x(), position.y(), position.z());
            if (mask != null && !mask.test(stablePosition)) continue;
            blocks.put(stablePosition, BukkitAdapter.adapt(region.getWorld().getBlock(stablePosition)));
        }
        return blocks;
    }

    private BlockVector3 transformPosition(BlockVector3 position, Region region, EditState state) {
        int x = position.x();
        int y = position.y();
        int z = position.z();
        int sumX = region.getMinimumPoint().x() + region.getMaximumPoint().x();
        int sumZ = region.getMinimumPoint().z() + region.getMaximumPoint().z();
        for (TransformOp operation : state.transforms) {
            if (!operation.rotation) {
                switch (operation.axis) {
                    case 0 -> x = sumX - x;
                    case 1 -> y = region.getMinimumPoint().y() + region.getMaximumPoint().y() - y;
                    case 2 -> z = sumZ - z;
                    default -> throw new IllegalStateException("Unknown flip axis");
                }
            } else {
                int rotatedX = Math.floorDiv(sumX + sumZ - 2 * z, 2);
                z = Math.floorDiv(sumZ + 2 * x - sumX, 2);
                x = rotatedX;
            }
        }
        return BlockVector3.at(x, y, z);
    }

    private BlockData transformBlockData(BlockData blockData, EditState state) {
        BlockData transformed = blockData.clone();
        for (TransformOp operation : state.transforms) {
            if (operation.rotation) {
                transformed.rotate(StructureRotation.CLOCKWISE_90);
            } else if (operation.axis == 0) {
                transformed.mirror(Mirror.FRONT_BACK);
            } else if (operation.axis == 2) {
                transformed.mirror(Mirror.LEFT_RIGHT);
            } else if (operation.axis == 1) {
                if (transformed instanceof Bisected bisected) {
                    bisected.setHalf(bisected.getHalf() == Bisected.Half.TOP ? Bisected.Half.BOTTOM : Bisected.Half.TOP);
                }
                if (transformed instanceof Directional directional) {
                    if (directional.getFacing() == BlockFace.UP) directional.setFacing(BlockFace.DOWN);
                    else if (directional.getFacing() == BlockFace.DOWN) directional.setFacing(BlockFace.UP);
                }
            }
        }
        return transformed;
    }

    private boolean hasBlockTransform(EditState state) {
        return !state.transforms.isEmpty();
    }

    private void commitCopy(Player player, EditState state) {
        Region sourceRegion = state.previewSelection;
        if (!(sourceRegion instanceof CuboidRegion) && !(sourceRegion instanceof Polygonal2DRegion)) {
            player.sendMessage("§cThe current selection is missing or is no longer a cuboid or polygon.");
            return;
        }
        sourceRegion = copyRegion(sourceRegion);
        restoreCopySelection(player, state, sourceRegion);
        BlockVector3 offset = BlockVector3.at(state.copy[0], state.copy[1], state.copy[2]);
        if (offset.equals(BlockVector3.ZERO) || state.acceptedCopies.contains(offset)) {
            player.sendMessage("§eNo new copy at this position. Scroll to place another copy.");
            return;
        }

        try {
            var editSession = state.session.createEditSession(BukkitAdapter.adapt(player));
            try (editSession) {
                if (!hasBlockTransform(state)) {
                    BlockArrayClipboard clipboard = createClipboard(sourceRegion, state.mask);
                    pasteAt(editSession, clipboard, sourceRegion.getMinimumPoint().add(offset), state.mask != null);
                } else {
                    Map<BlockVector3, BlockData> blocks = captureBlocks(sourceRegion, state.mask);
                    for (Map.Entry<BlockVector3, BlockData> block : blocks.entrySet()) {
                        BlockVector3 destination = transformPosition(block.getKey(), sourceRegion, state).add(offset);
                        editSession.setBlock(destination, BukkitAdapter.adapt(transformBlockData(block.getValue(), state)));
                    }
                }
            }
            rememberEdit(player, state.session, editSession);
            state.acceptedCopies.add(offset);
            refreshPreview(player, state, true);
        } catch (RuntimeException exception) {
            player.sendMessage("§cCould not place the copy: " + exception.getMessage());
        }
    }

    private void restoreCopySelection(Player player, EditState state, Region selection) {
        state.suppressSelectionRefresh = true;
        state.previewSelection = copyRegion(selection);
        state.session.setRegionSelector(selection.getWorld(), shiftedSelector(selection, BlockVector3.ZERO));
        state.session.dispatchCUISelection(BukkitAdapter.adapt(player));
        if (state.selectionRestoreTask != null) state.selectionRestoreTask.cancel();
        state.selectionRestoreTask = Bukkit.getScheduler().runTaskLater(BTEGUtilities.getPlugin(), () -> {
            if (editStates.get(player.getUniqueId()) != state || !player.isOnline()) return;
            state.session.setRegionSelector(selection.getWorld(), shiftedSelector(selection, BlockVector3.ZERO));
            state.session.dispatchCUISelection(BukkitAdapter.adapt(player));
            state.suppressSelectionRefresh = false;
            refreshPreview(player, state, true);
            state.selectionRestoreTask = null;
        }, 1L);
    }

    private BlockArrayClipboard createClipboard(Region region, Mask mask) {
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        clipboard.setOrigin(region.getMinimumPoint());
        ForwardExtentCopy copy = new ForwardExtentCopy(region.getWorld(), region,
                region.getMinimumPoint(), clipboard, region.getMinimumPoint());
        copy.setCopyingEntities(false);
        if (mask != null) copy.setSourceMask(mask);
        Operations.complete(copy);
        return clipboard;
    }

    private List<BlockVector3> getMatchingPositions(Region region, Mask mask) {
        List<BlockVector3> positions = new ArrayList<>();
        for (BlockVector3 position : region) {
            BlockVector3 stablePosition = BlockVector3.at(position.x(), position.y(), position.z());
            if (mask.test(stablePosition)) positions.add(stablePosition);
        }
        return positions;
    }

    private void rememberEdit(Player player, LocalSession session, com.sk89q.worldedit.EditSession editSession) {
        player.getServer().getScheduler().runTaskLater(BTEGUtilities.getPlugin(), () -> {
            session.remember(editSession);
            session.save();
        }, 5L);
    }

    private Location toLocation(org.bukkit.World world, BlockVector3 position) {
        return new Location(world, position.x(), position.y(), position.z());
    }

    private Object selectionSignature(Region region) {
        if (region == null) return null;
        Object points = region instanceof Polygonal2DRegion poly ? List.copyOf(poly.getPoints()) : null;
        return List.of(region.getWorld(), region.getClass(), region.getMinimumPoint(), region.getMaximumPoint(), points == null ? List.of() : points);
    }

    private Object getPreviewService() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("SchematicBrushReborn");
        if (plugin == null || !plugin.isEnabled()) return null;
        try {
            return plugin.getClass().getMethod("previews").invoke(plugin);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private void invokePreviewHandle(Object handle, String method, Map<Location, BlockData> changes) throws ReflectiveOperationException {
        Class<?> previewHandleApi = handle.getClass().getInterfaces()[0];
        previewHandleApi.getMethod(method, Map.class).invoke(handle, changes);
    }

    private void closePreview(EditState state) {
        if (state.previewHandle == null) return;
        try {
            Class<?> previewHandleApi = state.previewHandle.getClass().getInterfaces()[0];
            previewHandleApi.getMethod("close").invoke(state.previewHandle);
        } catch (ReflectiveOperationException exception) {
            BTEGUtilities.getPlugin().getLogger().warning("Could not close the SchematicBrush preview: " + exception.getMessage());
        } finally {
            state.previewHandle = null;
        }
    }

    private void stopEditing(Player player, EditState state) {
        editStates.remove(player.getUniqueId());
        if (state.pendingScrollTask != null) state.pendingScrollTask.cancel();
        state.pendingScrollInputs.clear();
        if (state.selectionWatcher != null) state.selectionWatcher.cancel();
        if (state.selectionRestoreTask != null) state.selectionRestoreTask.cancel();
        closePreview(state);
        if (state.modeBar != null) {
            state.modeBar.removeAll();
            state.modeBar.setVisible(false);
            state.modeBar = null;
        }
        if (player.isOnline()) {
            player.getInventory().setHeldItemSlot(state.lockedSlot);
            player.updateInventory();
        }
    }

    private static final class PreviewLimitException extends RuntimeException { }

    private void pasteAt(com.sk89q.worldedit.EditSession editSession, BlockArrayClipboard clipboard,
                         BlockVector3 destination, boolean ignoreAir) {
            Operations.complete(new ClipboardHolder(clipboard)
                .createPaste(editSession).to(destination).ignoreAirBlocks(ignoreAir).build());
    }

    private List<Integer> stackOffsets(int count, int dimension) {
        List<Integer> offsets = new ArrayList<>();
        int direction = Integer.signum(count);
        for (int i = 0; i <= Math.abs(count); i++) offsets.add(i * dimension * direction);
        return offsets;
    }

    private RegionSelector shiftedSelector(Region region, BlockVector3 offset) {
        World world = region.getWorld();
        if (region instanceof CuboidRegion cuboid) {
            return new CuboidRegionSelector(world, cuboid.getPos1().add(offset), cuboid.getPos2().add(offset));
        }
        Polygonal2DRegion poly = (Polygonal2DRegion) region;
        List<BlockVector2> points = poly.getPoints().stream()
                .map(point -> BlockVector2.at(point.x() + offset.x(), point.z() + offset.z())).toList();
        return new Polygonal2DRegionSelector(world, points,
                poly.getMinimumPoint().y() + offset.y(), poly.getMaximumPoint().y() + offset.y());
    }

    private Region copyRegion(Region region) {
        return region.clone();
    }

    private LocalSession getSession(Player player) {
        return WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));
    }

    private Region getSelection(LocalSession session) {
        try { return session.getSelection(session.getSelectionWorld()); }
        catch (Exception ignored) { return null; }
    }

    private BlockVector3[] expandEndpoints(BlockVector3 pos1, BlockVector3 pos2, BlockVector3 target) {
        int[] first = {pos1.x(), pos1.y(), pos1.z()};
        int[] second = {pos2.x(), pos2.y(), pos2.z()};
        int[] targetCoordinates = {target.x(), target.y(), target.z()};
        for (int axis = 0; axis < 3; axis++) {
            int minimum = Math.min(first[axis], second[axis]), maximum = Math.max(first[axis], second[axis]);
            if (targetCoordinates[axis] < minimum) {
                if (first[axis] <= second[axis]) first[axis] = targetCoordinates[axis]; else second[axis] = targetCoordinates[axis];
            } else if (targetCoordinates[axis] > maximum) {
                if (first[axis] >= second[axis]) first[axis] = targetCoordinates[axis]; else second[axis] = targetCoordinates[axis];
            }
        }
        return new BlockVector3[]{
                BlockVector3.at(first[0], first[1], first[2]),
                BlockVector3.at(second[0], second[1], second[2])
        };
    }

    private int dominantAxis(Vector direction) {
        double x = Math.abs(direction.getX()), y = Math.abs(direction.getY()), z = Math.abs(direction.getZ());
        return x >= y && x >= z ? 0 : y >= z ? 1 : 2;
    }

    private int axisDirection(Vector direction, int axis) {
        double component = axis == 0 ? direction.getX() : axis == 1 ? direction.getY() : direction.getZ();
        return component >= 0 ? 1 : -1;
    }

    private int findLookedAtPoint(Player player, List<BlockVector2> points, int y) {
        Location eye = player.getEyeLocation();
        Vector origin = eye.toVector(), direction = eye.getDirection().normalize();
        double closestDistance = MAX_POINT_DISTANCE_SQUARED;
        int closestIndex = -1;
        for (int i = 0; i < points.size(); i++) {
            BlockVector2 point = points.get(i);
            Vector target = new Vector(point.x() + 0.5, y + 0.5, point.z() + 0.5);
            Vector offset = target.clone().subtract(origin);
            double alongRay = offset.dot(direction);
            if (alongRay < 0 || alongRay > MAX_DISTANCE) continue;
            double distanceSquared = origin.clone().add(direction.clone().multiply(alongRay)).distanceSquared(target);
            if (distanceSquared < closestDistance) { closestDistance = distanceSquared; closestIndex = i; }
        }
        return closestIndex;
    }

    private void showMode(Player player, EditState state) {
        if (state.modeBar == null) {
            state.modeBar = Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SOLID);
            state.modeBar.addPlayer(player);
        }
        if (state.mode == Mode.MOVE) {
            state.modeBar.setTitle("§eMOVE §f[" + state.move[0] + ", " + state.move[1] + ", " + state.move[2]
                    + "] §7Q: rotate · Shift+Q: flip look axis · " + transformLabel(state)
                    + " · Scroll to move · Right click: apply · Left click: exit");
        } else if (state.mode == Mode.COPY) {
            state.modeBar.setTitle("§eCOPY §f[" + state.copy[0] + ", " + state.copy[1] + ", " + state.copy[2]
                    + "] §7Q: rotate · Shift+Q: flip look axis · " + transformLabel(state)
                    + " · Scroll to move copy · Right click: place · Left click: exit");
        } else {
            state.modeBar.setTitle("§eSTACK §f[X:" + state.stack[0] + " Y:" + state.stack[1] + " Z:" + state.stack[2]
                    + "] §7Scroll to stack · Right click: apply · Left click: exit");
        }
    }

    private String transformLabel(EditState state) {
        if (state.transforms.isEmpty()) return "Original";
        TransformOp last = state.transforms.get(state.transforms.size() - 1);
        return last.rotation ? "Last: rotate 90°" : "Last: flip " + switch (last.axis) {
            case 0 -> "X";
            case 1 -> "Y";
            default -> "Z";
        };
    }

    private enum Mode { NORMAL, MOVE, COPY, STACK }

    private static final class EditState {
        private final LocalSession session;
        private final int[] move = new int[3];
        private final int[] copy = new int[3];
        private final java.util.Set<BlockVector3> acceptedCopies = new java.util.HashSet<>();
        // One signed count per axis, with zero representing the original selection.
        private final int[] stack = new int[3];
        private Object previewHandle;
        private Object selectionSignature;
        private Region previewSelection;
        private BukkitTask selectionWatcher;
        private BukkitTask selectionRestoreTask;
        private boolean suppressSelectionRefresh;
        private int lastVirtualSlot;
        private final List<TransformOp> transforms = new ArrayList<>();
        private final List<ScrollInput> pendingScrollInputs = new ArrayList<>();
        private BukkitTask pendingScrollTask;
        private final int lockedSlot;
        private boolean slotResetScheduled;
        private Mask mask;
        private String maskExpression;
        private BossBar modeBar;
        private Mode mode = Mode.MOVE;

        private EditState(LocalSession session, int initialSlot) {
            this.session = session;
            this.lastVirtualSlot = initialSlot;
            this.lockedSlot = initialSlot;
        }
    }

    private record ScrollInput(int axis, int direction, int amount) { }

    private record TransformOp(boolean rotation, int axis) { }

}
