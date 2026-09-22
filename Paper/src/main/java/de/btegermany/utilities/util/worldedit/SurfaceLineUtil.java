package de.btegermany.utilities.util.worldedit;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.Set;

/**
 * Shared logic for drawing a straight imaginary line between two points but making it follow the
 * terrain surface: for every column along the line it searches for the nearest position with a
 * solid block that has air directly above it, starting at the imaginary line's height and then
 * alternating upwards and downwards until {@link #MAX_SEARCH_OFFSET} is reached.
 *
 * <p>Used by both {@code //surfaceline} (a single line between two selection points) and
 * {@code //surfaceconnect} (a closed loop of lines between poly selection points).</p>
 */
public final class SurfaceLineUtil {

    public static final int MAX_SEARCH_OFFSET = 50;

    private SurfaceLineUtil() {
    }

    public record Result(int placedBlocks, int failedBlocks) {
        public Result plus(Result other) {
            return new Result(this.placedBlocks + other.placedBlocks, this.failedBlocks + other.failedBlocks);
        }
    }

    /**
     * Draws a surface line between {@code pos1} and {@code pos2}, skipping any column already
     * present in {@code visitedColumns} (and adding newly visited columns to it), so that
     * consecutive calls sharing the same set don't place overlapping blocks twice.
     */
    public static Result drawSurfaceLine(SelectionEditSession session, BlockVector3 pos1, BlockVector3 pos2, BlockState blockState, Set<Long> visitedColumns) {
        int steps = Math.max(Math.abs(pos2.x() - pos1.x()), Math.abs(pos2.z() - pos1.z()));
        if (steps == 0) {
            return new Result(0, 0);
        }

        int worldMinY = session.editSession().getMinimumPoint().y();
        int worldMaxY = session.editSession().getMaximumPoint().y();

        int placedBlocks = 0;
        int failedBlocks = 0;

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            int x = (int) Math.round(pos1.x() + (pos2.x() - pos1.x()) * t);
            int y = (int) Math.round(pos1.y() + (pos2.y() - pos1.y()) * t);
            int z = (int) Math.round(pos1.z() + (pos2.z() - pos1.z()) * t);

            if (!visitedColumns.add(columnKey(x, z))) {
                continue;
            }

            if (placeOnSurface(session, x, y, z, blockState, worldMinY, worldMaxY)) {
                placedBlocks++;
            } else {
                failedBlocks++;
            }
        }

        return new Result(placedBlocks, failedBlocks);
    }

    private static long columnKey(int x, int z) {
        return (((long) x) << 32) ^ (z & 0xFFFFFFFFL);
    }

    /**
     * Searches for the nearest y-level to the given imaginary line position that has a solid block
     * with air directly above it, starting at the given y and then alternating upwards and
     * downwards (y, y+1, y-1, y+2, y-2, ...) up to {@link #MAX_SEARCH_OFFSET} blocks away.
     *
     * @return whether a suitable surface position was found and the block was placed
     */
    private static boolean placeOnSurface(SelectionEditSession session, int x, int y, int z, BlockState blockState, int worldMinY, int worldMaxY) {
        if (trySetSurfaceBlock(session, x, y, z, blockState, worldMinY, worldMaxY)) {
            return true;
        }

        for (int offset = 1; offset <= MAX_SEARCH_OFFSET; offset++) {
            if (trySetSurfaceBlock(session, x, y + offset, z, blockState, worldMinY, worldMaxY)) {
                return true;
            }
            if (trySetSurfaceBlock(session, x, y - offset, z, blockState, worldMinY, worldMaxY)) {
                return true;
            }
        }

        return false;
    }

    private static boolean trySetSurfaceBlock(SelectionEditSession session, int x, int y, int z, BlockState blockState, int worldMinY, int worldMaxY) {
        if (y < worldMinY || y >= worldMaxY) {
            return false;
        }

        BlockVector3 blockPos = BlockVector3.at(x, y, z);
        BlockVector3 abovePos = BlockVector3.at(x, y + 1, z);

        BlockType current = session.editSession().getBlock(blockPos).getBlockType();
        BlockType above = session.editSession().getBlock(abovePos).getBlockType();

        if (current.equals(BlockTypes.AIR) || !above.equals(BlockTypes.AIR)) {
            return false;
        }

        session.editSession().setBlock(blockPos, blockState);
        session.changedBlocks().put(blockPos, blockState);
        return true;
    }
}
