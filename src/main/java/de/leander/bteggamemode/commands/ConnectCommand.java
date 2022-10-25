package de.leander.bteggamemode.commands;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import com.sk89q.worldedit.patterns.BlockChance;
import com.sk89q.worldedit.patterns.RandomFillPattern;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.registry.WorldData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class ConnectCommand implements CommandExecutor {


    static World world1;
    private static Plugin plugin;

    private static Polygonal2DRegion polyRegion;
    static CuboidClipboard clipboard;
    static ClipboardHolder clipboardHolder;

    static Clipboard backup;
    static BlockVector koordinaten;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { return true; }
        Player player = (Player) sender;
        if (command.getName().equalsIgnoreCase("connect")||command.getName().equalsIgnoreCase("/connect")) {
            if (player.hasPermission("bteg.builder")) {
                if (args.length == 1) {
                    if (args[0].equals("undo")) {
                        load(player);
                        return true;
                    } else {
                        try {
                            terraform(player, args[0]);
                        } catch (MaxChangedBlocksException | EmptyClipboardException e) {
                            e.printStackTrace();
                        }

                        world1 = player.getWorld();
                        return true;
                    }
                }else{
                    player.sendMessage("§b§lBTEG §7» §cWrong usage");
                    player.sendMessage("§b§lBTEG §7» §7/connect <Block-ID>");
                    return true;
                }
            }else{
                player.sendMessage("§b§lBTEG §7» §cNo permission for //connect");
                return true;
            }
        }
        return true;
    }

    void terraform(Player player, String pattern) throws MaxChangedBlocksException, EmptyClipboardException {
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
                if (polyRegion.getLength() > 500 || polyRegion.getWidth() > 500 || polyRegion.getHeight() > 30) {
                    player.sendMessage("§7§l>> §cPlease adjust your selection size!");
                    return;
                }
                // Set minimum selection height under player location


            } else {
                player.sendMessage("§7§l>> §cPlease use poly selection to connect!");
                return;
            }
        } catch (Exception ex) {
            player.sendMessage("§7§l>> §cAn error occurred while selection area!");
            return;
        }

        line(polyRegion, player, pattern);

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

    }

    private static void line(Region region, Player player, String pattern) throws MaxChangedBlocksException, EmptyClipboardException {
            world1 = player.getWorld();

            //WorldEdit CLipboard backup
            backup(polyRegion, player);
            koordinaten = BlockVector.toBlockPoint(region.getMinimumPoint().getBlockX(),region.getMinimumPoint().getY(),region.getMinimumPoint().getZ());

           /* for(int i = 0;polyRegion.getPoints().size()>i;i=i+2){

                CuboidRegion cuboidRegion = new CuboidRegion(polyRegion.getPoints().get(i).toVector(),polyRegion.getPoints().get(i+1).toVector());

            }*/
            List<BlockVector2D> points = polyRegion.getPoints();
            int y = polyRegion.getMaximumPoint().getBlockY();
            WorldEditPlugin worldEdit = (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
            WorldEdit we = worldEdit.getWorldEdit();


            BaseBlock b;
            if(pattern.contains(":")){
                String[] block = pattern.split(":");
                b = new BaseBlock(Integer.parseInt(block[0]));
                b.setData(Integer.parseInt(block[1]));
            }else{
                int blockID = Integer.parseInt(pattern);
                b = new BaseBlock(blockID);
            }

            List<BlockChance> blocks = new ArrayList<BlockChance>();
            blocks.add(new BlockChance(b, 1));

            RandomFillPattern pat = new RandomFillPattern(blocks); // Create the random pattern


            LocalPlayer localPlayer = worldEdit.wrapPlayer(player);
            LocalSession localSession = we.getSession(localPlayer);

            EditSession editSession = localSession.createEditSession(localPlayer);

            for(int i = 0; points.size()>i;i++){
                if(i == points.size()-1){
                    Vector vector = new BlockVector(points.get(i).getBlockX(),y,points.get(i).getBlockZ());
                    Vector vector1 = new BlockVector(points.get(i+1-points.size()).getBlockX(),y,points.get(i+1-points.size()).getBlockZ());
                    editSession.drawLine(pat,vector, vector1,0,true);
                    localSession.remember(editSession);
                }else{
                    Vector vector = new BlockVector(points.get(i).getBlockX(),y,points.get(i).getBlockZ());
                    Vector vector1 = new BlockVector(points.get(i+1).getBlockX(),y,points.get(i+1).getBlockZ());
                    editSession.drawLine(pat,vector, vector1,0,true);
                    localSession.remember(editSession);
                }
            }

            player.sendMessage("§b§lBTEG §7» §7Blocks successfully connected!");

    }


    private static void backup(Region pRegion, Player player){
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
