package de.leander.bteggamemode.commands;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extension.factory.PatternFactory;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.RandomStatePattern;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.session.SessionManager;
import com.sk89q.worldedit.world.block.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;


public class ConnectCommand implements CommandExecutor {


    static World world1;

    private static Polygonal2DRegion polyRegion;
    static Clipboard clipboard;

    static Clipboard backup;
    static BlockVector3 koordinaten;

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
            koordinaten = BlockVector3.at(region.getMinimumPoint().getBlockX(),region.getMinimumPoint().getY(),region.getMinimumPoint().getZ());

            List<BlockVector2> points = polyRegion.getPoints();
            int y = polyRegion.getMaximumPoint().getBlockY();
            WorldEditPlugin worldEdit = (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");

            BlockType blockType = BlockTypes.get(pattern.toLowerCase());

            BlockState blockState = blockType.getDefaultState();

            Pattern pat = new RandomStatePattern(new FuzzyBlockState(blockState));

            BukkitPlayer actor = BukkitAdapter.adapt(player);
            SessionManager manager = WorldEdit.getInstance().getSessionManager();
            LocalSession localSession = manager.get(actor);
            EditSession editSession = localSession.createEditSession(actor);

            for(int i = 0; points.size()>i;i++){
                if(i == points.size()-1){
                    BlockVector3 vector = BlockVector3.at(points.get(i).getBlockX(),y,points.get(i).getBlockZ());
                    BlockVector3 vector1 = BlockVector3.at(points.get(i+1-points.size()).getBlockX(),y,points.get(i+1-points.size()).getBlockZ());
                    editSession.drawLine(pat,vector, vector1,0,true);
                    localSession.remember(editSession);
                }else{
                    BlockVector3 vector = BlockVector3.at(points.get(i).getBlockX(),y,points.get(i).getBlockZ());
                    BlockVector3 vector1 = BlockVector3.at(points.get(i+1).getBlockX(),y,points.get(i+1).getBlockZ());
                    editSession.drawLine(pat,vector, vector1,0,true);
                    localSession.remember(editSession);
                }
            }

            player.sendMessage("§b§lBTEG §7» §7Blocks successfully connected!");

    }


    private static void backup(Region pRegion,Player player){
        backup = new BlockArrayClipboard(pRegion);
        BukkitPlayer actor = BukkitAdapter.adapt(player);
        SessionManager manager = WorldEdit.getInstance().getSessionManager();
        LocalSession localSession = manager.get(actor);
        ClipboardHolder selection = new ClipboardHolder(backup);
        EditSession editSession = localSession.createEditSession(actor);
        BlockVector3 min = selection.getClipboard().getMinimumPoint();
        BlockVector3 max = selection.getClipboard().getMaximumPoint();
        editSession.enableQueue();
        /*
        clipboard = new Clipboard(max.subtract(min).add(BlockVector3.at(1, 1, 1)), min);
        clipboard.copy(editSession);
         */
        localSession.remember(editSession);
        editSession.flushQueue();
    }

    private void load(Player player) {
        try {
            //
            EditSession editSession = new EditSessionFactory().getEditSession(new BukkitWorld(player.getWorld()), -1);
            editSession.enableQueue();
            clipboard.paste(editSession, koordinaten,false,null);
            editSession.flushQueue();
            player.sendMessage("§b§lBTEG §7» §7Undo succesful!");
        } catch (WorldEditException exception) {
            exception.printStackTrace();
        }
    }
}
