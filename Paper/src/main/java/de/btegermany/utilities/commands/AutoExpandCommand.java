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
import de.btegermany.utilities.util.worldedit.Converter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

/** Expands a poly selection through face-connected blocks matching a block mask. */
public final class AutoExpandCommand implements TabExecutor {
    private static final int MAX_RADIUS = 128;
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
        if (args.length < 1 || args.length > 2) {
            player.sendMessage(BTEGUtilities.PREFIX + "Usage: //autoexpand <block[,block...]|auto|plot> [surface|ground]");
            return true;
        }

        boolean surfaceMode = true;
        if (args.length == 2) {
            if (!args[1].equalsIgnoreCase("surface") && !args[1].equalsIgnoreCase("ground")) {
                player.sendMessage(BTEGUtilities.PREFIX + "§cThe second parameter must be surface or ground.");
                return true;
            }
            surfaceMode = args[1].equalsIgnoreCase("surface");
        }
        String maskInput = args[0];
        boolean plotMode = maskInput.equalsIgnoreCase("plot");
        if (plotMode && args.length > 1) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cThe plot option does not take a second parameter.");
            return true;
        }
        if (maskInput.isBlank()) {
            player.sendMessage(BTEGUtilities.PREFIX + "Usage: //autoexpand <block[,block...]|auto|plot> [surface|ground]");
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

        Set<BlockType> mask = new HashSet<>();
        boolean autoMask = maskInput.equalsIgnoreCase("auto") || plotMode;
        if (autoMask) {
            for (int x = selection.getMinimumPoint().x(); x <= selection.getMaximumPoint().x(); x++)
                for (int y = selection.getMinimumPoint().y(); y <= selection.getMaximumPoint().y(); y++)
                    for (int z = selection.getMinimumPoint().z(); z <= selection.getMaximumPoint().z(); z++) {
                        BlockVector3 p = BlockVector3.at(x, y, z);
                        if (!selection.contains(p)) continue;
                        BlockType type = selection.getWorld().getBlock(p).getBlockType();
                        if (!type.getMaterial().isAir()) mask.add(type);
                    }
            if (plotMode) {
                mask.add(Objects.requireNonNull(BlockTypes.CLAY));
                mask.add(Objects.requireNonNull(BlockTypes.LAPIS_BLOCK));
            }
        } else {
            try {
                for (String id : maskInput.split(",")) {
                    if (id.isBlank()) throw new IllegalArgumentException("Empty block in mask");
                    mask.add(Converter.getBlockType(id, player));
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
        ArrayDeque<SearchNode> queue = new ArrayDeque<>();
        Set<BlockVector3> found = new HashSet<>();
        Set<BlockVector3> visited = new HashSet<>();
        // Matching blocks already inside the original poly selection are also search seeds.
        // The selection is only the search origin; it is not retained as a shape constraint.
        for (int x = selection.getMinimumPoint().x(); x <= selection.getMaximumPoint().x(); x++)
            for (int y = selection.getMinimumPoint().y(); y <= selection.getMaximumPoint().y(); y++)
                for (int z = selection.getMinimumPoint().z(); z <= selection.getMaximumPoint().z(); z++) {
                    BlockVector3 p = BlockVector3.at(x, y, z);
                    if (selection.contains(p) && visited.add(p) && matches(world, p, mask, surfaceMode, plotMode)) {
                        found.add(p);
                        queue.add(new SearchNode(p, 0));
                    }
                }
        // Every block immediately outside the selected volume is a possible flood-fill seed.
        for (int x = selection.getMinimumPoint().x() - 1; x <= selection.getMaximumPoint().x() + 1; x++)
            for (int y = selection.getMinimumPoint().y() - 1; y <= selection.getMaximumPoint().y() + 1; y++)
                for (int z = selection.getMinimumPoint().z() - 1; z <= selection.getMaximumPoint().z() + 1; z++) {
                    BlockVector3 p = BlockVector3.at(x,y,z);
                    if (selection.contains(p)) continue;
                    boolean adjacent = false;
                    for (int[] d : DIRECTIONS) if (selection.contains(p.add(d[0],d[1],d[2]))) { adjacent = true; break; }
                    if (adjacent && visited.add(p) && matches(world, p, mask, surfaceMode, plotMode)) {
                        found.add(p);
                        queue.add(new SearchNode(p, 1));
                    }
                }

        while (!queue.isEmpty()) {
            SearchNode node = queue.removeFirst();
            if (node.distance >= MAX_RADIUS) continue;
            for (int[] d : DIRECTIONS) {
                BlockVector3 next = node.position.add(d[0], d[1], d[2]);
                if (!visited.add(next) || !matches(world, next, mask, surfaceMode, plotMode)) continue;
                found.add(next);
                queue.addLast(new SearchNode(next, node.distance + 1));
            }
        }
        if (found.isEmpty()) {
            player.sendMessage(BTEGUtilities.PREFIX + "§7No adjacent blocks matched the mask.");
            return true;
        }

        Set<BlockVector2> footprint = new HashSet<>();
        int minY = selection.getMinimumPoint().y(), maxY = selection.getMaximumPoint().y();
        for (BlockVector3 p : found) {
            footprint.add(BlockVector2.at(p.x(), p.z()));
            minY = Math.min(minY, p.y());
            maxY = Math.max(maxY, p.y());
        }
        List<List<BlockVector2>> loops = traceBoundaries(footprint);
        // Convert cell-corner edges to the block columns immediately inside each edge.
        // Poly selector vertices are block coordinates, not geometric half-block corners;
        // leaving corners at x+1/z+1 makes the selection include an extra row/column.
        loops = loops.stream().map(this::edgeLoopToBlockCoordinates).map(this::simplifyCollinear).toList();
        List<BlockVector2> outline = joinLoops(loops);
        if (outline.size() < 3) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cThe expanded selection does not form a valid polygon.");
            return true;
        }
        localSession.setRegionSelector(world, new Polygonal2DRegionSelector(world, outline, minY, maxY));
        // Setting the server-side selector alone does not notify WorldEditCUI.
        localSession.dispatchCUISelection(BukkitAdapter.adapt(player));
        player.sendMessage(BTEGUtilities.PREFIX + "§aExpanded selection through " + found.size() + " matching block(s).");
        return true;
    }

    private boolean matches(com.sk89q.worldedit.world.World world, BlockVector3 position, Set<BlockType> mask, boolean surfaceMode, boolean plotMode) {
        BlockState state = world.getBlock(position);
        if (!mask.contains(state.getBlockType())) return false;
        if (!surfaceMode) return true;
        if (plotMode && isPlotMarker(state.getBlockType())) return true;

        // Treat a vertical run of mask blocks as one surface column. Any block in
        // the run is valid when the topmost matching block has air directly above.
        BlockVector3 above = position.add(0, 1, 0);
        while (mask.contains(world.getBlock(above).getBlockType())) above = above.add(0, 1, 0);
        BlockType aboveType = world.getBlock(above).getBlockType();
        return aboveType.getMaterial().isAir() || (plotMode && isSign(aboveType));
    }

    private boolean isPlotMarker(BlockType type) {
        return type == BlockTypes.CLAY || type == BlockTypes.LAPIS_BLOCK;
    }

    private boolean isSign(BlockType type) {
        return type.id().substring(type.id().indexOf(':') + 1).endsWith("sign");
    }

    /** Traces every directed edge between an occupied column and an empty column. */
    private List<List<BlockVector2>> traceBoundaries(Set<BlockVector2> cells) {
        List<Edge> edges = new ArrayList<>();
        for (BlockVector2 cell : cells) {
            int x = cell.x(), z = cell.z();
            if (!cells.contains(BlockVector2.at(x, z - 1))) edges.add(new Edge(x, z, x + 1, z));
            if (!cells.contains(BlockVector2.at(x + 1, z))) edges.add(new Edge(x + 1, z, x + 1, z + 1));
            if (!cells.contains(BlockVector2.at(x, z + 1))) edges.add(new Edge(x + 1, z + 1, x, z + 1));
            if (!cells.contains(BlockVector2.at(x - 1, z))) edges.add(new Edge(x, z + 1, x, z));
        }
        Map<Long, List<Integer>> outgoing = new HashMap<>();
        for (int i = 0; i < edges.size(); i++) outgoing.computeIfAbsent(key(edges.get(i).sx, edges.get(i).sz), ignored -> new ArrayList<>()).add(i);
        boolean[] used = new boolean[edges.size()];
        List<List<BlockVector2>> loops = new ArrayList<>();
        for (int start = 0; start < edges.size(); start++) {
            if (used[start]) continue;
            List<BlockVector2> loop = new ArrayList<>();
            int edgeIndex = start;
            Edge first = edges.get(start);
            loop.add(BlockVector2.at(first.sx, first.sz));
            int guard = edges.size() + 1;
            while (guard-- > 0 && !used[edgeIndex]) {
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
            if (loop.size() < 3) continue;
            if (signedArea(loop) >= 0) outerRings.add(loop);
            else holeRings.add(loop);
        }
        if (outerRings.isEmpty()) {
            // Degenerate orientation fallback: retain the largest traced outline as outer.
            List<BlockVector2> largest = loops.stream().max(Comparator.comparingDouble(loop -> Math.abs(signedArea(loop)))).orElseThrow();
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
            List<BlockVector2> loop = secondary.get(loopIndex);
            BlockVector2 loopAnchor = bridgePlan.secondaryPoints().get(loopIndex);
            loop = insertBoundaryPoint(loop, loopAnchor);
            rotateTo(loop, loopAnchor);
            main.add(loopAnchor);
            for (int i = 1; i < loop.size(); i++) main.add(loop.get(i));
            main.add(loopAnchor); // finish the complete ring
            main.add(anchor);     // bridge back before tracing the next ring
        }
        return main;
    }

    /** Picks the shortest bridge endpoints whose segment contains no intermediate block center. */
    private BridgePlan findBridgePlan(List<BlockVector2> main, List<List<BlockVector2>> secondary) {
        List<BlockVector2> mainCandidates = boundaryLatticePoints(main);
        List<List<BlockVector2>> candidatesByLoop = secondary.stream().map(this::boundaryLatticePoints).toList();
        long bestTotal = Long.MAX_VALUE;
        BlockVector2 bestMain = mainCandidates.getFirst();
        List<BlockVector2> bestSecondary = new ArrayList<>();
        for (BlockVector2 mainPoint : mainCandidates) {
            long total = 0;
            List<BlockVector2> destinations = new ArrayList<>();
            boolean viable = true;
            for (List<BlockVector2> candidates : candidatesByLoop) {
                BlockVector2 destination = null;
                long bestDistance = Long.MAX_VALUE;
                for (BlockVector2 candidate : candidates) {
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
                BlockVector2 destination = candidates.stream().min(Comparator.comparingLong(p -> squaredDistance(fallbackMain, p))).orElseThrow();
                bestSecondary.add(destination);
            }
        }
        return new BridgePlan(bestMain, bestSecondary);
    }

    private List<BlockVector2> boundaryLatticePoints(List<BlockVector2> loop) {
        LinkedHashSet<BlockVector2> points = new LinkedHashSet<>();
        for (int i = 0; i < loop.size(); i++) {
            BlockVector2 start = loop.get(i), end = loop.get((i + 1) % loop.size());
            int dx = end.x() - start.x(), dz = end.z() - start.z();
            int steps = (int) gcd(Math.abs((long) dx), Math.abs((long) dz));
            if (steps == 0) { points.add(start); continue; }
            int stepX = dx / steps, stepZ = dz / steps;
            for (int step = 0; step < steps; step++) points.add(BlockVector2.at(start.x() + step * stepX, start.z() + step * stepZ));
        }
        return new ArrayList<>(points);
    }

    private List<BlockVector2> insertBoundaryPoint(List<BlockVector2> source, BlockVector2 point) {
        List<BlockVector2> loop = new ArrayList<>(source);
        if (loop.contains(point)) return loop;
        for (int i = 0; i < loop.size(); i++) {
            BlockVector2 start = loop.get(i), end = loop.get((i + 1) % loop.size());
            int dx = end.x() - start.x(), dz = end.z() - start.z();
            int steps = (int) gcd(Math.abs((long) dx), Math.abs((long) dz));
            if (steps == 0) continue;
            int stepX = dx / steps, stepZ = dz / steps;
            for (int step = 1; step < steps; step++) {
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
            changed = false;
            for (int i = 0; i < points.size(); i++) {
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
        if (!sender.hasPermission("bteg.builder") || args.length == 0) return List.of();
        String current = args[args.length - 1];
        if (args.length >= 2) {
            return Stream.of("surface", "ground")
                    .filter(option -> option.startsWith(current.toLowerCase(Locale.ROOT))).toList();
        }
        int comma = current.lastIndexOf(',');
        String prefix = comma < 0 ? "" : current.substring(0, comma + 1);
        String partial = comma < 0 ? current : current.substring(comma + 1);
        List<String> suggestions = new ArrayList<>();
        if (comma < 0 && "auto".startsWith(partial.toLowerCase(Locale.ROOT))) suggestions.add("auto");
        suggestions.addAll(de.btegermany.utilities.util.TabUtil.getBlockPatternSuggestions(partial, true).stream()
                .map(suggestion -> prefix + suggestion).toList());
        return suggestions.stream().distinct().toList();
    }

    private record SearchNode(BlockVector3 position, int distance) {}
}
