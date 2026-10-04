package de.btegermany.utilities.commands;

import static java.util.Collections.emptyList;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockTypes;

import de.btegermany.utilities.BTEGUtilities;
import de.btegermany.utilities.util.Direction;
import de.btegermany.utilities.util.worldedit.SelectionEditSession;
import de.btegermany.utilities.util.worldedit.WorldEditUtil;

/**
 * Skews (shears) a cuboid selection along a horizontal axis: one side of the selection stays in
 * place while the opposite side is pushed by a given amount, and every "slice" in between is
 * offset by an amount that's linearly interpolated (and rounded) between those two extremes -
 * exactly the same rounding WorldEdit's {@code //line} algorithm produces. This means the
 * selection isn't split into equal parts; instead the result follows a perfectly straight
 * imaginary line between the fixed side and the moving side.
 *
 * <p>Typical use case: build a facade on a straight wall first, then use {@code //skew} to bend
 * it to follow a diagonal outline.</p>
 *
 * <p>Usage: {@code //skew <Amount> <Direction[n,e,s,w]> <Side[n,e,s,w]>}, where {@code Direction}
 * is the direction the moving side gets pushed towards, and {@code Side} is which side of the
 * selection is the one that moves (it must be perpendicular to {@code Direction}). For example,
 * {@code //skew 10 n w} pushes the west side of the selection 10 blocks north while the east side
 * stays in place.</p>
 */
public class SkewCommand implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player) || !command.getName().equalsIgnoreCase("skew")) {
            return true;
        }
        if (!player.hasPermission("bteg.builder")) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cNo permission for //skew");
            return true;
        }
        if (args.length != 3) {
            player.sendMessage(BTEGUtilities.PREFIX + "Usage: //skew <Amount> <Direction[n,e,s,w]> <Side[n,e,s,w]>");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[0]);
        } catch (NumberFormatException exception) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cInvalid amount: " + args[0]);
            return true;
        }
        if (amount <= 0) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cThe amount needs to be a positive number!");
            return true;
        }

        Direction direction;
        Direction side;
        try {
            direction = Direction.fromInput(args[1].toLowerCase(Locale.ROOT));
            side = Direction.fromInput(args[2].toLowerCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cInvalid direction or side. Use n, e, s or w.");
            return true;
        }

        if (!isHorizontalCardinal(direction) || !isHorizontalCardinal(side)) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cDirection and side both need to be n, e, s or w.");
            return true;
        }
        if (direction.isHorizontal() == side.isHorizontal()) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cThe side needs to be perpendicular to the direction, e.g. //skew " + amount + " n w.");
            return true;
        }

        int finalAmount = amount;
        try {
            WorldEditUtil.findSelection(player, session -> this.skew(session, finalAmount, direction, side));
        } catch (MaxChangedBlocksException | EmptyClipboardException exception) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cAn error occurred while skewing the selection.");
            exception.printStackTrace();
        }

        return true;
    }

    private boolean isHorizontalCardinal(Direction direction) {
        return direction == Direction.NORTH || direction == Direction.SOUTH || direction == Direction.EAST || direction == Direction.WEST;
    }

    private void skew(SelectionEditSession session, int amount, Direction direction, Direction side) {
        Player player = session.player();

        if (!(session.region() instanceof CuboidRegion cuboidRegion)) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cPlease use a cuboid selection (two points) for //skew!");
            return;
        }

        BlockVector3 min = cuboidRegion.getMinimumPoint();
        BlockVector3 max = cuboidRegion.getMaximumPoint();

        // If the direction moves along the X axis (e/w), slices are grouped by their Z coordinate
        // and vice versa - the side axis is always perpendicular to the direction axis.
        boolean dispIsX = (direction == Direction.EAST || direction == Direction.WEST);
        int dispSign = switch (direction) {
            case EAST, SOUTH -> 1;
            case WEST, NORTH -> -1;
            default -> throw new IllegalStateException("Direction must be a horizontal cardinal direction: " + direction);
        };

        int zeroCoord;
        int fullCoord;
        if (dispIsX) {
            zeroCoord = (side == Direction.NORTH) ? max.z() : min.z();
            fullCoord = (side == Direction.NORTH) ? min.z() : max.z();
        } else {
            zeroCoord = (side == Direction.WEST) ? max.x() : min.x();
            fullCoord = (side == Direction.WEST) ? min.x() : max.x();
        }

        if (fullCoord == zeroCoord) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cThe selection needs to be wider than one block along the side's axis!");
            return;
        }

        // Read the whole selection into memory first, since slices will be moved on top of each
        // other's original positions. getFullBlock() (rather than getBlock()) is used to also
        // capture tile entity data (e.g. sign text, container contents) so it isn't lost.
        Map<BlockVector3, BaseBlock> originalBlocks = new HashMap<>();
        for (int x = min.x(); x <= max.x(); x++) {
            for (int y = min.y(); y <= max.y(); y++) {
                for (int z = min.z(); z <= max.z(); z++) {
                    BlockVector3 position = BlockVector3.at(x, y, z);
                    originalBlocks.put(position, session.editSession().getFullBlock(position));
                }
            }
        }

        // Clear the source selection first so slices that move further don't leave the old blocks
        // behind underneath the ones that moved less (or didn't move at all).
        BaseBlock air = BlockTypes.AIR.getDefaultState().toBaseBlock();
        for (BlockVector3 position : originalBlocks.keySet()) {
            session.editSession().setBlock(position, air);
            session.changedBlocks().put(position, air.toImmutableState());
        }

        int movedBlocks = 0;
        for (Map.Entry<BlockVector3, BaseBlock> entry : originalBlocks.entrySet()) {
            BlockVector3 position = entry.getKey();
            BaseBlock blockState = entry.getValue();
            if (blockState.getBlockType().equals(BlockTypes.AIR)) {
                continue;
            }

            int sideCoord = dispIsX ? position.z() : position.x();
            // Same rounding WorldEdit's //line uses: a linear interpolation between 0 (at the
            // fixed side) and the full amount (at the moving side), rounded per slice.
            int offset = (int) Math.round(amount * (double) (sideCoord - zeroCoord) / (fullCoord - zeroCoord)) * dispSign;

            BlockVector3 target = dispIsX
                    ? BlockVector3.at(position.x() + offset, position.y(), position.z())
                    : BlockVector3.at(position.x(), position.y(), position.z() + offset);

            session.editSession().setBlock(target, blockState);
            session.changedBlocks().put(target, blockState.toImmutableState());
            movedBlocks++;
        }

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        player.sendMessage(BTEGUtilities.PREFIX + "Skewed §6§l" + movedBlocks + " §r§7block(s)!");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("bteg.builder")) {
            return emptyList();
        }
        if (args.length == 2 && args[1].isEmpty()) {
            return List.of("n", "e", "s", "w");
        }
        if (args.length == 3 && args[2].isEmpty()) {
            Direction direction;
            try {
                direction = Direction.fromInput(args[1].toLowerCase(Locale.ROOT));
            } catch (RuntimeException exception) {
                return emptyList();
            }
            if (!isHorizontalCardinal(direction)) {
                return emptyList();
            }
            return direction.isHorizontal() ? List.of("n", "s") : List.of("e", "w");
        }
        return emptyList();
    }
}
