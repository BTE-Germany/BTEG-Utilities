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
import de.btegermany.utilities.util.worldedit.WorldEditUtil;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.List;

/** Draws a catenary-shaped arch between the opposite corners of a cuboid selection. */
public class ArchCommand implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             String @NotNull [] args) {
        if (!(sender instanceof Player player) || !command.getName().equalsIgnoreCase("arch")) return true;
        if (!player.hasPermission("bteg.builder")) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cNo permission for //arch");
            return true;
        }
        if (args.length != 3) {
            player.sendMessage(BTEGUtilities.PREFIX + "Usage: //arch <block> <height> <up|down>");
            return true;
        }

        BlockState block;
        int height;
        boolean up;
        try {
            block = Converter.getBlockState(args[0], player);
            height = Integer.parseInt(args[1]);
            if (height < 1) throw new IllegalArgumentException("Height must be positive");
            if (!args[2].equalsIgnoreCase("up") && !args[2].equalsIgnoreCase("down")) {
                throw new IllegalArgumentException("Direction must be up or down");
            }
            up = args[2].equalsIgnoreCase("up");
        } catch (RuntimeException exception) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cInvalid block, height, or direction. Usage: //arch <block> <height> <up|down>");
            return true;
        }

        try {
            WorldEditUtil.findSelection(player, session -> drawArch(session, block, height, up));
        } catch (MaxChangedBlocksException | EmptyClipboardException exception) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cAn error occurred while drawing the arch.");
            exception.printStackTrace();
        }
        return true;
    }

    private void drawArch(SelectionEditSession session, BlockState block, int height, boolean up) {
        Player player = session.player();
        if (!(session.region() instanceof CuboidRegion region)) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cPlease use a cuboid selection to //arch!");
            return;
        }

        BlockVector3 start = region.getPos1();
        BlockVector3 end = region.getPos2();
        double horizontalLength = Math.hypot((double) end.x() - start.x(), (double) end.z() - start.z());
        if (horizontalLength == 0) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cThe cuboid needs at least two different horizontal points.");
            return;
        }

        // Flip up arches vertically so both modes can be fitted as a hanging catenary with a
        // known lowest point. Endpoint height differences naturally shift the low point.
        double startY = up ? -start.y() : start.y();
        double endY = up ? -end.y() : end.y();
        double targetMinimum = Math.min(startY, endY) - height;
        double curvature = findCurvature(horizontalLength, startY, endY, targetMinimum);
        double scale = horizontalLength / (2.0 * curvature);
        double normalizedDelta = (endY - startY) / (2.0 * scale * Math.sinh(curvature));
        double lowPointX = horizontalLength * (0.5 - inverseSinh(normalizedDelta) / (2.0 * curvature));
        double startCosh = Math.cosh(lowPointX / scale);
        // First rasterize the straight horizontal projection. Sample the curve once for each
        // horizontal line block, adding vertical fill only when steepness skips Y levels.
        int steps = Math.max(Math.abs(end.x() - start.x()), Math.abs(end.z() - start.z()));
        steps = Math.max(steps, 1);
        int changed = 0;
        Integer previousY = null;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double distance = t * horizontalLength;
            double catenaryY = startY + scale * (Math.cosh((distance - lowPointX) / scale) - startCosh);
            double targetY = up ? -catenaryY : catenaryY;
            int x = (int) Math.round(start.x() + (end.x() - start.x()) * t);
            int y = (int) Math.round(targetY);
            int z = (int) Math.round(start.z() + (end.z() - start.z()) * t);
            if (previousY != null && Math.abs(y - previousY) > 1) {
                int direction = Integer.signum(y - previousY);
                for (int fillY = previousY + direction; fillY != y; fillY += direction) {
                    changed += placeBlock(session, block, x, fillY, z);
                }
            }
            changed += placeBlock(session, block, x, y, z);
            previousY = y;
        }
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        player.sendMessage(BTEGUtilities.PREFIX + "Placed §6§l" + changed + " §r§7arch block(s).");
    }

    private int placeBlock(SelectionEditSession session, BlockState block, int x, int y, int z) {
        BlockVector3 position = BlockVector3.at(x, y, z);
        if (session.changedBlocks().containsKey(position)) return 0;
        session.editSession().setBlock(x, y, z, block);
        session.changedBlocks().put(position, block);
        return 1;
    }

    /** Fits a cosh catenary whose minimum is the requested distance below both endpoints. */
    private double findCurvature(double span, double startY, double endY, double targetMinimum) {
        double low = 1.0e-4;
        double high = 1.0;
        while (high < 300.0 && catenaryMinimum(span, startY, endY, high) > targetMinimum) {
            high *= 2.0;
        }
        high = Math.min(high, 300.0);
        for (int i = 0; i < 80; i++) {
            double middle = (low + high) / 2.0;
            if (catenaryMinimum(span, startY, endY, middle) > targetMinimum) low = middle;
            else high = middle;
        }
        return (low + high) / 2.0;
    }

    /** Returns the catenary minimum for dimensionless curvature k = span / (2a). */
    private double catenaryMinimum(double span, double startY, double endY, double curvature) {
        double scale = span / (2.0 * curvature);
        double normalizedDelta = (endY - startY) / (2.0 * scale * Math.sinh(curvature));
        double lowPointX = span * (0.5 - inverseSinh(normalizedDelta) / (2.0 * curvature));
        if (lowPointX < 0.0 || lowPointX > span) return Math.min(startY, endY);
        return startY + scale * (1.0 - Math.cosh(lowPointX / scale));
    }

    private double inverseSinh(double value) {
        return Math.copySign(Math.log(Math.abs(value) + Math.sqrt(value * value + 1.0)), value);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String @NonNull [] args) {
        if (!sender.hasPermission("bteg.builder")) return Collections.emptyList();
        if (args.length == 1) return TabUtil.getBlockPatternSuggestions(args[0], true);
        if (args.length == 3) return List.of("up", "down");
        return Collections.emptyList();
    }
}
