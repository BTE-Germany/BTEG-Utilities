package de.btegermany.utilities.commands;

import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import de.btegermany.utilities.BTEGUtilities;
import de.btegermany.utilities.util.TabUtil;
import de.btegermany.utilities.util.worldedit.Converter;
import de.btegermany.utilities.util.worldedit.SelectionEditSession;
import de.btegermany.utilities.util.worldedit.WorldEditUtil;
import de.btegermany.utilities.util.CommandSound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.*;

/**
 * Replaces marker blocks in a WorldEdit selection with curb stairs and solid curb blocks.
 *
 * <p>The command first resolves the marker and curb materials. Processing then follows these
 * stages: classify buried markers as solid fill; find and trace the visible marker segments;
 * determine each segment's neighboring segments and plan its base replacement; refine crossings
 * and singleton segments; finally write all planned states to the edit session. Keeping discovery
 * and placement separate lets each special shape use information from the whole selection.</p>
 */
public class CurbCommand implements TabExecutor {
    /** Validates the command, prepares the block states, and runs the replacement for the selection. */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player) || !command.getName().equalsIgnoreCase("curb")) return true;
        if (!player.hasPermission("bteg.builder")) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cNo permission for //curb");
            return true;
        }
        if (args.length != 2) {
            player.sendMessage(BTEGUtilities.PREFIX + "Usage: //curb <marker> <material>");
            return true;
        }

        BlockType marker;
        CurbVariants curbVariants;
        Map<String, BlockState> stairStates;

        try {
            marker = Converter.getBlockType(args[0], player);
            curbVariants = resolveCurbVariants(args[1]);

            stairStates = new HashMap<>();
            for (String facing : List.of("north", "east", "south", "west")) {
                for (String shape : List.of("straight", "inner_left", "inner_right", "outer_left", "outer_right")) {
                    stairStates.put(stairStateKey(facing, shape), Converter.getBlockState(
                            curbVariants.stairs().id() + "[facing=" + facing + ",shape=" + shape + "]", player));
                }
            }
        } catch (RuntimeException exception) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cInvalid marker or curb material; no matching stairs and solid block were found.");
            return true;
        }

        try {
            WorldEditUtil.findSelection(player, session -> replaceCurbs(session, marker,
                    curbVariants.solid().getDefaultState(), stairStates));
        } catch (MaxChangedBlocksException | EmptyClipboardException exception) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cAn error occurred while processing the curbs.");
            exception.printStackTrace();
        }
        return true;
    }

    /** Resolves a material name to its stair block and matching solid curb block. */
    private CurbVariants resolveCurbVariants(String materialInput) {
        String input = materialInput.toLowerCase(Locale.ROOT);
        String namespace = "minecraft";
        int namespaceSeparator = input.indexOf(':');
        if (namespaceSeparator >= 0) {
            namespace = input.substring(0, namespaceSeparator);
            input = input.substring(namespaceSeparator + 1);
        }
        if (input.endsWith("_stairs")) input = input.substring(0, input.length() - "_stairs".length());
        if (input.isBlank()) throw new IllegalArgumentException("Material name is empty");

        BlockType stairs = BlockTypes.get(namespace + ":" + input + "_stairs");
        if (stairs == null) throw new IllegalArgumentException("No stairs variant for " + materialInput);

        BlockType solid = BlockTypes.get(namespace + ":" + input + "_planks");
        if (solid == null) solid = BlockTypes.get(namespace + ":" + input);
        if (solid == null) solid = BlockTypes.get(namespace + ":" + input + "_block");
        if (solid == null) solid = BlockTypes.get(namespace + ":" + input + "s");
        if (solid == null) throw new IllegalArgumentException("No solid block variant for " + materialInput);
        return new CurbVariants(solid, stairs);
    }

    /** Returns tab-completable materials that have both supported block variants. */
    private List<String> getCurbMaterialSuggestions(String prefix) {
        String normalizedPrefix = prefix.toLowerCase(Locale.ROOT);
        Set<String> suggestions = new TreeSet<>();
        for (BlockType blockType : BlockType.REGISTRY.values()) {
            String id = blockType.id();
            int separator = id.indexOf(':');
            String namespace = separator < 0 ? "minecraft" : id.substring(0, separator);
            String path = separator < 0 ? id : id.substring(separator + 1);
            if (!path.endsWith("_stairs")) continue;

            String material = path.substring(0, path.length() - "_stairs".length());
            String suggestion = namespace.equals("minecraft") ? material : namespace + ":" + material;
            try {
                resolveCurbVariants(suggestion);
                if (suggestion.startsWith(normalizedPrefix)) suggestions.add(suggestion);
            } catch (IllegalArgumentException ignored) {
                // Omit stair types that have no matching solid block naming variant.
            }
        }
        return new ArrayList<>(suggestions);
    }

    /** The two block types generated from the command's single curb material argument. */
    private record CurbVariants(BlockType solid, BlockType stairs) {}

    /**
     * Builds the replacement plan for the selection. Buried markers are excluded from discovery,
     * while visible markers are grouped into segments. Segment and neighbor data is collected
     * before base states, intersection shapes, and singleton shapes are resolved and written.
     */
    private void replaceCurbs(SelectionEditSession session, BlockType marker, BlockState solidState,
                              Map<String, BlockState> stairStates) {
        Player player = session.player();
        Region region = session.region();

        int changed = 0;
        LinkedHashSet<BlockVector3> remainingBlocks = new LinkedHashSet<>();
        Map<BlockVector3, BlockState> plannedReplacements = new LinkedHashMap<>();
        Map<BlockVector3, String> plannedFacings = new HashMap<>();
        Map<BlockVector3, EnumMap<LineSegment.Axis, String>> plannedAxisFacings = new HashMap<>();
        List<LineSegment> segments = new ArrayList<>();
        Map<BlockVector3, Set<SegmentDirection>> singletonNeighbors = new HashMap<>();
        region.forEach(pos -> {
            if (region.getWorld().getBlock(pos).getBlockType().equals(marker)) {
                // FAWE's region iterator reuses a mutable vector; copy it before using it as a key.
                BlockVector3 copiedPosition = BlockVector3.at(pos.x(), pos.y(), pos.z());
                BlockVector3 above = copiedPosition.add(0, 1, 0);
                if (region.getWorld().getBlock(above).getBlockType().equals(marker)) {
                    // Buried marker blocks are solid curb fill, not part of the surface layout.
                    plannedReplacements.put(copiedPosition, solidState);
                } else {
                    remainingBlocks.add(copiedPosition);
                }
            }
        });
        Set<BlockVector3> markerPositions = new HashSet<>(remainingBlocks);

        // Claim each visible marker once, while allowing traces to pass through claimed junctions.
        while (!remainingBlocks.isEmpty()) {
            BlockVector3 pos = remainingBlocks.getFirst();
            remainingBlocks.remove(pos);

            // Group markers along the axis established by the first valid neighbor.
            LineSegment segment = new LineSegment(pos);
            // A marker already assigned to another run can still connect this run through an intersection.
            BlockVector3 firstNeighbor = findNeighbor(session, region, pos, marker, 0, 0, markerPositions);

            if (firstNeighbor != null) {
                int stepX = Integer.signum(firstNeighbor.x() - pos.x());
                int stepZ = Integer.signum(firstNeighbor.z() - pos.z());
                segment.setAxis(stepX == 0 ? LineSegment.Axis.Z : LineSegment.Axis.X);
                segment.add(firstNeighbor);
                remainingBlocks.remove(firstNeighbor);

                traceDirection(session, region, marker, segment, firstNeighbor, stepX, stepZ,
                        remainingBlocks, markerPositions);
                traceDirection(session, region, marker, segment, pos, -stepX, -stepZ,
                        remainingBlocks, markerPositions);
            } else {
                checkEndpointDiagonals(session, region, marker, segment, pos, 0, 0, markerPositions);
            }

            segments.add(segment);
        }

        Map<BlockVector3, List<LineSegment>> segmentsByBlock = indexSegmentsByBlock(segments);
        // Neighbor lengths and all axis facings are now available for replacement decisions.
        for (LineSegment segment : segments) {
            segment.resolveNeighborLengths(segmentsByBlock);
            plannedReplacements.putAll(getSegmentReplacements(
                    segment, solidState, stairStates, plannedFacings, plannedAxisFacings));
            if (segment.blocks().size() == 1) {
                singletonNeighbors.put(segment.blocks().getFirst(), new HashSet<>(segment.neighboringDirections()));
            }
        }

        // Special cases override the regular straight-stair plan before any world blocks are changed.
        applyIntersectionShapes(plannedReplacements, plannedFacings, plannedAxisFacings, markerPositions,
                region, marker, solidState, stairStates);
        applySingletonShapes(plannedReplacements, singletonNeighbors, plannedFacings,
                plannedAxisFacings, markerPositions, region, marker, solidState, stairStates);

        for (Map.Entry<BlockVector3, BlockState> replacement : plannedReplacements.entrySet()) {
            BlockVector3 position = replacement.getKey();
            session.editSession().setBlock(position.x(), position.y(), position.z(), replacement.getValue());
            session.changedBlocks().put(replacement.getKey(), replacement.getValue());
            changed++;
        }

        CommandSound.playSuccess(player);
        player.sendMessage(BTEGUtilities.PREFIX + "Replaced §6§l" + changed + " §r§7curb marker block(s).");
    }

    /** Orders a run, chooses how its halves divide, and plans each block's base curb state. */
    private Map<BlockVector3, BlockState> getSegmentReplacements(LineSegment segment, BlockState solidState,
                                                                  Map<String, BlockState> stairStates,
                                                                  Map<BlockVector3, String> plannedFacings,
                                                                  Map<BlockVector3, EnumMap<LineSegment.Axis, String>> plannedAxisFacings) {
        Map<BlockVector3, BlockState> replacements = new LinkedHashMap<>();
        if (segment.blocks().size() == 1) {
            BlockVector3 position = segment.blocks().getFirst();
            // An isolated singleton defaults to the existing z-axis curb orientation.
            replacements.put(position, stairStates.get(stairStateKey("west", "straight")));
            return replacements;
        }

        segment.blocks().sort(Comparator.comparingInt(block ->
                segment.axis() == LineSegment.Axis.X ? block.x() : block.z()));
        int split = segment.blocks().size() / 2;

        boolean firstFirstSide, firstSecondSide, secondFirstSide, secondSecondSide;

        if (segment.axis() == LineSegment.Axis.Z) {
            // Ordered north to south: NW/NE apply to the north half, SW/SE to the south half.
            firstFirstSide = segment.hasNeighbor(SegmentDirection.NW);
            firstSecondSide = segment.hasNeighbor(SegmentDirection.NE);
            secondFirstSide = segment.hasNeighbor(SegmentDirection.SW);
            secondSecondSide = segment.hasNeighbor(SegmentDirection.SE);
        } else {
            // Ordered west to east: NW/SW apply to the west half, NE/SE to the east half.
            firstFirstSide = segment.hasNeighbor(SegmentDirection.NW);
            firstSecondSide = segment.hasNeighbor(SegmentDirection.SW);
            secondFirstSide = segment.hasNeighbor(SegmentDirection.NE);
            secondSecondSide = segment.hasNeighbor(SegmentDirection.SE);
        }

        if (segment.blocks().size() % 2 == 1) {
            Set<SegmentDirection> firstHalfDirections = segment.axis() == LineSegment.Axis.Z
                    ? EnumSet.of(SegmentDirection.NW, SegmentDirection.NE)
                    : EnumSet.of(SegmentDirection.NW, SegmentDirection.SW);
            Set<SegmentDirection> secondHalfDirections = segment.axis() == LineSegment.Axis.Z
                    ? EnumSet.of(SegmentDirection.SW, SegmentDirection.SE)
                    : EnumSet.of(SegmentDirection.NE, SegmentDirection.SE);
            int firstNeighborLength = segment.shortestNeighborLength(firstHalfDirections);
            int secondNeighborLength = segment.shortestNeighborLength(secondHalfDirections);
            if (firstNeighborLength == 0
                    || (secondNeighborLength > 0 && firstNeighborLength > secondNeighborLength)) {
                split++;
            }
        }

        String firstFacing = selectHalfFacing(firstFirstSide, firstSecondSide,
                secondFirstSide, secondSecondSide, segment.axis());
        String secondFacing = selectHalfFacing(secondFirstSide, secondSecondSide,
                firstFirstSide, firstSecondSide, segment.axis());
        for (int i = 0; i < segment.blocks().size(); i++) {
            boolean firstHalf = i < split;
            boolean hasFirstSide = firstHalf ? firstFirstSide : secondFirstSide;
            boolean hasSecondSide = firstHalf ? firstSecondSide : secondSecondSide;
            String facing = firstHalf ? firstFacing : secondFacing;
            BlockState state = hasFirstSide && hasSecondSide
                    ? solidState
                    : stairStates.get(stairStateKey(facing, "straight"));
            BlockVector3 position = segment.blocks().get(i);
            plannedFacings.put(position, facing);
            plannedAxisFacings.computeIfAbsent(position, ignored -> new EnumMap<>(LineSegment.Axis.class))
                    .put(segment.axis(), facing);
            replacements.put(position, state);
        }
        return replacements;
    }

    /** Indexes every marker coordinate to the segment or segments that contain it. */
    private Map<BlockVector3, List<LineSegment>> indexSegmentsByBlock(List<LineSegment> segments) {
        Map<BlockVector3, List<LineSegment>> segmentsByBlock = new HashMap<>();
        for (LineSegment segment : segments) {
            for (BlockVector3 block : segment.blocks()) {
                segmentsByBlock.computeIfAbsent(block, ignored -> new ArrayList<>()).add(segment);
            }
        }
        return segmentsByBlock;
    }

    /** Applies the cardinal-neighbor rules for bends, T-junctions, and four-way crossings. */
    private void applyIntersectionShapes(Map<BlockVector3, BlockState> replacements,
                                         Map<BlockVector3, String> plannedFacings,
                                         Map<BlockVector3, EnumMap<LineSegment.Axis, String>> plannedAxisFacings,
                                         Set<BlockVector3> markerPositions,
                                         Region region, BlockType marker, BlockState solidState,
                                         Map<String, BlockState> stairStates) {
        for (BlockVector3 position : replacements.keySet()) {
            EnumMap<CardinalDirection, BlockVector3> neighbors = getCardinalMarkerNeighbors(
                    region, marker, position, markerPositions);
            int neighborCount = neighbors.size();

            if (neighborCount == 2 && hasOneNeighborOnEachAxis(neighbors.keySet())) {
                CardinalDirection facing = clockwiseLater(neighbors.keySet());
                replacements.put(position, stairStates.get(stairStateKey(facing.facing(), "outer_left")));
            } else if (neighborCount == 3) {
                applyThreeNeighborShape(position, neighbors, replacements, plannedFacings,
                        plannedAxisFacings, stairStates);
            } else if (neighborCount == 4) {
                applyFourNeighborShape(position, neighbors, replacements, plannedFacings, solidState, stairStates);
            }
        }
    }

    /** Finds at most one visible marker per horizontal cardinal direction, checking above first. */
    private EnumMap<CardinalDirection, BlockVector3> getCardinalMarkerNeighbors(Region region, BlockType marker,
                                                                                BlockVector3 position,
                                                                                Set<BlockVector3> markerPositions) {
        EnumMap<CardinalDirection, BlockVector3> neighbors = new EnumMap<>(CardinalDirection.class);
        for (CardinalDirection direction : CardinalDirection.values()) {
            for (int dy : new int[]{1, 0, -1}) {
                BlockVector3 neighbor = position.add(direction.dx(), dy, direction.dz());
                if (markerPositions.contains(neighbor) && region.contains(neighbor)
                        && region.getWorld().getBlock(neighbor).getBlockType().equals(marker)) {
                    neighbors.put(direction, neighbor);
                    break;
                }
            }
        }
        return neighbors;
    }

    /** Whether the neighbor set includes at least one X and one Z direction. */
    private boolean hasOneNeighborOnEachAxis(Set<CardinalDirection> neighbors) {
        boolean hasXNeighbor = false;
        boolean hasZNeighbor = false;
        for (CardinalDirection direction : neighbors) {
            if (direction.dx() != 0) hasXNeighbor = true;
            if (direction.dz() != 0) hasZNeighbor = true;
        }
        return hasXNeighbor && hasZNeighbor;
    }

    /** Uses the opposing pair as the through-axis, then shapes the third connection. */
    private void applyThreeNeighborShape(BlockVector3 position,
                                         EnumMap<CardinalDirection, BlockVector3> neighbors,
                                         Map<BlockVector3, BlockState> replacements,
                                         Map<BlockVector3, String> plannedFacings,
                                         Map<BlockVector3, EnumMap<LineSegment.Axis, String>> plannedAxisFacings,
                                         Map<String, BlockState> stairStates) {
        boolean hasNorthSouthLine = neighbors.containsKey(CardinalDirection.NORTH)
                && neighbors.containsKey(CardinalDirection.SOUTH);
        CardinalDirection thirdNeighbor;
        String defaultFacing;
        if (hasNorthSouthLine) {
            thirdNeighbor = neighbors.containsKey(CardinalDirection.EAST)
                    ? CardinalDirection.EAST : CardinalDirection.WEST;
            defaultFacing = "west";
        } else {
            thirdNeighbor = neighbors.containsKey(CardinalDirection.NORTH)
                    ? CardinalDirection.NORTH : CardinalDirection.SOUTH;
            defaultFacing = "north";
        }

        LineSegment.Axis throughAxis = hasNorthSouthLine ? LineSegment.Axis.Z : LineSegment.Axis.X;
        Map<LineSegment.Axis, String> axisFacings = plannedAxisFacings.get(position);
        String facing = axisFacings == null ? null : axisFacings.get(throughAxis);
        if (facing == null) facing = plannedFacings.getOrDefault(position, defaultFacing);
        String shape = "straight";
        if (oppositeFacing(facing).equals(thirdNeighbor.facing())) {
            String adjacentFacing = plannedFacings.get(neighbors.get(thirdNeighbor));
            if (adjacentFacing != null) shape = innerShape(facing, adjacentFacing);
        }
        replacements.put(position, stairStates.get(stairStateKey(facing, shape)));
        plannedFacings.put(position, facing);
    }

    /** Chooses a full block or stair state from the facings of all four adjacent stairs. */
    private void applyFourNeighborShape(BlockVector3 position,
                                        EnumMap<CardinalDirection, BlockVector3> neighbors,
                                        Map<BlockVector3, BlockState> replacements,
                                        Map<BlockVector3, String> plannedFacings,
                                        BlockState solidState,
                                        Map<String, BlockState> stairStates) {
        Set<String> distinctFacings = new HashSet<>();
        for (BlockVector3 neighbor : neighbors.values()) {
            String facing = plannedFacings.get(neighbor);
            if (facing != null) distinctFacings.add(facing);
        }

        if (distinctFacings.size() >= 3) {
            replacements.put(position, solidState);
        } else if (distinctFacings.size() == 2) {
            Set<CardinalDirection> facings = new HashSet<>();
            for (String facing : distinctFacings) facings.add(CardinalDirection.fromFacing(facing));
            String clockwiseFacing = clockwiseLater(facings).facing();
            replacements.put(position, stairStates.get(stairStateKey(clockwiseFacing, "inner_left")));
        } else if (distinctFacings.size() == 1) {
            String facing = distinctFacings.iterator().next();
            replacements.put(position, stairStates.get(stairStateKey(facing, "straight")));
        }
    }

    /** Resolves a one-block segment from its diagonal segment neighbors and their axes. */
    private void applySingletonShapes(Map<BlockVector3, BlockState> replacements,
                                      Map<BlockVector3, Set<SegmentDirection>> singletonNeighbors,
                                      Map<BlockVector3, String> plannedFacings,
                                      Map<BlockVector3, EnumMap<LineSegment.Axis, String>> plannedAxisFacings,
                                      Set<BlockVector3> markerPositions, Region region,
                                      BlockType marker, BlockState solidState,
                                      Map<String, BlockState> stairStates) {
        for (Map.Entry<BlockVector3, Set<SegmentDirection>> entry : singletonNeighbors.entrySet()) {
            BlockVector3 position = entry.getKey();
            Set<SegmentDirection> neighbors = entry.getValue();
            if (neighbors.size() == 4) {
                replacements.put(position, solidState);
            } else if (neighbors.size() == 3) {
                SegmentDirection freeCorner = Arrays.stream(SegmentDirection.values())
                        .filter(direction -> !neighbors.contains(direction))
                        .findFirst().orElseThrow();
                CardinalDirection facing = previousCardinal(freeCorner).counterClockwise();
                replacements.put(position, stairStates.get(stairStateKey(facing.facing(), "inner_left")));
                plannedFacings.put(position, facing.facing());
            } else if (neighbors.size() == 2) {
                CardinalDirection facing;
                String shape;
                if (areOpposingCorners(neighbors)) {
                    SegmentDirection axisReference = neighbors.contains(SegmentDirection.SW)
                            ? SegmentDirection.SW : SegmentDirection.NW;
                    BlockVector3 neighborPosition = findDiagonalMarker(
                            region, marker, position, axisReference, markerPositions);
                    LineSegment.Axis axis = neighborPosition == null ? null
                            : findPlannedAxis(neighborPosition, plannedFacings, plannedAxisFacings);
                    facing = resolveOpposingCornerFacing(axisReference, axis);
                    shape = "inner_left";
                } else {
                    facing = commonCornerDirection(neighbors);
                    shape = "straight";
                }
                replacements.put(position, stairStates.get(stairStateKey(facing.facing(), shape)));
                plannedFacings.put(position, facing.facing());
            } else if (neighbors.size() == 1) {
                SegmentDirection neighborDirection = neighbors.iterator().next();
                BlockVector3 neighborPosition = findDiagonalMarker(
                        region, marker, position, neighborDirection, markerPositions);
                LineSegment.Axis axis = neighborPosition == null ? null
                        : findPlannedAxis(neighborPosition, plannedFacings, plannedAxisFacings);
                CardinalDirection facing;
                if (axis == LineSegment.Axis.Z) {
                    facing = neighborDirection.isWestSide() ? CardinalDirection.WEST : CardinalDirection.EAST;
                } else if (axis == LineSegment.Axis.X) {
                    facing = neighborDirection.isNorthSide() ? CardinalDirection.NORTH : CardinalDirection.SOUTH;
                } else {
                    facing = previousCardinal(neighborDirection);
                }
                replacements.put(position, stairStates.get(stairStateKey(facing.facing(), "straight")));
                plannedFacings.put(position, facing.facing());
            }
        }
    }

    /** Finds the visible marker at a corner, preferring the block above, then level, then below. */
    private BlockVector3 findDiagonalMarker(Region region, BlockType marker, BlockVector3 position,
                                            SegmentDirection direction, Set<BlockVector3> markerPositions) {
        for (int dy : new int[]{1, 0, -1}) {
            BlockVector3 candidate = position.add(direction.dx(), dy, direction.dz());
            if (markerPositions.contains(candidate) && region.contains(candidate)
                    && region.getWorld().getBlock(candidate).getBlockType().equals(marker)) {
                return candidate;
            }
        }
        return null;
    }

    /** Gets a neighbor's traced axis, falling back to its planned stair-facing axis. */
    private LineSegment.Axis findPlannedAxis(
            BlockVector3 position,
            Map<BlockVector3, String> plannedFacings,
            Map<BlockVector3, EnumMap<LineSegment.Axis, String>> plannedAxisFacings) {
        Map<LineSegment.Axis, String> axisFacings = plannedAxisFacings.get(position);
        if (axisFacings != null && !axisFacings.isEmpty()) return axisFacings.keySet().iterator().next();
        String facing = plannedFacings.get(position);
        if (facing == null) return null;
        CardinalDirection direction = CardinalDirection.fromFacing(facing);
        return direction.dx() == 0 ? LineSegment.Axis.Z : LineSegment.Axis.X;
    }

    /** Converts a diagonal corner to the cardinal direction immediately before it. */
    private CardinalDirection previousCardinal(SegmentDirection direction) {
        return switch (direction) {
            case NW -> CardinalDirection.WEST;
            case NE -> CardinalDirection.NORTH;
            case SE -> CardinalDirection.EAST;
            case SW -> CardinalDirection.SOUTH;
        };
    }

    /** Tests whether two diagonal corners are diagonally opposite. */
    private boolean areOpposingCorners(Set<SegmentDirection> directions) {
        return (directions.contains(SegmentDirection.NW) && directions.contains(SegmentDirection.SE))
                || (directions.contains(SegmentDirection.NE) && directions.contains(SegmentDirection.SW));
    }

    /** Returns the shared cardinal side of two adjacent diagonal corners. */
    private CardinalDirection commonCornerDirection(Set<SegmentDirection> directions) {
        boolean west = directions.stream().anyMatch(SegmentDirection::isWestSide);
        boolean east = directions.stream().anyMatch(SegmentDirection::isEastSide);
        boolean north = directions.stream().anyMatch(SegmentDirection::isNorthSide);
        boolean south = directions.stream().anyMatch(SegmentDirection::isSouthSide);
        if (west && east) return north ? CardinalDirection.NORTH : CardinalDirection.SOUTH;
        if (north && south) return west ? CardinalDirection.WEST : CardinalDirection.EAST;
        throw new IllegalArgumentException("Corner directions do not share a cardinal side: " + directions);
    }

    /** Applies the axis-specific facing rule for opposite singleton-neighbor corners. */
    private CardinalDirection resolveOpposingCornerFacing(SegmentDirection axisReference, LineSegment.Axis axis) {
        if (axisReference == SegmentDirection.SW) {
            if (axis == LineSegment.Axis.Z) return CardinalDirection.SOUTH;
            if (axis == LineSegment.Axis.X) return CardinalDirection.NORTH;
            return previousCardinal(axisReference);
        }
        // NW/SE is the NE/SW case rotated one cardinal step clockwise.
        if (axis == LineSegment.Axis.X) return CardinalDirection.WEST;
        if (axis == LineSegment.Axis.Z) return CardinalDirection.EAST;
        return previousCardinal(axisReference);
    }

    /** Chooses an inner stair corner from this stair's facing and the adjacent stair's facing. */
    private String innerShape(String facing, String adjacentFacing) {
        CardinalDirection own = CardinalDirection.fromFacing(facing);
        CardinalDirection neighbor = CardinalDirection.fromFacing(adjacentFacing);
        if (neighbor == own.clockwise()) return "inner_right";
        if (neighbor == own.counterClockwise()) return "inner_left";
        return "straight";
    }

    /** Selects the clockwise-later direction for a two-sided marker corner. */
    private CardinalDirection clockwiseLater(Set<CardinalDirection> directions) {
        if (directions.size() != 2) throw new IllegalArgumentException("Expected two directions");
        CardinalDirection[] pair = directions.toArray(CardinalDirection[]::new);
        if (pair[0].clockwise() == pair[1]) return pair[1];
        if (pair[1].clockwise() == pair[0]) return pair[0];
        // Opposite directions have no unique clockwise-later member; keep the result stable.
        return pair[0].ordinal() < pair[1].ordinal() ? pair[0] : pair[1];
    }

    /** Creates the lookup key used for prebuilt facing/shape stair states. */
    private String stairStateKey(String facing, String shape) {
        return facing + "_" + shape;
    }

    /** Chooses a half's stair-facing from its neighbors, or from the opposite half if empty. */
    private String selectHalfFacing(boolean hasFirstSide, boolean hasSecondSide,
                                    boolean otherHasFirstSide, boolean otherHasSecondSide,
                                    LineSegment.Axis axis) {
        String firstFacing = axis == LineSegment.Axis.Z ? "west" : "north";
        String secondFacing = axis == LineSegment.Axis.Z ? "east" : "south";
        if (hasFirstSide && !hasSecondSide) return firstFacing;
        if (hasSecondSide && !hasFirstSide) return secondFacing;
        if (!hasFirstSide && otherHasFirstSide ^ otherHasSecondSide) {
            return otherHasFirstSide ? oppositeFacing(firstFacing) : oppositeFacing(secondFacing);
        }
        return firstFacing;
    }

    /** Returns the cardinal direction opposite the supplied facing. */
    private String oppositeFacing(String facing) {
        return switch (facing) {
            case "north" -> "south";
            case "south" -> "north";
            case "west" -> "east";
            case "east" -> "west";
            default -> throw new IllegalArgumentException("Unknown facing: " + facing);
        };
    }

    /** Follows a segment in one fixed direction until it ends, then inspects that endpoint. */
    private void traceDirection(SelectionEditSession session, Region region, BlockType marker,
                                LineSegment segment, BlockVector3 from, int stepX, int stepZ,
                                LinkedHashSet<BlockVector3> remainingBlocks,
                                Set<BlockVector3> markerPositions) {
        BlockVector3 current = from;
        while (true) {
            BlockVector3 next = findNeighbor(session, region, current, marker, stepX, stepZ, markerPositions);
            if (next == null) {
                checkEndpointDiagonals(session, region, marker, segment, current, stepX, stepZ, markerPositions);
                return;
            }
            segment.add(next);
            remainingBlocks.remove(next);
            current = next;
        }
    }

    /** Finds a marker one horizontal step away, checking vertical offsets top-to-bottom. */
    private BlockVector3 findNeighbor(SelectionEditSession session, Region region, BlockVector3 from,
                                     BlockType marker, int stepX, int stepZ,
                                     Set<BlockVector3> remainingBlocks) {
        int[] verticalOffsets = {1, 0, -1};
        int[][] horizontalOffsets = stepX == 0 && stepZ == 0
                ? new int[][]{{0, -1}, {1, 0}, {0, 1}, {-1, 0}}
                : new int[][]{{stepX, stepZ}};
        for (int[] horizontal : horizontalOffsets) {
            for (int dy : verticalOffsets) {
                BlockVector3 candidate = from.add(horizontal[0], dy, horizontal[1]);
                if (region.contains(candidate)
                    && remainingBlocks.contains(candidate)
                        && region.getWorld().getBlock(candidate).getBlockType().equals(marker)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /** Records diagonal connections at both ends of a traced run (or all corners of a singleton). */
    private void checkEndpointDiagonals(SelectionEditSession session, Region region, BlockType marker,
                                        LineSegment segment, BlockVector3 endpoint, int stepX, int stepZ,
                                        Set<BlockVector3> markerPositions) {
        if (stepX == 0 && stepZ == 0) {
            checkDiagonal(session, region, marker, segment, endpoint, -1, -1, SegmentDirection.NW, markerPositions);
            checkDiagonal(session, region, marker, segment, endpoint, 1, -1, SegmentDirection.NE, markerPositions);
            checkDiagonal(session, region, marker, segment, endpoint, -1, 1, SegmentDirection.SW, markerPositions);
            checkDiagonal(session, region, marker, segment, endpoint, 1, 1, SegmentDirection.SE, markerPositions);
        } else {
            int[][] lateralOffsets = segment.axis() == LineSegment.Axis.X
                    ? new int[][]{{0, -1}, {0, 1}}
                    : new int[][]{{-1, 0}, {1, 0}};
            for (int[] lateral : lateralOffsets) {
                int dx = stepX + lateral[0];
                int dz = stepZ + lateral[1];
                SegmentDirection direction = dz < 0
                        ? (dx < 0 ? SegmentDirection.NW : SegmentDirection.NE)
                        : (dx < 0 ? SegmentDirection.SW : SegmentDirection.SE);
                checkDiagonal(session, region, marker, segment, endpoint, dx, dz, direction, markerPositions);
            }
        }
    }

    /** Adds a diagonal neighbor and its coordinate when a visible marker is found. */
    private void checkDiagonal(SelectionEditSession session, Region region, BlockType marker,
                              LineSegment segment, BlockVector3 endpoint, int dx, int dz,
                              SegmentDirection direction, Set<BlockVector3> markerPositions) {
        for (int dy : new int[]{1, 0, -1}) {
            BlockVector3 candidate = endpoint.add(dx, dy, dz);
            if (markerPositions.contains(candidate) && region.contains(candidate)
                    && region.getWorld().getBlock(candidate).getBlockType().equals(marker)) {
                segment.addNeighbor(direction, candidate);
                return;
            }
        }
    }

    /** A traced straight run, including its axis and the diagonal segments connected at its ends. */
    private static class LineSegment {
        private enum Axis { X, Z }

        private final List<BlockVector3> blocks = new ArrayList<>();
        private final List<SegmentDirection> neighboringDirections = new ArrayList<>();
        private final EnumMap<SegmentDirection, BlockVector3> neighborPositions = new EnumMap<>(SegmentDirection.class);
        private final EnumMap<SegmentDirection, Integer> neighborLengths = new EnumMap<>(SegmentDirection.class);
        private Axis axis;

        private LineSegment(BlockVector3 start) {
            blocks.add(start);
        }

        private void setAxis(Axis axis) {
            this.axis = axis;
        }

        private void add(BlockVector3 block) {
            blocks.add(block);
        }

        private List<BlockVector3> blocks() {
            return blocks;
        }

        private Axis axis() {
            return axis;
        }

        private void addNeighbor(SegmentDirection direction, BlockVector3 position) {
            if (!neighboringDirections.contains(direction)) neighboringDirections.add(direction);
            neighborPositions.putIfAbsent(direction, position);
        }

        private boolean hasNeighbor(SegmentDirection direction) {
            return neighboringDirections.contains(direction);
        }

        private List<SegmentDirection> neighboringDirections() {
            return neighboringDirections;
        }

        /** Resolves corner markers to neighboring runs so odd-length halves can compare run lengths. */
        private void resolveNeighborLengths(Map<BlockVector3, List<LineSegment>> segmentsByBlock) {
            for (Map.Entry<SegmentDirection, BlockVector3> neighbor : neighborPositions.entrySet()) {
                List<LineSegment> candidates = segmentsByBlock.get(neighbor.getValue());
                if (candidates == null) continue;
                int length = candidates.stream()
                        .filter(candidate -> candidate != this && candidate.axis == axis)
                        .mapToInt(candidate -> candidate.blocks.size())
                        .max()
                        .orElseGet(() -> candidates.stream()
                                .filter(candidate -> candidate != this)
                                .mapToInt(candidate -> candidate.blocks.size())
                                .max().orElse(0));
                if (length > 0) neighborLengths.put(neighbor.getKey(), length);
            }
        }

        /** Returns the shortest connected run in the supplied half, or zero when it has none. */
        private int shortestNeighborLength(Set<SegmentDirection> directions) {
            return directions.stream()
                    .filter(this::hasNeighbor)
                    .mapToInt(direction -> neighborLengths.getOrDefault(direction, 1))
                    .min()
                    .orElse(0);
        }
    }

    /** Diagonal direction from a segment endpoint to a neighboring segment. */
    private enum SegmentDirection {
        NW(-1, -1, 0),
        NE(1, -1, 2),
        SW(-1, 1, 1),
        SE(1, 1, 3);

        private final int dx;
        private final int dz;
        private final int westPriority;

        SegmentDirection(int dx, int dz, int westPriority) {
            this.dx = dx;
            this.dz = dz;
            this.westPriority = westPriority;
        }

        private int dx() { return dx; }
        private int dz() { return dz; }
        private int westPriority() { return westPriority; }
        private boolean isWestSide() { return dx < 0; }
        private boolean isEastSide() { return dx > 0; }
        private boolean isNorthSide() { return dz < 0; }
        private boolean isSouthSide() { return dz > 0; }
    }

    /** Horizontal direction with coordinate offsets and stair-facing conversions. */
    private enum CardinalDirection {
        NORTH(0, -1, "north"),
        WEST(-1, 0, "west"),
        SOUTH(0, 1, "south"),
        EAST(1, 0, "east");

        private final int dx;
        private final int dz;
        private final String facing;
        CardinalDirection(int dx, int dz, String facing) {
            this.dx = dx;
            this.dz = dz;
            this.facing = facing;
        }

        private int dx() { return dx; }
        private int dz() { return dz; }
        private String facing() { return facing; }
        private CardinalDirection clockwise() {
            return switch (this) {
                case NORTH -> EAST;
                case EAST -> SOUTH;
                case SOUTH -> WEST;
                case WEST -> NORTH;
            };
        }

        private CardinalDirection counterClockwise() {
            return switch (this) {
                case NORTH -> WEST;
                case WEST -> SOUTH;
                case SOUTH -> EAST;
                case EAST -> NORTH;
            };
        }

        private static CardinalDirection fromFacing(String facing) {
            for (CardinalDirection direction : values()) {
                if (direction.facing.equals(facing)) return direction;
            }
            throw new IllegalArgumentException("Unknown facing: " + facing);
        }
    }

    /** Supplies marker suggestions for the first argument and supported curb materials for the second. */
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NonNull [] args) {
        if (!sender.hasPermission("bteg.builder")) return Collections.emptyList();
        if (args.length == 1) return TabUtil.getBlockPatternSuggestions(args[0], true);
        if (args.length == 2) return getCurbMaterialSuggestions(args[1]);
        return Collections.emptyList();
    }
}
