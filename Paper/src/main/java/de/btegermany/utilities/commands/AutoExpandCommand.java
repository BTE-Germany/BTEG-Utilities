package de.btegermany.utilities.commands;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.selector.Polygonal2DRegionSelector;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import de.btegermany.utilities.BTEGUtilities;
import de.btegermany.utilities.util.ExecutionGuard;
import de.btegermany.utilities.util.worldedit.Converter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;

/** Expands a poly selection through face-connected blocks matching a block mask. */
public final class AutoExpandCommand implements TabExecutor {
    private static final int MAX_RADIUS = 256;
    private static final int MAX_STARTING_SELECTION_BLOCKS = 50_000;
    private static final Duration MAX_EXECUTION_TIME = Duration.ofSeconds(5);
    // Four horizontal cardinals at the same level and one level above/below, plus
    // directly vertical neighbors. This allows stepped surfaces without diagonals.
    private static final int[][] DIRECTIONS = {
            {1,0,0}, {-1,0,0}, {0,0,1}, {0,0,-1},
            {1,1,0}, {1,-1,0}, {-1,1,0}, {-1,-1,0},
            {0,1,1}, {0,-1,1}, {0,1,-1}, {0,-1,-1},
            {0,1,0}, {0,-1,0}
    };

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) return true;
        if (!player.hasPermission("bteg.builder")) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cNo permission for //autoexpand");
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(BTEGUtilities.PREFIX + "Usage: //autoexpand <block[,block...] | auto>");
            return true;
        }

        String maskInput = args[0];
        boolean plotMode = maskInput.equalsIgnoreCase("plot");
        if (maskInput.isBlank()) {
            player.sendMessage(BTEGUtilities.PREFIX + "Usage: //autoexpand <block[,block...] | auto>");
            return true;
        }

        var localSession = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));
        final Polygonal2DRegion selection;
        try {
            if (!(localSession.getSelection(localSession.getSelectionWorld()) instanceof Polygonal2DRegion poly)) {
                player.sendMessage(BTEGUtilities.PREFIX + "§cPlease use a poly selection for //autoexpand.");
                return true;
            }
            selection = poly;
        } catch (IncompleteRegionException exception) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cPlease make a poly selection first.");
            return true;
        }
        if (selection.getVolume() > MAX_STARTING_SELECTION_BLOCKS) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cThe starting selection exceeds the 50,000-block limit.");
            return true;
        }

        try (ExecutionGuard ignored = ExecutionGuard.start(MAX_EXECUTION_TIME)) {
        org.bukkit.World bukkitWorld = BukkitAdapter.adapt(selection.getWorld());
        Map<BlockVector3, Boolean> fullCubeCache = new HashMap<>();
        Set<BlockType> mask = new HashSet<>();
        String[] maskEntries = maskInput.split(",", -1);
        boolean automaticMask = Arrays.stream(maskEntries).anyMatch(entry -> entry.equalsIgnoreCase("auto"));
        boolean autoMask = automaticMask || plotMode;
        if (autoMask) {
            for (int x = selection.getMinimumPoint().x(); x <= selection.getMaximumPoint().x(); x++)
                for (int y = selection.getMinimumPoint().y(); y <= selection.getMaximumPoint().y(); y++)
                    for (int z = selection.getMinimumPoint().z(); z <= selection.getMaximumPoint().z(); z++) {
                        ExecutionGuard.check();
                        BlockVector3 p = BlockVector3.at(x, y, z);
                        if (!selection.contains(p)) continue;
                        BlockType type = selection.getWorld().getBlock(p).getBlockType();
                        if (type.getMaterial().isAir()) continue;
                        if (automaticMask) {
                            boolean blockIsNonFull = !isFullCube(bukkitWorld, p, fullCubeCache);
                            boolean aboveIsNonFull = !isFullCube(bukkitWorld, p.add(0, 1, 0), fullCubeCache);
                            if (!blockIsNonFull && !aboveIsNonFull) continue;
                        }
                        mask.add(type);
                    }
            if (plotMode) {
                mask.add(Objects.requireNonNull(BlockTypes.CLAY));
                mask.add(Objects.requireNonNull(BlockTypes.LAPIS_BLOCK));
            }
        }
        if (!plotMode) {
            try {
                for (String id : maskEntries) {
                    if (id.equalsIgnoreCase("auto")) continue;
                    if (id.isBlank()) throw new IllegalArgumentException("Empty block in mask");
                    BlockType type = Converter.getBlockType(id, player);
                    if (type.getMaterial().isAir()) {
                        player.sendMessage(BTEGUtilities.PREFIX + "§cAir blocks cannot be used in the expansion pattern.");
                        return true;
                    }
                    mask.add(type);
                }
            } catch (RuntimeException exception) {
                player.sendMessage(BTEGUtilities.PREFIX + "§cInvalid block type in mask.");
                return true;
            }
        }
        if (mask.isEmpty()) {
            player.sendMessage(BTEGUtilities.PREFIX + (autoMask ? "§cThe selection contains no non-air blocks." : "§cPlease provide at least one block type."));
            return true;
        }

        var world = selection.getWorld();
        SearchCenter center = selectionCenter(selection);
        ArrayDeque<SearchNode> queue = new ArrayDeque<>();
        Set<BlockVector3> found = new HashSet<>();
        Set<BlockVector3> visited = new HashSet<>();
        // Matching blocks already inside the original poly selection are also search seeds.
        // The selection is only the search origin; it is not retained as a shape constraint.
        for (int x = selection.getMinimumPoint().x(); x <= selection.getMaximumPoint().x(); x++)
            for (int y = selection.getMinimumPoint().y(); y <= selection.getMaximumPoint().y(); y++)
                for (int z = selection.getMinimumPoint().z(); z <= selection.getMaximumPoint().z(); z++) {
                    ExecutionGuard.check();
                    BlockVector3 p = BlockVector3.at(x, y, z);
                    if (selection.contains(p) && visited.add(p) && matches(world, bukkitWorld, p, mask, plotMode, fullCubeCache)) {
                        found.add(p);
                        queue.add(new SearchNode(p));
                    }
                }
        // Every block immediately outside the selected volume is a possible flood-fill seed.
        for (int x = selection.getMinimumPoint().x() - 1; x <= selection.getMaximumPoint().x() + 1; x++)
            for (int y = selection.getMinimumPoint().y() - 1; y <= selection.getMaximumPoint().y() + 1; y++)
                for (int z = selection.getMinimumPoint().z() - 1; z <= selection.getMaximumPoint().z() + 1; z++) {
                    ExecutionGuard.check();
                    BlockVector3 p = BlockVector3.at(x,y,z);
                    if (selection.contains(p)) continue;
                    if (!withinRadius(p, center)) continue;
                    boolean adjacent = false;
                    for (int[] d : DIRECTIONS) if (selection.contains(p.add(d[0],d[1],d[2]))) { adjacent = true; break; }
                    if (adjacent && visited.add(p) && matches(world, bukkitWorld, p, mask, plotMode, fullCubeCache)) {
                        found.add(p);
                        queue.add(new SearchNode(p));
                    }
                }

        while (!queue.isEmpty()) {
            ExecutionGuard.check();
            SearchNode node = queue.removeFirst();
            for (int[] d : DIRECTIONS) {
                ExecutionGuard.check();
                BlockVector3 next = node.position.add(d[0], d[1], d[2]);
                if (!selection.contains(next) && !withinRadius(next, center)) continue;
                if (!visited.add(next) || !matches(world, bukkitWorld, next, mask, plotMode, fullCubeCache)) continue;
                found.add(next);
                queue.addLast(new SearchNode(next));
            }
        }
        if (found.isEmpty()) {
            player.sendMessage(BTEGUtilities.PREFIX + "§7No adjacent blocks matched the mask.");
            return true;
        }

        Set<BlockVector2> footprint = new HashSet<>();
        int minY = selection.getMinimumPoint().y(), maxY = selection.getMaximumPoint().y();
        for (BlockVector3 p : found) {
            ExecutionGuard.check();
            footprint.add(BlockVector2.at(p.x(), p.z()));
            minY = Math.min(minY, p.y());
            maxY = Math.max(maxY, p.y());
        }
        List<List<BlockVector2>> loops = traceBoundaries(footprint);
        // Convert cell-corner edges to the block columns immediately inside each edge.
        // Poly selector vertices are block coordinates, not geometric half-block corners;
        // leaving corners at x+1/z+1 makes the selection include an extra row/column.
        List<List<BlockVector2>> convertedLoops = new ArrayList<>();
        for (List<BlockVector2> loop : loops) {
            ExecutionGuard.check();
            convertedLoops.add(simplifyCollinear(edgeLoopToBlockCoordinates(loop)));
        }
        List<BlockVector2> outline = joinLoops(convertedLoops);
        if (outline.size() < 3) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cThe expanded selection does not form a valid polygon.");
            return true;
        }
        ExecutionGuard.check();
        localSession.setRegionSelector(world, new Polygonal2DRegionSelector(world, outline, minY, maxY));
        // Setting the server-side selector alone does not notify WorldEditCUI.
        localSession.dispatchCUISelection(BukkitAdapter.adapt(player));
        player.sendMessage(BTEGUtilities.PREFIX + "§aExpanded selection through " + found.size() + " matching block(s).");
        return true;
        } catch (ExecutionGuard.TimeoutException timeout) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cAutoexpand exceeded its 5-second calculation limit. The selection was not changed.");
            return true;
        }
    }

    private boolean matches(com.sk89q.worldedit.world.World world, org.bukkit.World bukkitWorld, BlockVector3 position,
                            Set<BlockType> mask, boolean plotMode, Map<BlockVector3, Boolean> fullCubeCache) {
        BlockState state = world.getBlock(position);
        if (!mask.contains(state.getBlockType())) return false;
        if (plotMode && isPlotMarker(state.getBlockType())) return true;

        // Full-cube mask behavior: a block is on the surface when the first block
        // above the matching vertical run does not fill its entire collision cube.
        BlockVector3 above = position.add(0, 1, 0);
        while (true) {
            ExecutionGuard.check();
            BlockState aboveState = world.getBlock(above);
            boolean fullCube = isFullCube(bukkitWorld, above, fullCubeCache);
            if (!fullCube) return true;
            if (!mask.contains(aboveState.getBlockType())) return false;
            above = above.add(0, 1, 0);
        }
    }

    /** Tests whether any collision box fills the complete block cube (FAWE-independent #fullcube equivalent). */
    private boolean isFullCube(org.bukkit.World world, BlockVector3 position, Map<BlockVector3, Boolean> cache) {
        return cache.computeIfAbsent(position, key -> isFullCube(world, key));
    }

    private boolean isFullCube(org.bukkit.World world, BlockVector3 position) {
        var block = world.getBlockAt(position.x(), position.y(), position.z());
        for (BoundingBox box : block.getCollisionShape().getBoundingBoxes()) {
            boolean localCube = containsCube(box, 0, 0, 0);
            boolean worldCube = containsCube(box, position.x(), position.y(), position.z());
            if (localCube || worldCube) return true;
        }
        return false;
    }

    private boolean containsCube(BoundingBox box, double x, double y, double z) {
        double epsilon = 1.0e-7;
        return box.getMinX() <= x + epsilon && box.getMinY() <= y + epsilon && box.getMinZ() <= z + epsilon
                && box.getMaxX() >= x + 1 - epsilon && box.getMaxY() >= y + 1 - epsilon && box.getMaxZ() >= z + 1 - epsilon;
    }

    private SearchCenter selectionCenter(Polygonal2DRegion selection) {
        BlockVector3 min = selection.getMinimumPoint(), max = selection.getMaximumPoint();
        return new SearchCenter((min.x() + (double) max.x()) / 2.0,
                (min.y() + (double) max.y()) / 2.0,
                (min.z() + (double) max.z()) / 2.0);
    }

    private boolean withinRadius(BlockVector3 position, SearchCenter center) {
        double dx = position.x() - center.x();
        double dy = position.y() - center.y();
        double dz = position.z() - center.z();
        return dx * dx + dy * dy + dz * dz <= (long) MAX_RADIUS * MAX_RADIUS;
    }

    private boolean isPlotMarker(BlockType type) {
        return type == BlockTypes.CLAY || type == BlockTypes.LAPIS_BLOCK;
    }

    /** Traces every directed edge between an occupied column and an empty column. */
    private List<List<BlockVector2>> traceBoundaries(Set<BlockVector2> cells) {
        List<Edge> edges = new ArrayList<>();
        for (BlockVector2 cell : cells) {
            ExecutionGuard.check();
            int x = cell.x(), z = cell.z();
            if (!cells.contains(BlockVector2.at(x, z - 1))) edges.add(new Edge(x, z, x + 1, z));
            if (!cells.contains(BlockVector2.at(x + 1, z))) edges.add(new Edge(x + 1, z, x + 1, z + 1));
            if (!cells.contains(BlockVector2.at(x, z + 1))) edges.add(new Edge(x + 1, z + 1, x, z + 1));
            if (!cells.contains(BlockVector2.at(x - 1, z))) edges.add(new Edge(x, z + 1, x, z));
        }
        Map<Long, List<Integer>> outgoing = new HashMap<>();
        for (int i = 0; i < edges.size(); i++) {
            ExecutionGuard.check();
            outgoing.computeIfAbsent(key(edges.get(i).sx, edges.get(i).sz), ignored -> new ArrayList<>()).add(i);
        }
        boolean[] used = new boolean[edges.size()];
        List<List<BlockVector2>> loops = new ArrayList<>();
        for (int start = 0; start < edges.size(); start++) {
            ExecutionGuard.check();
            if (used[start]) continue;
            List<BlockVector2> loop = new ArrayList<>();
            int edgeIndex = start;
            Edge first = edges.get(start);
            loop.add(BlockVector2.at(first.sx, first.sz));
            int guard = edges.size() + 1;
            while (guard-- > 0 && !used[edgeIndex]) {
                ExecutionGuard.check();
                Edge edge = edges.get(edgeIndex);
                used[edgeIndex] = true;
                loop.add(BlockVector2.at(edge.ex, edge.ez));
                if (edge.ex == first.sx && edge.ez == first.sz) break;
                List<Integer> candidates = outgoing.getOrDefault(key(edge.ex, edge.ez), List.of()).stream().filter(i -> !used[i]).toList();
                if (candidates.isEmpty()) break;
                edgeIndex = chooseTurn(edge, candidates, edges);
            }
            if (loop.size() >= 4 && loop.getFirst().equals(loop.getLast())) {
                loop.removeLast();
                loops.add(loop);
            }
        }
        return loops;
    }

    // At a vertex shared by diagonal cells, prefer the tightest right-hand turn to keep loops separate.
    private int chooseTurn(Edge incoming, List<Integer> choices, List<Edge> edges) {
        return choices.stream().min(Comparator.comparingInt(i -> turnRank(incoming, edges.get(i)))).orElseThrow();
    }
    private int turnRank(Edge a, Edge b) {
        int ax = a.ex - a.sx, az = a.ez - a.sz, bx = b.ex - b.sx, bz = b.ez - b.sz;
        int cross = ax * bz - az * bx, dot = ax * bx + az * bz;
        if (cross > 0) return 0;
        if (dot > 0) return 1;
        return 2;
    }

    /** Emits complete outer rings first, then traverses each hole as a bridge-out-and-back loop. */
    private List<BlockVector2> joinLoops(List<List<BlockVector2>> loops) {
        if (loops.isEmpty()) return List.of();
        List<List<BlockVector2>> outerRings = new ArrayList<>();
        List<List<BlockVector2>> holeRings = new ArrayList<>();
        for (List<BlockVector2> loop : loops) {
            ExecutionGuard.check();
            if (loop.size() < 3) continue;
            if (signedArea(loop) >= 0) outerRings.add(loop);
            else holeRings.add(loop);
        }
        if (outerRings.isEmpty()) {
            // Degenerate orientation fallback: retain the largest traced outline as outer.
            List<BlockVector2> largest = null;
            double largestArea = -1;
            for (List<BlockVector2> loop : loops) {
                ExecutionGuard.check();
                double area = Math.abs(signedArea(loop));
                if (area > largestArea) { largestArea = area; largest = loop; }
            }
            if (largest == null) return List.of();
            outerRings.add(largest);
            holeRings.remove(largest);
        }
        outerRings.sort(Comparator.comparingDouble((List<BlockVector2> loop) -> Math.abs(signedArea(loop))).reversed());
        List<List<BlockVector2>> secondary = new ArrayList<>(outerRings.subList(1, outerRings.size()));
        secondary.addAll(holeRings);

        BridgePlan bridgePlan = findBridgePlan(outerRings.getFirst(), secondary);
        List<BlockVector2> main = insertBoundaryPoint(outerRings.getFirst(), bridgePlan.mainPoint());
        rotateTo(main, bridgePlan.mainPoint());
        BlockVector2 anchor = bridgePlan.mainPoint();
        // Complete the main outer ring before making any bridge to an enclave or hole.
        main.add(anchor);
        for (int loopIndex = 0; loopIndex < secondary.size(); loopIndex++) {
            ExecutionGuard.check();
            List<BlockVector2> loop = secondary.get(loopIndex);
            BlockVector2 loopAnchor = bridgePlan.secondaryPoints().get(loopIndex);
            loop = insertBoundaryPoint(loop, loopAnchor);
            rotateTo(loop, loopAnchor);
            main.add(loopAnchor);
            for (int i = 1; i < loop.size(); i++) {
                ExecutionGuard.check();
                main.add(loop.get(i));
            }
            main.add(loopAnchor); // finish the complete ring
            main.add(anchor);     // bridge back before tracing the next ring
        }
        return main;
    }

    /** Picks the shortest bridge endpoints whose segment contains no intermediate block center. */
    private BridgePlan findBridgePlan(List<BlockVector2> main, List<List<BlockVector2>> secondary) {
        List<BlockVector2> mainCandidates = boundaryLatticePoints(main);
        List<List<BlockVector2>> candidatesByLoop = new ArrayList<>();
        for (List<BlockVector2> loop : secondary) candidatesByLoop.add(boundaryLatticePoints(loop));
        long bestTotal = Long.MAX_VALUE;
        BlockVector2 bestMain = mainCandidates.getFirst();
        List<BlockVector2> bestSecondary = new ArrayList<>();
        for (BlockVector2 mainPoint : mainCandidates) {
            ExecutionGuard.check();
            long total = 0;
            List<BlockVector2> destinations = new ArrayList<>();
            boolean viable = true;
            for (List<BlockVector2> candidates : candidatesByLoop) {
                ExecutionGuard.check();
                BlockVector2 destination = null;
                long bestDistance = Long.MAX_VALUE;
                for (BlockVector2 candidate : candidates) {
                    ExecutionGuard.check();
                    if (!primitiveBridge(mainPoint, candidate)) continue;
                    long distance = squaredDistance(mainPoint, candidate);
                    if (distance < bestDistance) { bestDistance = distance; destination = candidate; }
                }
                if (destination == null) { viable = false; break; }
                destinations.add(destination);
                total += bestDistance;
            }
            if (viable && total < bestTotal) {
                bestTotal = total;
                bestMain = mainPoint;
                bestSecondary = destinations;
            }
        }
        // Distinct block-grid boundaries always provide primitive lattice pairs. Keep a safe
        // fallback for degenerate rings where the selector collapsed adjacent boundary points.
        if (bestSecondary.size() != secondary.size()) {
            bestSecondary.clear();
            BlockVector2 fallbackMain = bestMain;
            for (List<BlockVector2> candidates : candidatesByLoop) {
                BlockVector2 destination = null;
                long bestDistance = Long.MAX_VALUE;
                for (BlockVector2 candidate : candidates) {
                    ExecutionGuard.check();
                    long distance = squaredDistance(fallbackMain, candidate);
                    if (distance < bestDistance) { bestDistance = distance; destination = candidate; }
                }
                if (destination == null) throw new IllegalStateException("Boundary has no bridge candidates");
                bestSecondary.add(destination);
            }
        }
        return new BridgePlan(bestMain, bestSecondary);
    }

    private List<BlockVector2> boundaryLatticePoints(List<BlockVector2> loop) {
        LinkedHashSet<BlockVector2> points = new LinkedHashSet<>();
        for (int i = 0; i < loop.size(); i++) {
            ExecutionGuard.check();
            BlockVector2 start = loop.get(i), end = loop.get((i + 1) % loop.size());
            int dx = end.x() - start.x(), dz = end.z() - start.z();
            int steps = (int) gcd(Math.abs((long) dx), Math.abs((long) dz));
            if (steps == 0) { points.add(start); continue; }
            int stepX = dx / steps, stepZ = dz / steps;
            for (int step = 0; step < steps; step++) {
                ExecutionGuard.check();
                points.add(BlockVector2.at(start.x() + step * stepX, start.z() + step * stepZ));
            }
        }
        return new ArrayList<>(points);
    }

    private List<BlockVector2> insertBoundaryPoint(List<BlockVector2> source, BlockVector2 point) {
        List<BlockVector2> loop = new ArrayList<>(source);
        if (loop.contains(point)) return loop;
        for (int i = 0; i < loop.size(); i++) {
            ExecutionGuard.check();
            BlockVector2 start = loop.get(i), end = loop.get((i + 1) % loop.size());
            int dx = end.x() - start.x(), dz = end.z() - start.z();
            int steps = (int) gcd(Math.abs((long) dx), Math.abs((long) dz));
            if (steps == 0) continue;
            int stepX = dx / steps, stepZ = dz / steps;
            for (int step = 1; step < steps; step++) {
                ExecutionGuard.check();
                if (start.x() + step * stepX == point.x() && start.z() + step * stepZ == point.z()) {
                    loop.add(i + 1, point);
                    return loop;
                }
            }
        }
        return loop;
    }

    private void rotateTo(List<BlockVector2> loop, BlockVector2 point) {
        int index = loop.indexOf(point);
        if (index <= 0) return;
        Collections.rotate(loop, -index);
    }

    private boolean primitiveBridge(BlockVector2 a, BlockVector2 b) {
        long dx = Math.abs((long) a.x() - b.x()), dz = Math.abs((long) a.z() - b.z());
        return (dx != 0 || dz != 0) && gcd(dx, dz) == 1;
    }

    private long squaredDistance(BlockVector2 a, BlockVector2 b) {
        long dx = (long) a.x() - b.x(), dz = (long) a.z() - b.z();
        return dx * dx + dz * dz;
    }

    private long gcd(long a, long b) {
        while (b != 0) { long remainder = a % b; a = b; b = remainder; }
        return a;
    }

    private double signedArea(List<BlockVector2> loop) {
        long area = 0;
        for (int i = 0; i < loop.size(); i++) {
            ExecutionGuard.check();
            BlockVector2 a = loop.get(i), b = loop.get((i + 1) % loop.size());
            area += (long) a.x() * b.z() - (long) b.x() * a.z();
        }
        return area / 2.0;
    }

    private long key(int x, int z) { return ((long) x << 32) ^ (z & 0xffffffffL); }
    private record Edge(int sx, int sz, int ex, int ez) {}
    private record BridgePlan(BlockVector2 mainPoint, List<BlockVector2> secondaryPoints) {}

    private List<BlockVector2> edgeLoopToBlockCoordinates(List<BlockVector2> loop) {
        List<BlockVector2> result = new ArrayList<>();
        for (int i = 0; i < loop.size(); i++) {
            ExecutionGuard.check();
            BlockVector2 from = loop.get(i), to = loop.get((i + 1) % loop.size());
            int dx = to.x() - from.x(), dz = to.z() - from.z();
            BlockVector2 inside;
            if (dx > 0) inside = BlockVector2.at(from.x(), from.z());
            else if (dx < 0) inside = BlockVector2.at(from.x() - 1, from.z() - 1);
            else if (dz > 0) inside = BlockVector2.at(from.x() - 1, from.z());
            else inside = BlockVector2.at(from.x(), from.z() - 1);
            if (result.isEmpty() || !result.getLast().equals(inside)) result.add(inside);
        }
        while (result.size() > 1 && result.getFirst().equals(result.getLast())) result.removeLast();
        return result;
    }

    /** Removes vertices in the middle of a straight boundary run. */
    private List<BlockVector2> simplifyCollinear(List<BlockVector2> loop) {
        List<BlockVector2> points = new ArrayList<>(loop);
        boolean changed = true;
        while (changed && points.size() > 3) {
            ExecutionGuard.check();
            changed = false;
            for (int i = 0; i < points.size(); i++) {
                ExecutionGuard.check();
                BlockVector2 previous = points.get((i + points.size() - 1) % points.size());
                BlockVector2 current = points.get(i);
                BlockVector2 next = points.get((i + 1) % points.size());
                long cross = (long) (current.x() - previous.x()) * (next.z() - current.z())
                        - (long) (current.z() - previous.z()) * (next.x() - current.x());
                if (cross == 0) {
                    points.remove(i);
                    changed = true;
                    break;
                }
            }
        }
        return points;
    }

    @Override public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
        if (!sender.hasPermission("bteg.builder") || args.length != 1) return List.of();

        String current = args[args.length - 1];
        int comma = current.lastIndexOf(',');
        String prefix = comma < 0 ? "" : current.substring(0, comma + 1);
        String partial = comma < 0 ? current : current.substring(comma + 1);

        List<String> suggestions = new ArrayList<>();
        if ("auto".startsWith(partial.toLowerCase(Locale.ROOT))) suggestions.add(prefix + "auto");
        if (comma < 0 && "plot".startsWith(partial.toLowerCase(Locale.ROOT))) suggestions.add("plot");
        suggestions.addAll(de.btegermany.utilities.util.TabUtil.getBlockPatternSuggestions(partial, true).stream()
                .filter(suggestion -> !isAirSuggestion(suggestion, sender))
                .map(suggestion -> prefix + suggestion).toList());
        return suggestions.stream().distinct().toList();
    }

    private boolean isAirSuggestion(String suggestion, CommandSender sender) {
        int stateStart = suggestion.indexOf('[');
        String blockId = stateStart < 0 ? suggestion : suggestion.substring(0, stateStart);
        try {
            BlockType type = blockId.equalsIgnoreCase("hand") && sender instanceof Player player
                    ? Converter.getBlockType(blockId, player) : Converter.getBlockType(blockId);
            return type.getMaterial().isAir();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private record SearchNode(BlockVector3 position) {}
    private record SearchCenter(double x, double y, double z) {}
}
