package de.leander.bteggamemode.commands;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.registry.WorldData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class Side implements CommandExecutor {

    static World world1;
    private static Plugin plugin;
    private static Polygonal2DRegion polyRegion;
    static CuboidClipboard clipboard;
    static ClipboardHolder clipboardHolder;
    static String[] preBlock;
    static String[] postBlock;
    static String direction;
    static byte blockData;

    static Clipboard backup;
    static BlockVector koordinaten;

    public Side(JavaPlugin pPlugin) {
        plugin = pPlugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { return true; }
        Player player = (Player) sender;
        if (command.getName().equalsIgnoreCase("/side")) {
            if (player.hasPermission("bteg.side")) {
                if(args.length == 0) {
                    player.sendMessage("§b§lBTEG §7» §7Usage:");
                    player.sendMessage("§b§lBTEG §7» §7//side <Block-ID> <Block-ID> <Direction[n,e,s,w]>");
                    player.sendMessage("§b§lBTEG §7» §7//side undo>");
                    return true;
                }
                else if(args.length == 1){
                    if (args[0].equals("undo")) {
                        load(player);
                        return true;
                    }else{
                        player.sendMessage("§b§lBTEG §7» §7Usage:");
                        player.sendMessage("§b§lBTEG §7» §7//side <Block-ID> <Block-ID> <Direction[n,e,s,w]>");
                        player.sendMessage("§b§lBTEG §7» §7//side undo>");
                        return true;
                    }
                }
                else if(args.length == 3){


                    if(args[0].contains(":")){
                        preBlock = args[0].split(":");
                    }else{//falls der nutzer kein doppelpunkt angibt
                        preBlock = new String[2];
                        preBlock[0] = args[0];
                        preBlock[1] = "0";
                    }
                    if(args[1].contains(":")) {
                        postBlock = args[1].split(":");
                    }else{//falls der nutzer kein doppelpunkt angibt
                        postBlock = new String[2];
                        postBlock[0] = args[1];
                        postBlock[1] = "0";
                    }



                    direction = args[2];
                    try {
                        setSelection(player);
                    } catch (MaxChangedBlocksException | EmptyClipboardException e) {
                        e.printStackTrace();
                    }

                    world1 = player.getWorld();
                    return true;
                }
                else{
                    player.sendMessage("§b§lBTEG §7» §7Usage:");
                    player.sendMessage("§b§lBTEG §7» §7//side <Block-ID> <Block-ID> <Direction[n,e,s,w]>");
                    player.sendMessage("§b§lBTEG §7» §7//side undo>");
                    return true;
                }

            }
        } return true;
    }

    void setSelection(Player player) throws MaxChangedBlocksException, EmptyClipboardException {
        Region plotRegion;
        // Get WorldEdit selection of player
        try {
            plotRegion = WorldEdit.getInstance().getSessionManager().findByName(player.getName()).getSelection(WorldEdit.getInstance().getSessionManager().findByName(player.getName()).getSelectionWorld());
        } catch (NullPointerException | IncompleteRegionException ex) {
            ex.printStackTrace();
            player.sendMessage("§7§l>> §cPlease select a WorldEdit selection!");
            return;
        }
        try {
            // Check if WorldEdit selection is polygonal
            if (plotRegion instanceof Polygonal2DRegion) {
                // Cast WorldEdit region to polygonal region
                polyRegion = (Polygonal2DRegion) plotRegion;
                if (polyRegion.getLength() > 200 || polyRegion.getWidth() > 200 || polyRegion.getHeight() > 100) {
                    player.sendMessage("§7§l>> §cPlease adjust your selection size!");
                    return;
                }
                // Set minimum selection height under player location

            } else {
                player.sendMessage("§7§l>> §cPlease use poly selection to use //side!");
                return;
            }
        } catch (Exception ex) {
            player.sendMessage("§7§l>> §cAn error occurred while selection area!");
            return;
        }

        replace(polyRegion, player);

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

    }


    private static void replace(Region region, Player player) throws MaxChangedBlocksException, EmptyClipboardException {

            world1 = player.getWorld();

            backup(polyRegion, player);
            koordinaten = BlockVector.toBlockPoint(region.getMinimumPoint().getBlockX(),region.getMinimumPoint().getY(),region.getMinimumPoint().getZ());

            for (int i = region.getMinimumPoint().getBlockX(); i <= region.getMaximumPoint().getBlockX(); i++) {
                for (int j = region.getMinimumPoint().getBlockY(); j <= region.getMaximumPoint().getBlockY(); j++) {
                    for (int k = region.getMinimumPoint().getBlockZ(); k <= region.getMaximumPoint().getBlockZ(); k++) {
                        if (region.contains(new Vector(i, j, k))) {
                            Block block = world1.getBlockAt(i, j, k);
                            if (block.getTypeId() == Integer.parseInt(preBlock[0]) && block.getData() == (byte) Integer.parseInt(preBlock[1])) {
                                switch (direction){
                                    case "n":{
                                        if(world1.getBlockAt(i, j, k-1).getTypeId() != Integer.parseInt(preBlock[0])){
                                            world1.getBlockAt(i, j, k-1).setTypeId(Integer.parseInt(postBlock[0]));
                                            world1.getBlockAt(i, j, k-1).setData((byte) Integer.parseInt(postBlock[1]));
                                        }
                                        break;
                                    }
                                    case "e":{
                                        if(world1.getBlockAt(i+1, j, k).getTypeId() != Integer.parseInt(preBlock[0])) {
                                            world1.getBlockAt(i + 1, j, k).setTypeId(Integer.parseInt(postBlock[0]));
                                            world1.getBlockAt(i + 1, j, k).setData((byte) Integer.parseInt(postBlock[1]));
                                        }
                                        break;
                                    }
                                    case "s": {
                                        if (world1.getBlockAt(i, j, k + 1).getTypeId() != Integer.parseInt(preBlock[0])) {
                                            world1.getBlockAt(i, j, k + 1).setTypeId(Integer.parseInt(postBlock[0]));
                                            world1.getBlockAt(i, j, k + 1).setData((byte) Integer.parseInt(postBlock[1]));
                                    }
                                        break;
                                    }
                                    case "w": {
                                        if (world1.getBlockAt(i -1, j, k).getTypeId() != Integer.parseInt(preBlock[0])) {
                                            world1.getBlockAt(i - 1, j, k).setTypeId(Integer.parseInt(postBlock[0]));
                                            world1.getBlockAt(i - 1, j, k).setData((byte) Integer.parseInt(postBlock[1]));
                                    }
                                        break;
                                    }

                                }

                            }




                        }
                    }

            }

        }
        player.sendMessage("§b§lBTEG §7» §7Successfully replaced blocks sideways!");
    }

    private static void backup(Region pRegion,Player player){
        WorldEditPlugin worldEdit = (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
        WorldEdit we = worldEdit.getWorldEdit();

        WorldData data = polyRegion.getWorld().getWorldData();
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
