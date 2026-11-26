package de.btegermany.utilities.events;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.regions.selector.Polygonal2DRegionSelector;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Handles offhand-swap editing for WorldEdit polygon and cuboid selections. */
public class SelectionEditorListener implements Listener {

    private static final double MAX_DISTANCE = 6.0;
    private static final double MAX_POINT_DISTANCE_SQUARED = 0.65 * 0.65;

    @EventHandler
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));

        final Region region;
        try {
            region = session.getSelection(session.getSelectionWorld());
        } catch (Exception ignored) {
            return;
        }

        if (region instanceof Polygonal2DRegion polyRegion) {
            List<BlockVector2> points = new ArrayList<>(polyRegion.getPoints());
            if (points.isEmpty()) return;

            int selectedIndex = findLookedAtPoint(player, points, polyRegion.getMinimumPoint().y());
            if (selectedIndex < 0) return;

            event.setCancelled(true);
            if (selectedIndex == points.size() - 1) {
                points.remove(selectedIndex);
            } else {
                // Rotate the sequence so the selected vertex is last, preserving the
                // polygon's cyclic order and therefore its shape.
                Collections.rotate(points, -(selectedIndex + 1));
            }

            session.setRegionSelector(polyRegion.getWorld(), new Polygonal2DRegionSelector(
                    polyRegion.getWorld(), points, polyRegion.getMinimumPoint().y(), polyRegion.getMaximumPoint().y()));
            session.dispatchCUISelection(BukkitAdapter.adapt(player));
            return;
        }

        if (!(region instanceof CuboidRegion cuboid)) return;
        Block target = player.getTargetBlockExact((int) MAX_DISTANCE);
        if (target == null) return;

        BlockVector3 targetPosition = BlockVector3.at(target.getX(), target.getY(), target.getZ());
        if (cuboid.contains(targetPosition)) return;

        BlockVector3 pos1;
        BlockVector3 pos2;
        try {
            CuboidRegionSelector currentSelector = (CuboidRegionSelector) session.getRegionSelector(session.getSelectionWorld());
            pos1 = currentSelector.getPrimaryPosition();
            pos2 = cuboid.getPos2();
        } catch (Exception ignored) {
            return;
        }

        BlockVector3[] expanded = expandEndpoints(pos1, pos2, targetPosition);
        event.setCancelled(true);
        session.setRegionSelector(cuboid.getWorld(), new CuboidRegionSelector(cuboid.getWorld(), expanded[0], expanded[1]));
        session.dispatchCUISelection(BukkitAdapter.adapt(player));
    }

    private BlockVector3[] expandEndpoints(BlockVector3 pos1, BlockVector3 pos2, BlockVector3 target) {
        int[] first = {pos1.x(), pos1.y(), pos1.z()};
        int[] second = {pos2.x(), pos2.y(), pos2.z()};
        int[] targetCoordinates = {target.x(), target.y(), target.z()};
        for (int axis = 0; axis < 3; axis++) {
            int minimum = Math.min(first[axis], second[axis]);
            int maximum = Math.max(first[axis], second[axis]);
            if (targetCoordinates[axis] < minimum) {
                if (first[axis] <= second[axis]) first[axis] = targetCoordinates[axis];
                else second[axis] = targetCoordinates[axis];
            } else if (targetCoordinates[axis] > maximum) {
                if (first[axis] >= second[axis]) first[axis] = targetCoordinates[axis];
                else second[axis] = targetCoordinates[axis];
            }
        }
        return new BlockVector3[]{
                BlockVector3.at(first[0], first[1], first[2]),
                BlockVector3.at(second[0], second[1], second[2])
        };
    }

    private int findLookedAtPoint(Player player, List<BlockVector2> points, int y) {
        Location eye = player.getEyeLocation();
        Vector origin = eye.toVector();
        Vector direction = eye.getDirection().normalize();
        double closestDistance = MAX_POINT_DISTANCE_SQUARED;
        int closestIndex = -1;

        for (int i = 0; i < points.size(); i++) {
            BlockVector2 point = points.get(i);
            Vector target = new Vector(point.x() + 0.5, y + 0.5, point.z() + 0.5);
            Vector offset = target.clone().subtract(origin);
            double alongRay = offset.dot(direction);
            if (alongRay < 0 || alongRay > MAX_DISTANCE) continue;

            double distanceSquared = origin.clone().add(direction.clone().multiply(alongRay))
                    .distanceSquared(target);
            if (distanceSquared < closestDistance) {
                closestDistance = distanceSquared;
                closestIndex = i;
            }
        }
        return closestIndex;
    }
}
