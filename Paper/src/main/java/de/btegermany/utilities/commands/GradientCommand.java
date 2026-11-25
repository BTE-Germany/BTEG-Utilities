package de.btegermany.utilities.commands;

import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import de.btegermany.utilities.BTEGUtilities;
import de.btegermany.utilities.util.TabUtil;
import de.btegermany.utilities.util.worldedit.Converter;
import de.btegermany.utilities.util.worldedit.SelectionEditSession;
import de.btegermany.utilities.util.worldedit.WorldEditUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Applies an ordered block gradient across or inward/outward through a cuboid or polygon selection. */
public class GradientCommand implements TabExecutor {
    private static final Set<String> DIRECTIONS = Set.of("north", "south", "west", "east", "ne", "nw", "se", "sw", "up", "down", "inwards", "outwards");

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player) || !command.getName().equalsIgnoreCase("gradient")) return true;
        if (!player.hasPermission("bteg.builder")) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cNo permission for //gradient");
            return true;
        }
        boolean hasTransition = args.length > 0 && (args[args.length - 1].equalsIgnoreCase("clean") || args[args.length - 1].equalsIgnoreCase("grainy"));
        int argumentCount = args.length - (hasTransition ? 1 : 0);
        if (argumentCount != 2 && argumentCount != 3) {
            player.sendMessage(BTEGUtilities.PREFIX + "Usage: //gradient [replace-blocks] <block[,block...]> <direction> [clean|grainy]");
            return true;
        }

        String transition = hasTransition ? args[args.length - 1].toLowerCase(Locale.ROOT) : "grainy";
        boolean hasMask = argumentCount == 3;
        String listArg = args[hasMask ? 1 : 0];
        String rawDirection = args[hasMask ? 2 : 1].toLowerCase(Locale.ROOT);
        String direction = normalizeDirection(rawDirection);
        if (direction == null) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cInvalid direction. Use north, south, west, east, ne, nw, se, sw, up, down, inwards, or outwards.");
            return true;
        }

        List<BlockState> blocks = new ArrayList<>();
        try {
            for (String block : splitBlockList(listArg)) {
                if (block.isBlank()) throw new IllegalArgumentException();
                blocks.add(Converter.getBlockState(block, player));
            }
        } catch (RuntimeException exception) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cInvalid gradient block list: " + listArg);
            return true;
        }

        Set<BlockType> mask = null;
        if (hasMask) {
            try {
                Set<BlockType> types = new HashSet<>();
                for (String block : splitBlockList(args[0])) types.add(Converter.getBlockType(block));
                mask = types;
            } catch (RuntimeException exception) {
                player.sendMessage(BTEGUtilities.PREFIX + "§cInvalid replace block list: " + args[0]);
                return true;
            }
        }

        Set<BlockType> finalMask = mask;
        try {
            WorldEditUtil.findSelection(player, session -> apply(session, blocks, direction, transition, finalMask));
        } catch (MaxChangedBlocksException | EmptyClipboardException exception) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cAn error occurred while applying the gradient.");
            exception.printStackTrace();
        }
        return true;
    }

    private String normalizeDirection(String input) {
        if (Set.of("ne", "nw", "se", "sw").contains(input)) return input;
        if (input.startsWith("northe")) return "ne";
        if (input.startsWith("northw")) return "nw";
        if (input.startsWith("southe")) return "se";
        if (input.startsWith("southw")) return "sw";
        for (String direction : List.of("north", "south", "west", "east", "up", "down", "inwards", "outwards")) {
            if (direction.startsWith(input)) return direction;
        }
        return null;
    }

    /** Splits block lists on commas outside WorldEdit's square-bracket block states. */
    private List<String> splitBlockList(String input) {
        List<String> blocks = new ArrayList<>();
        int bracketDepth = 0;
        int start = 0;
        for (int i = 0; i < input.length(); i++) {
            char current = input.charAt(i);
            if (current == '[') bracketDepth++;
            else if (current == ']' && bracketDepth > 0) bracketDepth--;
            else if (current == ',' && bracketDepth == 0) {
                blocks.add(input.substring(start, i));
                start = i + 1;
            }
        }
        blocks.add(input.substring(start));
        return blocks;
    }

    private List<String> blockListSuggestions(String input) {
        int bracketDepth = 0;
        int lastSeparator = -1;
        for (int i = 0; i < input.length(); i++) {
            char current = input.charAt(i);
            if (current == '[') bracketDepth++;
            else if (current == ']' && bracketDepth > 0) bracketDepth--;
            else if (current == ',' && bracketDepth == 0) lastSeparator = i;
        }
        String prefix = input.substring(0, lastSeparator + 1);
        String partialBlock = input.substring(lastSeparator + 1);
        return TabUtil.getBlockPatternSuggestions(partialBlock, true).stream()
                .map(suggestion -> prefix + suggestion)
                .toList();
    }

    private void apply(SelectionEditSession session, List<BlockState> blocks, String direction, String transition, Set<BlockType> mask) {
        Player player = session.player();
        Region region = session.region();
        if (!(region instanceof CuboidRegion) && !(region instanceof Polygonal2DRegion)) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cPlease use a cuboid or polygon selection for //gradient!");
            return;
        }
        BlockVector3 min = region.getMinimumPoint(), max = region.getMaximumPoint();
        int[] vector = directionVector(direction);
        boolean radial = direction.equals("inwards") || direction.equals("outwards");
        List<BlockVector3> positions = new ArrayList<>();
        Map<BlockVector3, Integer> radialDistances = new HashMap<>();
        Map<BlockVector3, Integer> localMaxima = new HashMap<>();
        if (radial) {
            collectPositions(region, min, max, positions);
            if (region instanceof Polygonal2DRegion polygon) {
                radialDistances.putAll(polygonBoundaryDistances(polygon, positions, min, max));
            } else {
                for (BlockVector3 position : positions) {
                    int distance = boundaryDistance(position, min, max);
                    radialDistances.put(position, distance);
                }
            }
            localMaxima.putAll(findReachableLocalMaxima(positions, radialDistances));
        } else {
            collectPositions(region, min, max, positions);
        }
        int totalLayers = Math.abs(vector[0]) * (max.x()-min.x()) + Math.abs(vector[1]) * (max.y()-min.y()) + Math.abs(vector[2]) * (max.z()-min.z()) + 1;
        int changed = 0;
        int minProjection = vector[0] * (vector[0] < 0 ? max.x() : min.x()) + vector[1] * (vector[1] < 0 ? max.y() : min.y()) + vector[2] * (vector[2] < 0 ? max.z() : min.z());
        for (BlockVector3 position : positions) {
            BlockType currentType = session.editSession().getBlock(position).getBlockType();
            if (mask == null ? currentType.getMaterial().isAir() : !mask.contains(currentType)) continue;
            int layer;
            int layers = totalLayers;
            if (radial) {
                int distance = radialDistances.get(position);
                int localMaximum = localMaxima.get(position);
                layer = direction.equals("inwards") ? distance : localMaximum - distance;
                layers = localMaximum + 1;
            } else {
                int projection = vector[0] * position.x() + vector[1] * position.y() + vector[2] * position.z();
                layer = projection - minProjection;
            }
            int index = selectBlock(layer, layers, blocks.size(), transition);
            BlockState block = blocks.get(index);
            if (session.editSession().setBlock(position.x(), position.y(), position.z(), block)) {
                session.changedBlocks().put(position, block);
                changed++;
            }
        }
        player.sendMessage(BTEGUtilities.PREFIX + "Applied gradient to §6§l" + changed + " §r§7block(s).");
    }

    /** Propagates each position's highest reachable distance through strictly increasing neighbors. */
    private Map<BlockVector3, Integer> findReachableLocalMaxima(List<BlockVector3> positions,
                                                                 Map<BlockVector3, Integer> distances) {
        List<BlockVector3> descending = new ArrayList<>(positions);
        descending.sort(Comparator.comparingInt((BlockVector3 p) -> distances.get(p)).reversed());
        Map<BlockVector3, Integer> maxima = new HashMap<>(positions.size());
        int[][] neighbors = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        for (BlockVector3 position : descending) {
            int distance = distances.get(position);
            int maximum = distance;
            for (int[] offset : neighbors) {
                BlockVector3 neighbor = position.add(offset[0], offset[1], offset[2]);
                Integer neighborDistance = distances.get(neighbor);
                if (neighborDistance != null && neighborDistance > distance) {
                    maximum = Math.max(maximum, maxima.get(neighbor));
                }
            }
            maxima.put(position, maximum);
        }
        return maxima;
    }

    private int selectBlock(int layer, int totalLayers, int blockCount, String transition) {
        if (blockCount == 1 || totalLayers <= 1) return 0;
        if (transition.equals("clean")) {
            return Math.min(blockCount - 1, (int) ((long) layer * blockCount / totalLayers));
        }
        NavigableMap<Integer, Integer> anchors = new TreeMap<>();
        for (int i = 0; i < blockCount; i++) {
            int anchor = (int) Math.round((double) i * (totalLayers - 1) / (blockCount - 1));
            anchors.putIfAbsent(anchor, i);
        }
        Map.Entry<Integer, Integer> lower = anchors.floorEntry(layer);
        Map.Entry<Integer, Integer> upper = anchors.ceilingEntry(layer);
        if (lower == null) return anchors.firstEntry().getValue();
        if (upper == null) return anchors.lastEntry().getValue();
        if (lower.getKey().equals(upper.getKey())) return lower.getValue();
        double upperChance = (double) (layer - lower.getKey()) / (upper.getKey() - lower.getKey());
        return ThreadLocalRandom.current().nextDouble() < upperChance ? upper.getValue() : lower.getValue();
    }

    /** Iterating the enclosing bounds and checking contains is consistent across FAWE region types. */
    private void collectPositions(Region region, BlockVector3 min, BlockVector3 max, List<BlockVector3> positions) {
        for (int x = min.x(); x <= max.x(); x++) {
            for (int y = min.y(); y <= max.y(); y++) {
                for (int z = min.z(); z <= max.z(); z++) {
                    BlockVector3 position = BlockVector3.at(x, y, z);
                    if (region.contains(position)) positions.add(position);
                }
            }
        }
    }

    private int boundaryDistance(BlockVector3 position, BlockVector3 min, BlockVector3 max) {
        int distance = Integer.MAX_VALUE;
        if (max.x() - min.x() > 1) distance = Math.min(distance, Math.min(position.x() - min.x(), max.x() - position.x()));
        if (max.z() - min.z() > 1) distance = Math.min(distance, Math.min(position.z() - min.z(), max.z() - position.z()));
        return distance == Integer.MAX_VALUE ? 0 : distance;
    }

    /** Seeds the polygon outline, then spreads inward one six-connected block at a time. */
    private Map<BlockVector3, Integer> polygonBoundaryDistances(Polygonal2DRegion polygon, List<BlockVector3> positions,
                                                                BlockVector3 min, BlockVector3 max) {
        Set<BlockVector3> selected = new HashSet<>(positions);
        Map<BlockVector3, Integer> distances = new HashMap<>();
        ArrayDeque<BlockVector3> queue = new ArrayDeque<>();
        List<com.sk89q.worldedit.math.BlockVector2> points = polygon.getPoints();
        for (int i = 0; i < points.size(); i++) {
            var a = points.get(i);
            var b = points.get((i + 1) % points.size());
            int steps = Math.max(Math.abs(b.x() - a.x()), Math.abs(b.z() - a.z()));
            for (int step = 0; step <= steps; step++) {
                double t = steps == 0 ? 0 : (double) step / steps;
                int x = (int) Math.round(a.x() + (b.x() - a.x()) * t);
                int z = (int) Math.round(a.z() + (b.z() - a.z()) * t);
                for (int y = min.y(); y <= max.y(); y++) {
                    BlockVector3 boundary = BlockVector3.at(x, y, z);
                    if (selected.contains(boundary) && distances.putIfAbsent(boundary, 0) == null) queue.add(boundary);
                }
            }
        }
        // Polygon vertices describe a continuous outline, while the selection contains only
        // block centers inside it. Include any selected block that actually borders an empty
        // horizontal neighbor so rasterization rounding cannot leave a gap in the seed line.
        int[][] horizontalNeighbors = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}};
        for (BlockVector3 position : positions) {
            for (int[] offset : horizontalNeighbors) {
                if (!selected.contains(position.add(offset[0], 0, offset[2]))) {
                    if (distances.putIfAbsent(position, 0) == null) queue.addLast(position);
                    break;
                }
            }
        }
        // Only the vertical outline walls seed this distance field, not the top or bottom faces.
        int[][] neighbors = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        while (!queue.isEmpty()) {
            BlockVector3 current = queue.removeFirst();
            int nextDistance = distances.get(current) + 1;
            for (int[] offset : neighbors) {
                BlockVector3 next = current.add(offset[0], offset[1], offset[2]);
                if (selected.contains(next) && distances.putIfAbsent(next, nextDistance) == null) queue.addLast(next);
            }
        }
        return distances;
    }

    private int[] directionVector(String direction) {
        return switch (direction) {
            case "north" -> new int[]{0, 0, -1}; case "south" -> new int[]{0, 0, 1};
            case "west" -> new int[]{-1, 0, 0}; case "east" -> new int[]{1, 0, 0};
            case "ne" -> new int[]{1, 0, -1}; case "nw" -> new int[]{-1, 0, -1};
            case "se" -> new int[]{1, 0, 1}; case "sw" -> new int[]{-1, 0, 1};
            case "up" -> new int[]{0, 1, 0}; case "down" -> new int[]{0, -1, 0};
            default -> new int[]{0, 0, 0};
        };
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("bteg.builder")) return Collections.emptyList();
        if (args.length == 1) return blockListSuggestions(args[0]);
        if (args.length == 2) {
            List<String> suggestions = new ArrayList<>(blockListSuggestions(args[1]));
            suggestions.addAll(DIRECTIONS.stream().filter(d -> d.startsWith(args[1].toLowerCase(Locale.ROOT))).toList());
            return suggestions;
        }
        if (args.length == 3) {
            List<String> suggestions = new ArrayList<>(DIRECTIONS.stream().filter(d -> d.startsWith(args[2].toLowerCase(Locale.ROOT))).toList());
            suggestions.addAll(List.of("clean", "grainy").stream().filter(mode -> mode.startsWith(args[2].toLowerCase(Locale.ROOT))).toList());
            return suggestions;
        }
        if (args.length == 4) return List.of("clean", "grainy").stream().filter(mode -> mode.startsWith(args[3].toLowerCase(Locale.ROOT))).toList();
        return Collections.emptyList();
    }
}
