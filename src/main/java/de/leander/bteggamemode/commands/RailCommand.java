package de.leander.bteggamemode.commands;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.patterns.BlockChance;
import com.sk89q.worldedit.patterns.RandomFillPattern;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.registry.WorldData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;



public class RailCommand implements CommandExecutor {

    static Clipboard backup;
    static CuboidClipboard clipboard;
    static BlockVector koordinaten;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { return true; }
        Player player = (Player) sender;
        World world = player.getWorld();
        if (command.getName().equalsIgnoreCase("rail")||command.getName().equalsIgnoreCase("/rail")) {
            if (player.hasPermission("bteg.builder")) {
                if (args.length > 0) {
                    if (args[0].equals("undo")) {
                        load(player);
                        return true;
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
                    koordinaten = BlockVector.toBlockPoint(region.getMinimumPoint().getBlockX(),region.getMinimumPoint().getY(),region.getMinimumPoint().getZ());
                    backup(region,player);
                    ArrayList<de.leander.bteggamemode.util.Block> blocks = null;
                    boolean inGround = false;
                    if(args.length==4) {
                        if(args[3].equalsIgnoreCase("y")) {
                                inGround = true;
                                blocks = new ArrayList<>();
                                for (int i = region.getMinimumPoint().getBlockX(); i <= region.getMaximumPoint().getBlockX(); i++) {
                                    //for (int j = region.getMinimumPoint().getBlockY(); j <= region.getMaximumPoint().getBlockY(); j++) {
                                    for (int k = region.getMinimumPoint().getBlockZ(); k <= region.getMaximumPoint().getBlockZ(); k++) {
                                        if (region.contains(new Vector(i, world.getHighestBlockYAt(i, k), k))) {
                                            Block block = world.getBlockAt(i, world.getHighestBlockYAt(i, k) - 1, k);
                                            blocks.add(new de.leander.bteggamemode.util.Block(block.getX(), block.getZ(), block.getType(), block.getData()));
                                        }
                                    }
                                    //  }
                                }
                        }
                    }
                    String anvils = "145";
                    if(getDirection(player).equalsIgnoreCase("east")||getDirection(player).equalsIgnoreCase("west")){
                        player.chat("//side "+args[0]+" 0 n y");
                        anvils = "145:1";
                    }else if(getDirection(player).equalsIgnoreCase("north")||getDirection(player).equalsIgnoreCase("south")){
                        player.chat("//side "+args[0]+" 0 e y");
                        anvils = "145";
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

                        if(args.length==1 || args.length == 2|| ((args.length==3 || args.length==4) && !args[2].equalsIgnoreCase("0"))){
                            player.chat("//side 249 243 n");
                            player.chat("//side 249 243 s");

                            player.chat("//side 239 242 n");
                            player.chat("//side 239 242 s");
                        }
                    }else if(getDirection(player).equalsIgnoreCase("north")||getDirection(player).equalsIgnoreCase("south")){
                        player.chat("//side 35:6 249 w");
                        player.chat("//side 35:6 249 e");

                        player.chat("//side 35:9 239 w");
                        player.chat("//side 35:9 239 e");

                        if(args.length==1 || args.length == 2 || ((args.length==3 || args.length==4) && !args[2].equalsIgnoreCase("0"))){
                            player.chat("//side 249 243 w");
                            player.chat("//side 249 243 e");

                            player.chat("//side 239 242 w");
                            player.chat("//side 239 242 e");
                        }

                    }
                    player.chat("//re 249,239 "+anvils);
                    if(args.length>1){
                        if(!inGround) {
                            player.chat("//re 243,35:6 " + args[1]);
                        }
                        else{
                            player.chat("//re 243 " + args[1]);
                        }
                    }else{
                        player.chat("//re 243 44");
                    }

                    if(!inGround) {
                        player.chat("//re 242,35:9 0");
                    }else{
                        player.chat("//re 242 0");
                        player.chat("//re 35:9,35:6 "+args[0]);
                    }
                    if(args.length>=2) {
                        if (args[1].equalsIgnoreCase("0")) {
                            //player.chat("//re "+args[1]+" 0");
                        }
                    }
                    if(inGround) {
                        for (int i = region.getMinimumPoint().getBlockX(); i <= region.getMaximumPoint().getBlockX(); i++) {
                            for (int k = region.getMinimumPoint().getBlockZ(); k <= region.getMaximumPoint().getBlockZ(); k++) {
                                if (region.contains(new Vector(i, world.getHighestBlockYAt(i, k) - 1, k))) {
                                    for (de.leander.bteggamemode.util.Block savedBlock : blocks) {
                                        Block surfaceBlock = world.getBlockAt(i, savedBlock.getY(), k);
                                        if (savedBlock.getX() == surfaceBlock.getLocation().getBlockX() && savedBlock.getZ() == surfaceBlock.getLocation().getBlockZ() && surfaceBlock.getTypeId() == 0) {
                                            surfaceBlock.setType(savedBlock.getMat());
                                            surfaceBlock.setData(savedBlock.getData());
                                        }
                                    }
                                }
                            }

                        }
                    }
                }else{
                    player.sendMessage("§b§lBTEG §7» §7Usage:");
                    player.sendMessage("§b§lBTEG §7» §7//rail <Block-ID> <Block-ID-railway-sleepers-inside> <Block-ID-railway-sleepers-outside> <rails-in-ground[y,n]>");
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

    private static void backup(Region pRegion,Player player){
        WorldEditPlugin worldEdit = (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
        WorldEdit we = worldEdit.getWorldEdit();

        WorldData data = pRegion.getWorld().getWorldData();


        backup = new BlockArrayClipboard(pRegion);

        LocalPlayer localPlayer = worldEdit.wrapPlayer(player);
        LocalSession localSession = we.getSession(localPlayer);
        ClipboardHolder selection = new ClipboardHolder(backup, data); //localSession.getClipboard();
        EditSession editSession = localSession.createEditSession(localPlayer);

        Vector min = selection.getClipboard().getMinimumPoint();
        Vector max = selection.getClipboard().getMaximumPoint();

        editSession.enableQueue();
        clipboard = new CuboidClipboard(max.subtract(min).add(new Vector(1, 1, 1)), min);
        clipboard.copy(editSession);

        localSession.remember(editSession);
        editSession.flushQueue();
    }

    private void load(Player player) {
        try {
            //
            EditSession editSession = new EditSession(new BukkitWorld(player.getWorld()), -1);
            editSession.enableQueue();

            clipboard.paste(editSession, koordinaten,false,true);
            editSession.flushQueue();


            player.sendMessage("§b§lBTEG §7» §7Undo succesful!");

        } catch (WorldEditException exception) {
            exception.printStackTrace();
        }

    }

}
