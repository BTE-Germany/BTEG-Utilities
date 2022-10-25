package de.leander.bteggamemode.commands;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionOperationException;
import com.sk89q.worldedit.regions.factory.CuboidRegionFactory;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
                    player.sendMessage("§b§lBTEG §7» §cPlease select a WorldEdit selection!");
                    return true;
                }
                //CompletableFuture.runAsync(() -> {
                main(player, region, args);
               // });
            }else{
                player.sendMessage("§b§lBTEG §7» §cNo permission for /lidar");
            }
        }
        return true;
    }

    private void main(Player player, Region region, String[] args){
        try {
            ArrayList<de.leander.bteggamemode.util.Block> blocks = new ArrayList<>();
            World world = player.getWorld();
            region.expand(new Vector(0,1,0));
            if(args.length>0) {
                if (args[0].equalsIgnoreCase("save")) {
                    for (int i = region.getMinimumPoint().getBlockX(); i <= region.getMaximumPoint().getBlockX(); i++) {
                        //for (int j = region.getMinimumPoint().getBlockY(); j <= region.getMaximumPoint().getBlockY(); j++) {
                        for (int k = region.getMinimumPoint().getBlockZ(); k <= region.getMaximumPoint().getBlockZ(); k++) {
                            if (region.contains(new Vector(i, world.getHighestBlockYAt(i, k), k))) {
                                Block block = world.getBlockAt(i, world.getHighestBlockYAt(i, k) - 1, k);
                                blocks.add(new de.leander.bteggamemode.util.Block(block.getX(), block.getZ(), block.getType(), block.getData()));
                                player.sendMessage("§b§lBTEG §7» §7" + block.getType() + " saved");
                            }
                        }
                        //  }
                    }
                    player.sendMessage("§b§lBTEG §7» §7§lSaved surface blocks");
                }
            }

            EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(region.getWorld(), -1);
            LocalSession localSession = WorldEdit.getInstance().getSessionManager().findByName(player.getName());
            editSession.enableQueue();
            region.getWorld().regenerate(region,editSession);
            editSession.flushQueue();
            localSession.remember(editSession);
            player.sendMessage("§b§lBTEG §7» §7§lRegion regenerated");
            for (int i = region.getMinimumPoint().getBlockX(); i <= region.getMaximumPoint().getBlockX(); i++) {
                for (int j = region.getMinimumPoint().getBlockY(); j <= region.getMaximumPoint().getBlockY(); j++) {
                    for (int k = region.getMinimumPoint().getBlockZ(); k <= region.getMaximumPoint().getBlockZ(); k++) {
                        if (region.contains(new Vector(i, j, k))) {
                            Block block = world.getBlockAt(i, j, k);
                                   /* switch (block.getType()){
                                        case LEAVES:
                                            deleteBlock(block);
                                            break;
                                        case WOOD:
                                            deleteBlock(block);
                                            break;
                                        case YELLOW_FLOWER:
                                            deleteBlock(block);
                                            break;
                                        case BROWN_MUSHROOM:
                                            deleteBlock(block);
                                            break;
                                        case RED_MUSHROOM:
                                            deleteBlock(block);
                                            break;
                                        case VINE:
                                            deleteBlock(block);
                                            break;
                                        case DOUBLE_PLANT:
                                            deleteBlock(block);
                                            break;
                                        case LONG_GRASS:
                                            deleteBlock(block);
                                            break;
                                    }*/
                            int[] deleteBlocks = {17,18,39,106,31,175,37,38,40,86};
                            for(int b : deleteBlocks){
                                if(block.getTypeId()==b){
                                    deleteBlock(block);
                                }
                            }

                        }
                    }
                }
            }
            player.sendMessage("§b§lBTEG §7» §7§lRegion cleaned");
            if(args.length>0) {
                if (args[0].equalsIgnoreCase("save")) {
                    for (int i = region.getMinimumPoint().getBlockX(); i <= region.getMaximumPoint().getBlockX(); i++) {
                        for (int k = region.getMinimumPoint().getBlockZ(); k <= region.getMaximumPoint().getBlockZ(); k++) {
                            if (region.contains(new Vector(i, world.getHighestBlockYAt(i, k) - 1, k))) {
                                Block surfaceBlock = world.getBlockAt(i, world.getHighestBlockYAt(i, k) - 1, k);
                                for (de.leander.bteggamemode.util.Block savedBlock : blocks) {
                                    if (savedBlock.getX() == surfaceBlock.getLocation().getBlockX() && savedBlock.getZ() == surfaceBlock.getLocation().getBlockZ()) {
                                        surfaceBlock.setType(savedBlock.getMat());
                                        surfaceBlock.setData(savedBlock.getData());
                                        player.sendMessage("§b§lBTEG §7» §7" + savedBlock.getMat() + " pasted");
                                    }
                                }
                            }
                        }

                    }
                    player.sendMessage("§b§lBTEG §7» §7§lSuccessfully regenerated region and replaced surface blocks");
                }
            }else{
                player.sendMessage("§b§lBTEG §7» §7§lSuccessfully regenerated and cleaned region");
            }

        } catch (RegionOperationException e) {
            player.sendMessage("§b§lBTEG §7» §c§lAn error occurred.");
            e.printStackTrace();
        }
    }

    public void deleteBlock(Block block){
        block.setType(Material.AIR);
    }




}
