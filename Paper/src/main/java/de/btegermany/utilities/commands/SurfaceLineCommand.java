package de.btegermany.utilities.commands;

import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BlockState;
import de.btegermany.utilities.BTEGUtilities;
import de.btegermany.utilities.util.TabUtil;
import de.btegermany.utilities.util.worldedit.Converter;
import de.btegermany.utilities.util.worldedit.SelectionEditSession;
import de.btegermany.utilities.util.worldedit.SurfaceLineUtil;
import de.btegermany.utilities.util.worldedit.WorldEditUtil;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Draws a line between the two selection points, just like WorldEdit's //line, but instead of a
 * perfectly straight line it makes the line follow the terrain surface. See
 * {@link SurfaceLineUtil} for details on the surface-following algorithm.
 */
public class SurfaceLineCommand implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player) || !command.getName().equalsIgnoreCase("surfaceline")) {
            return true;
        }
        if (!player.hasPermission("bteg.builder")) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cNo permission for //surfaceline");
            return true;
        }
        if (args.length < 1 || args.length > 2) {
            player.sendMessage(BTEGUtilities.PREFIX + "Usage: //surfaceline <Block-ID> [ground|top]");
            return true;
        }

        BlockState blockState;
        try {
            blockState = Converter.getBlockState(args[0], player);
        } catch (RuntimeException exception) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cInvalid block type: " + args[0]);
            return true;
        }

        var isTop = args.length == 2 && args[1].equalsIgnoreCase("top");

        try {
            WorldEditUtil.findSelection(player, session -> this.drawSurfaceLine(session, blockState, isTop));
        } catch (MaxChangedBlocksException | EmptyClipboardException exception) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cAn error occurred while drawing the line.");
            exception.printStackTrace();
        }

        return true;
    }

    private void drawSurfaceLine(SelectionEditSession session, BlockState blockState, boolean isTop) {
        Player player = session.player();

        if (!(session.region() instanceof CuboidRegion cuboidRegion)) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cPlease use a cuboid selection (two points) for //surfaceline!");
            return;
        }

        BlockVector3 pos1 = cuboidRegion.getPos1();
        BlockVector3 pos2 = cuboidRegion.getPos2();

        if (Math.max(Math.abs(pos2.x() - pos1.x()), Math.abs(pos2.z() - pos1.z())) == 0) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cThe two selection points need to differ on the X or Z axis!");
            return;
        }

        // Every step corresponds to one column (x/z pair) along the imaginary straight line.
        // Since multiple steps can round to the same column, we only need to process each once.
        Set<Long> visitedColumns = new HashSet<>();
        SurfaceLineUtil.Result result = SurfaceLineUtil.drawSurfaceLine(session, pos1, pos2, blockState, visitedColumns, isTop);

        player.sendMessage(BTEGUtilities.PREFIX + "Placed §6§l" + result.placedBlocks() + " §r§7surface line block(s)!");
        if (result.failedBlocks() > 0) {
            player.sendMessage(BTEGUtilities.PREFIX + "§eWarning: " + result.failedBlocks() + " block(s) could not be placed (no nearby surface found).");
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("bteg.builder")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return TabUtil.getBlockPatternSuggestions(args[0], true);
        }
        if (args.length == 2) {
            return Arrays.asList("top", "ground");
        }
        return Collections.emptyList();
    }
}
