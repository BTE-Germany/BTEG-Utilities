package de.leander.bteggamemode.commands;

import com.sk89q.worldedit.*;


import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionOperationException;

import de.leander.bteggamemode.BTEGGamemode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockVector;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class LidarCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { return true; }
        Player player = (Player) sender;
        if (command.getName().equalsIgnoreCase("lidar")||command.getName().equalsIgnoreCase("/lidar")) {
            if (player.hasPermission("bteg.lidar")) {
                Region region;
                // Get WorldEdit selection of player
                try {
                    region = WorldEdit.getInstance().getSessionManager().findByName(player.getName()).getSelection(WorldEdit.getInstance().getSessionManager().findByName(player.getName()).getSelectionWorld());
                } catch (NullPointerException | IncompleteRegionException ex) {
                    ex.printStackTrace();
                    player.sendMessage(BTEGGamemode.prefix + "§cPlease select a WorldEdit selection!");
                    return true;
                }
                //CompletableFuture.runAsync(() -> {
                main(player, region, args);
               // });
            }else{
                player.sendMessage(BTEGGamemode.prefix + "§cNo permission for /lidar");
            }
        }
        return true;
    }

    private void main(Player player, Region region, String[] args){
        try {
            ArrayList<de.leander.bteggamemode.util.Block> blocks = new ArrayList<>();
            World world = player.getWorld();

            region.expand(BlockVector3.UNIT_Y);
            if(args.length>0) {
                if (args[0].equalsIgnoreCase("save")) {
                    for (int i = region.getMinimumPoint().getBlockX(); i <= region.getMaximumPoint().getBlockX(); i++) {
                        //for (int j = region.getMinimumPoint().getBlockY(); j <= region.getMaximumPoint().getBlockY(); j++) {
                        for (int k = region.getMinimumPoint().getBlockZ(); k <= region.getMaximumPoint().getBlockZ(); k++) {
                            if (region.contains((BlockVector3.at(i, world.getHighestBlockYAt(i, k), k)))) {
                                Block block = world.getBlockAt(i, world.getHighestBlockYAt(i, k) - 1, k);
                                blocks.add(new de.leander.bteggamemode.util.Block(block.getX(), block.getZ(), block.getBlockData().getMaterial()));
                                player.sendMessage(BTEGGamemode.prefix + "" + block.getType() + " saved");
                            }
                        }
                        //  }
                    }
                    player.sendMessage(BTEGGamemode.prefix + "§lSaved surface blocks");
                }
            }

            EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(region.getWorld(), -1);
            LocalSession localSession = WorldEdit.getInstance().getSessionManager().findByName(player.getName());
            editSession.enableQueue();
            region.getWorld().regenerate(region,editSession);
            editSession.flushQueue();
            localSession.remember(editSession);
            player.sendMessage(BTEGGamemode.prefix + "§lRegion regenerated");
            for (int i = region.getMinimumPoint().getBlockX(); i <= region.getMaximumPoint().getBlockX(); i++) {
                for (int j = region.getMinimumPoint().getBlockY(); j <= region.getMaximumPoint().getBlockY(); j++) {
                    for (int k = region.getMinimumPoint().getBlockZ(); k <= region.getMaximumPoint().getBlockZ(); k++) {
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

                            for(Material material : materials){
                                if(block.getBlockData().getMaterial().equals(material)) {
                                    deleteBlock(block);
                                }
                            }


                        }
                    }
                }
            }
            player.sendMessage(BTEGGamemode.prefix + "§lRegion cleaned");
            if(args.length>0) {
                if (args[0].equalsIgnoreCase("save")) {
                    for (int i = region.getMinimumPoint().getBlockX(); i <= region.getMaximumPoint().getBlockX(); i++) {
                        for (int k = region.getMinimumPoint().getBlockZ(); k <= region.getMaximumPoint().getBlockZ(); k++) {
                            if (region.contains((BlockVector3.at(i, world.getHighestBlockYAt(i, k) - 1, k)))) {
                                Block surfaceBlock = world.getBlockAt(i, world.getHighestBlockYAt(i, k) - 1, k);
                                for (de.leander.bteggamemode.util.Block savedBlock : blocks) {
                                    if (savedBlock.getX() == surfaceBlock.getLocation().getBlockX() && savedBlock.getZ() == surfaceBlock.getLocation().getBlockZ()) {
                                        surfaceBlock.setType(savedBlock.getMat());
                                        //player.sendMessage(BTEGGamemode.prefix + "" + savedBlock.getMat() + " pasted");
                                    }
                                }
                            }
                        }

                    }
                    player.sendMessage(BTEGGamemode.prefix + "§lSuccessfully regenerated region and replaced surface blocks");
                }
            }else{
                player.sendMessage(BTEGGamemode.prefix + "§lSuccessfully regenerated and cleaned region");
            }

        } catch (RegionOperationException e) {
            player.sendMessage(BTEGGamemode.prefix + "§c§lAn error occurred.");
            e.printStackTrace();
        }
    }

    public void deleteBlock(Block block){
        block.setType(Material.AIR);
    }




}
