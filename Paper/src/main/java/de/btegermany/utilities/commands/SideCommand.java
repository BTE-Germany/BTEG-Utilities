package de.btegermany.utilities.commands;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;

import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import de.btegermany.utilities.BTEGUtilities;
import de.btegermany.utilities.util.*;
import de.btegermany.utilities.util.worldedit.*;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static java.util.Collections.emptyList;

public class SideCommand implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        if (!player.hasPermission("bteg.builder")) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cNo permission for //side");
            return true;
        }

        if (args.length <= 2) {
            player.sendMessage(BTEGUtilities.PREFIX + "Usage:");
            player.sendMessage(BTEGUtilities.PREFIX + "//side <Block-ID> <Block-ID> <Direction[n,e,s,w]> <ignoreSameBlocks[y,n]> <Mask1> <Mask2> <...>");
            return true;
        }

        BlockType preBlock = Converter.getBlockType(args[0].toUpperCase(), player);
        BlockType postBlock = Converter.getBlockType(args[1].toUpperCase(), player);
        Direction direction = Direction.fromInput(args[2]);
        boolean ignoreSameBlock = false;
        TypeOnlyMask mask = new TypeOnlyMask(true);

        if (args.length >= 4) {
            if (args[3].equalsIgnoreCase("y") || args[3].equalsIgnoreCase("yes")) {
                ignoreSameBlock = true;
            } else if (!(args[3].equalsIgnoreCase("n") || args[3].equalsIgnoreCase("no"))) {
                player.sendMessage(BTEGUtilities.PREFIX + "§cWrong usage:");
                player.sendMessage(BTEGUtilities.PREFIX + "//side <Block-ID> <Block-ID> <Direction[n,e,s,w]> <ignoreSameBlocks[y,n]> <Mask1> <Mask2> <...>");
                return true;
            }

            if (args.length >= 5) {
                boolean inverse = false;
                String blocksString = args[4];
                if (args[4].startsWith("!")) {
                    inverse = true;
                    blocksString = args[4].substring("!".length());
                }

                BlockType[] blockTypes = WorldEditUtil.getBlockTypesFromInput(player, blocksString);
                mask = new TypeOnlyMask(inverse, blockTypes);
            }
        }
        try {
            ReplaceSideArgs replaceSideArgs = new ReplaceSideArgs(preBlock, postBlock, direction, ignoreSameBlock, mask);
            WorldEditUtil.findSelection(player, session -> {
                SideCommand.replaceSide(session, replaceSideArgs);

                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            });
        } catch (MaxChangedBlocksException | EmptyClipboardException e) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cAn error occurred.");
            e.printStackTrace();
        }

        return true;
    }

    public static void replaceSide(SelectionEditSession session, ReplaceSideArgs args) throws MaxChangedBlocksException, EmptyClipboardException {
        Region region = session.region();
        Player player = session.player();

        Map<BlockVector3, BlockState> changedBlocks = new HashMap<>();

        for (int i = region.getMinimumPoint().x(); i <= region.getMaximumPoint().x(); i++) {
            for (int j = region.getMinimumPoint().y(); j <= region.getMaximumPoint().y(); j++) {
                for (int k = region.getMinimumPoint().z(); k <= region.getMaximumPoint().z(); k++) {
                    BlockVector3 blockVector = BlockVector3.at(i, j, k);
                    if (region.contains(blockVector) && !changedBlocks.containsKey(blockVector)) {
                        BlockType blockType = (session.changedBlocks().containsKey(blockVector) ? session.changedBlocks().get(blockVector) : session.editSession().getBlock(blockVector)).getBlockType();
                        if (blockType.equals(args.preBlockType())) {
                            switch (args.direction()) {
                                case NORTH -> {
                                    BlockVector3 nearbyBlockVector = BlockVector3.at(i, j, k - 1);
                                    if (replaceBlock(nearbyBlockVector, args, session)) {
                                        changedBlocks.put(nearbyBlockVector, args.postBlockState());
                                    }
                                }
                                case EAST -> {
                                    BlockVector3 nearbyBlockVector = BlockVector3.at(i + 1, j, k);
                                    if (replaceBlock(nearbyBlockVector, args, session)) {
                                        changedBlocks.put(nearbyBlockVector, args.postBlockState());
                                    }
                                }
                                case SOUTH -> {
                                    BlockVector3 nearbyBlockVector = BlockVector3.at(i, j, k + 1);
                                    if (replaceBlock(nearbyBlockVector, args, session)) {
                                        changedBlocks.put(nearbyBlockVector, args.postBlockState());
                                    }
                                }
                                case WEST -> {
                                    BlockVector3 nearbyBlockVector = BlockVector3.at(i - 1, j, k);
                                    if (replaceBlock(nearbyBlockVector, args, session)) {
                                        changedBlocks.put(nearbyBlockVector, args.postBlockState());
                                    }
                                }
                                case UP -> {
                                    BlockVector3 nearbyBlockVector = BlockVector3.at(i, j + 1, k);
                                    if (replaceBlock(nearbyBlockVector, args, session)) {
                                        changedBlocks.put(nearbyBlockVector, args.postBlockState());
                                    }
                                }
                                case DOWN -> {
                                    BlockVector3 nearbyBlockVector = BlockVector3.at(i, j - 1, k);
                                    if (replaceBlock(nearbyBlockVector, args, session)) {
                                        changedBlocks.put(nearbyBlockVector, args.postBlockState());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        session.changedBlocks().putAll(changedBlocks);

        player.sendMessage(BTEGUtilities.PREFIX + "Successfully replaced §6§l" + changedBlocks.size() + " §r§7blocks sideways!");
    }

    /**
     * @return whether changes were made
     */
    private static boolean replaceBlock(BlockVector3 blockVector, ReplaceSideArgs args, SelectionEditSession session) {
        BlockType blockType = (session.changedBlocks().containsKey(blockVector) ? session.changedBlocks().get(blockVector) : session.editSession().getBlock(blockVector)).getBlockType();

        if ((args.mask() != null) && !args.mask().test(blockType)) {
            return false;
        }

        if (!args.ignoreSameBlock() && blockType.equals(args.preBlockType())) {
            return false;
        }

        session.editSession().setBlock(blockVector.x(), blockVector.y(), blockVector.z(), args.postBlockState());
        session.changedBlocks().put(blockVector, args.postBlockState());
        return true;
    }


    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("bteg.builder")) {
            return emptyList();
        }
        // First argument: target
        return switch (args.length) {
            case 1, 2 -> TabUtil.getMaterialBlocks(args[args.length - 1], true);

            case 3 -> {
                if (!args[args.length - 1].isEmpty()) {
                    yield emptyList();
                }
                yield List.of("n", "e", "s", "w", "u", "d");
            }

            case 4 -> {
                if (!args[args.length - 1].isEmpty()) {
                    yield emptyList();
                }
                yield List.of("y", "n");
            }

            case 5 -> {
                int lastIndex = args[4].lastIndexOf(",") + 1;
                if (lastIndex == 0) {
                    lastIndex += args[4].startsWith("!") ? 1 : 0;
                }
                yield TabUtil.getMaterialBlocks(args[4].substring(lastIndex), args[4].substring(0, lastIndex), true);
            }

            default -> emptyList();
        };
    }
}

