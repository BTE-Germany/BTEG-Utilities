package de.btegermany.utilities.commands;

import static java.util.Collections.emptyList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;

import de.btegermany.utilities.BTEGUtilities;
import de.btegermany.utilities.util.TabUtil;
import de.btegermany.utilities.util.worldedit.Converter;
import de.btegermany.utilities.util.worldedit.SelectionEditSession;
import de.btegermany.utilities.util.worldedit.SurfaceLineUtil;
import de.btegermany.utilities.util.worldedit.WorldEditUtil;

/**
 * Like {@code //connect}, this draws a closed loop of lines between all points of a poly
 * selection, but instead of perfectly straight lines it makes every line follow the terrain
 * surface, just like {@code //surfaceline} does for a single line. See {@link SurfaceLineUtil}
 * for details on the surface-following algorithm.
 */
public class SurfaceConnectCommand implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player) || !command.getName().equalsIgnoreCase("surfaceconnect")) {
            return true;
        }
        if (!player.hasPermission("bteg.builder")) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cNo permission for //surfaceconnect");
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(BTEGUtilities.PREFIX + "Usage: //surfaceconnect <Block-ID>");
            return true;
        }

        BlockType blockType;
        try {
            blockType = Converter.getBlockType(args[0], player);
        } catch (RuntimeException exception) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cInvalid block type: " + args[0]);
            return true;
        }
        BlockState blockState = blockType.getDefaultState();

        try {
            WorldEditUtil.findSelection(player, session -> this.drawSurfaceConnection(session, blockState));
        } catch (MaxChangedBlocksException | EmptyClipboardException exception) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cAn error occurred while connecting the points.");
            exception.printStackTrace();
        }

        return true;
    }

    private void drawSurfaceConnection(SelectionEditSession session, BlockState blockState) {
        Player player = session.player();

        if (!(session.region() instanceof Polygonal2DRegion polyRegion)) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cPlease use a poly selection to //surfaceconnect!");
            return;
        }

        List<BlockVector2> points = polyRegion.getPoints();
        if (points.size() < 2) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cThe poly selection needs at least two points!");
            return;
        }
        int y = polyRegion.getMaximumPoint().y();

        // Shared across all edges so that a column already placed by a previous edge isn't
        // overwritten (and double-counted) by the next one.
        Set<Long> visitedColumns = new HashSet<>();
        int placedBlocks = 0;
        int failedBlocks = 0;

        for (int i = 0; i < points.size(); i++) {
            BlockVector2 point = points.get(i);
            BlockVector2 nextPoint = points.get((i + 1) % points.size());

            BlockVector3 pos1 = BlockVector3.at(point.x(), y, point.z());
            BlockVector3 pos2 = BlockVector3.at(nextPoint.x(), y, nextPoint.z());

            SurfaceLineUtil.Result result = SurfaceLineUtil.drawSurfaceLine(session, pos1, pos2, blockState, visitedColumns);
            placedBlocks += result.placedBlocks();
            failedBlocks += result.failedBlocks();
        }

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        player.sendMessage(BTEGUtilities.PREFIX + "Placed §6§l" + placedBlocks + " §r§7surface connection block(s)!");
        if (failedBlocks > 0) {
            player.sendMessage(BTEGUtilities.PREFIX + "§eWarning: " + failedBlocks + " block(s) could not be placed (no nearby surface found).");
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("bteg.builder")) {
            return emptyList();
        }
        if (args.length == 1) {
            return TabUtil.getMaterialBlocks(args[0], true);
        }
        return emptyList();
    }
}
