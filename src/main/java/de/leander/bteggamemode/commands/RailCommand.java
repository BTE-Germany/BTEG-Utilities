package de.leander.bteggamemode.commands;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RailCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { return true; }
        Player player = (Player) sender;
        World world = player.getWorld();
        if (command.getName().equalsIgnoreCase("rail")||command.getName().equalsIgnoreCase("/rail")) {
            if (player.hasPermission("bteg.builder")) {
                if (args.length > 0) {
                    String anvils = "145";
                    if(getDirection(player).equalsIgnoreCase("east")||getDirection(player).equalsIgnoreCase("west")){
                        player.chat("//side "+args[0]+" 0 n y");
                        anvils = "145:1";
                    }else if(getDirection(player).equalsIgnoreCase("north")||getDirection(player).equalsIgnoreCase("south")){
                        player.chat("//side "+args[0]+" 0 e y");
                        anvils = "145";
                    }

                    Region region;
                    // Get WorldEdit selection of player
                    try {
                        region = WorldEdit.getInstance().getSessionManager().findByName(player.getName()).getSelection(WorldEdit.getInstance().getSessionManager().findByName(player.getName()).getSelectionWorld());
                    } catch (NullPointerException | IncompleteRegionException ex) {
                        ex.printStackTrace();
                        player.sendMessage("§b§lBTEG §7» §cPlease select a WorldEdit selection!");
                        return true;
                    }


                    for (int i = region.getMinimumPoint().getBlockX(); i <= region.getMaximumPoint().getBlockX(); i++) {
                        for (int j = region.getMinimumPoint().getBlockY(); j <= region.getMaximumPoint().getBlockY(); j++) {
                            for (int k = region.getMinimumPoint().getBlockZ(); k <= region.getMaximumPoint().getBlockZ(); k++) {
                                if (region.contains(new Vector(i, j, k))) {
                                    Block block = world.getBlockAt(i, j, k);
                                    if (block.getTypeId() == Integer.parseInt(args[0])) {
                                        switch (getDirection(player)) {
                                            case "NORTH":
                                            case "SOUTH":
                                                if(block.getLocation().getBlockZ()% 2 == 0) {
                                                    world.getBlockAt(i, j, k).setTypeId(35);
                                                    world.getBlockAt(i, j, k).setData((byte) 6);
                                                }else{
                                                    world.getBlockAt(i, j, k).setTypeId(35);
                                                    world.getBlockAt(i, j, k).setData((byte) 9);
                                                }
                                                break;
                                            case "EAST":
                                            case "WEST":
                                                if(block.getLocation().getBlockX()% 2 == 0) {
                                                    world.getBlockAt(i, j, k).setTypeId(35);
                                                    world.getBlockAt(i, j, k).setData((byte) 6);
                                                }else{
                                                    world.getBlockAt(i, j, k).setTypeId(35);
                                                    world.getBlockAt(i, j, k).setData((byte) 9);
                                                }
                                                break;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Schienen mit verschiedenen Ausrichtungen vom NOrmen HUb nach verschiedenen winkeln pasten

                    //player.chat("//re "+args[0]+" #clipboard");
                    if(getDirection(player).equalsIgnoreCase("east")||getDirection(player).equalsIgnoreCase("west")){
                        player.chat("//side 35:6 249 n");
                        player.chat("//side 35:6 249 s");

                        player.chat("//side 35:9 239 n");
                        player.chat("//side 35:9 239 s");

                        player.chat("//side 249 243 n");
                        player.chat("//side 249 243 s");

                        player.chat("//side 239 242 n");
                        player.chat("//side 239 242 s");
                    }else if(getDirection(player).equalsIgnoreCase("north")||getDirection(player).equalsIgnoreCase("south")){
                        player.chat("//side 35:6 249 w");
                        player.chat("//side 35:6 249 e");

                        player.chat("//side 35:9 239 w");
                        player.chat("//side 35:9 239 e");

                        player.chat("//side 249 243 w");
                        player.chat("//side 249 243 e");

                        player.chat("//side 239 242 w");
                        player.chat("//side 239 242 e");
                    }

                    player.chat("//re 249,239 "+anvils);
                    player.chat("//re 243 44");
                    player.chat("//re 242 0");
                    if(args.length==2) {
                        if (!args[1].equalsIgnoreCase("n") || !args[1].equalsIgnoreCase("no")) {
                            player.chat("//re 44 0");
                        }
                    }
                }else{
                    player.sendMessage("§b§lBTEG §7» §7Usage:");
                    player.sendMessage("§b§lBTEG §7» §7//rail <Block-ID> <railway-sleepers[y,n]>");
                }

            }
        }
        return true;
    }

    public static String getDirection(Player player) {
        float yaw = player.getLocation().getYaw();
        if (yaw < 0) {
            yaw += 360;
        }
        if (yaw >= 315 || yaw < 45) {
            return "SOUTH";
        } else if (yaw < 135) {
            return "WEST";
        } else if (yaw < 225) {
            return "NORTH";
        } else if (yaw < 315) {
            return "EAST";
        }
        return "NORTH";
    }
}
