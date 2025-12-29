package de.btegermany.utilities.commands;

import com.sk89q.worldedit.*;


import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionOperationException;

import de.btegermany.utilities.BTEGUtilities;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LidarCommand implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player) || !(command.getName().equalsIgnoreCase("lidar") || command.getName().equalsIgnoreCase("/lidar"))) {
            return true;
        }
        if (!player.hasPermission("bteg.lidar")) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cNo permission for /lidar");
            return true;
        }

        Region region;
        // Get WorldEdit selection of player
        try {
            LocalSession localSession = WorldEdit.getInstance().getSessionManager().findByName(player.getName());
            if(localSession == null) {
                return true;
            }
            region = localSession.getSelection(localSession.getSelectionWorld());
        } catch (NullPointerException | IncompleteRegionException ex) {
            ex.printStackTrace();
            player.sendMessage(BTEGUtilities.PREFIX + "§cPlease select a WorldEdit selection!");
            return true;
        }
        this.lidar(player, region, args);

        return true;
    }

    private void lidar(@NotNull Player player, @NotNull Region region, String @NotNull [] args) {
        try {
            List<de.btegermany.utilities.util.Block> blocks = new ArrayList<>();
            World world = player.getWorld();

            region.expand(BlockVector3.UNIT_Y);
            if (args.length > 0 && args[0].equalsIgnoreCase("save")) {
                    for (int i = region.getMinimumPoint().x(); i <= region.getMaximumPoint().x(); i++) {
                        for (int k = region.getMinimumPoint().z(); k <= region.getMaximumPoint().z(); k++) {
                            if (region.contains((BlockVector3.at(i, world.getHighestBlockYAt(i, k), k)))) {
                                Block block = world.getBlockAt(i, world.getHighestBlockYAt(i, k) - 1, k);
                                blocks.add(new de.btegermany.utilities.util.Block(block.getX(), block.getZ(), block.getBlockData().getMaterial()));
                                player.sendMessage(BTEGUtilities.PREFIX + block.getType() + " saved");
                            }
                        }
                    }
                    player.sendMessage(BTEGUtilities.PREFIX + "§lSaved surface blocks");
                }


            EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder().world(region.getWorld()).maxBlocks(-1).build();
            LocalSession localSession = WorldEdit.getInstance().getSessionManager().findByName(player.getName());
            if (localSession == null || region.getWorld() == null) {
                return;
            }
            editSession.enableQueue();
            region.getWorld().regenerate(region,editSession);
            editSession.flushQueue();
            localSession.remember(editSession);
            player.sendMessage(BTEGUtilities.PREFIX + "§lRegion regenerated");
            for (int i = region.getMinimumPoint().x(); i <= region.getMaximumPoint().x(); i++) {
                for (int j = region.getMinimumPoint().y(); j <= region.getMaximumPoint().y(); j++) {
                    for (int k = region.getMinimumPoint().z(); k <= region.getMaximumPoint().z(); k++) {
                        if (region.contains((BlockVector3.at(i, j, k)))) {
                            Block block = world.getBlockAt(i, j, k);
                            ArrayList<Material> materials = new ArrayList<>();
                            materials.add(Material.OAK_LEAVES);
                            materials.add(Material.ACACIA_LEAVES);
                            materials.add(Material.BIRCH_LEAVES);
                            materials.add(Material.DARK_OAK_LEAVES);
                            materials.add(Material.SPRUCE_LEAVES);
                            materials.add(Material.JUNGLE_LEAVES);
                            materials.add(Material.AZALEA_LEAVES);
                            materials.add(Material.MANGROVE_LEAVES);
                            materials.add(Material.OAK_WOOD);
                            materials.add(Material.ACACIA_WOOD);
                            materials.add(Material.BIRCH_WOOD);
                            materials.add(Material.DARK_OAK_WOOD);
                            materials.add(Material.SPRUCE_WOOD);
                            materials.add(Material.JUNGLE_WOOD);
                            materials.add(Material.MANGROVE_WOOD);
                            materials.add(Material.DANDELION);
                            materials.add(Material.BROWN_MUSHROOM);
                            materials.add(Material.RED_MUSHROOM);
                            materials.add(Material.VINE);
                            materials.add(Material.LARGE_FERN);
                            materials.add(Material.TALL_GRASS);
                            materials.add(Material.LILY_PAD);
                            materials.add(Material.SUGAR_CANE);
                            materials.add(Material.ROSE_BUSH);

                            for (Material material : materials){
                                if (block.getBlockData().getMaterial().equals(material)) {
                                    this.deleteBlock(block);
                                }
                            }

                        }
                    }
                }
            }
            player.sendMessage(BTEGUtilities.PREFIX + "§lRegion cleaned");
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("save")) {
                    for (int i = region.getMinimumPoint().x(); i <= region.getMaximumPoint().x(); i++) {
                        for (int k = region.getMinimumPoint().z(); k <= region.getMaximumPoint().z(); k++) {
                            if (region.contains((BlockVector3.at(i, world.getHighestBlockYAt(i, k) - 1, k)))) {
                                Block surfaceBlock = world.getBlockAt(i, world.getHighestBlockYAt(i, k) - 1, k);
                                for (de.btegermany.utilities.util.Block savedBlock : blocks) {
                                    if (savedBlock.getX() == surfaceBlock.getLocation().getBlockX() && savedBlock.getZ() == surfaceBlock.getLocation().getBlockZ()) {
                                        surfaceBlock.setType(savedBlock.getMat());
                                    }
                                }
                            }
                        }
                    }
                    player.sendMessage(BTEGUtilities.PREFIX + "§lSuccessfully regenerated region and replaced surface blocks");
                }
            } else {
                player.sendMessage(BTEGUtilities.PREFIX + "§lSuccessfully regenerated and cleaned region");
            }

        } catch (RegionOperationException e) {
            player.sendMessage(BTEGUtilities.PREFIX + "§c§lAn error occurred.");
            e.printStackTrace();
        }
    }

    public void deleteBlock(@NotNull Block block){
        block.setType(Material.AIR);
    }


    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return null;
    }
}
