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
import com.sk89q.worldedit.world.registry.LegacyMapper;
import de.leander.bteggamemode.BTEGGamemode;
import de.leander.bteggamemode.util.Converter;
import de.leander.bteggamemode.util.TabUtil;
import de.leander.bteggamemode.util.WorldEditUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.google.common.io.Files.map;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;


public class ConnectCommand implements TabExecutor {


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
                        WorldEditUtil.pasteBackup(player, backup, koordinaten);
                        return true;
                    } else {
                        try {
                            if(args[0].equalsIgnoreCase("plot")){
                                terraform(player, args[0],true);
                            }else{
                                terraform(player, args[0],false);
                            }
                        } catch (MaxChangedBlocksException | EmptyClipboardException e) {
                            e.printStackTrace();
                        }

                        world1 = player.getWorld();
                        return true;
                    }
                }else{
                    player.sendMessage(BTEGGamemode.prefix + "§cWrong usage");
                    player.sendMessage(BTEGGamemode.prefix + "/connect <Block-ID>");
                    return true;
                }
            }else{
                player.sendMessage(BTEGGamemode.prefix + "§cNo permission for //connect");
                return true;
            }
        }
        return true;
    }

    void terraform(Player player, String pattern, boolean plot) throws MaxChangedBlocksException, EmptyClipboardException {
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

        line(polyRegion, player, pattern, plot);

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

    }

    private static void line(Region region, Player player, String pattern, boolean plot) throws MaxChangedBlocksException, EmptyClipboardException {
            world1 = player.getWorld();

            //WorldEdit CLipboard backup
            backup = WorldEditUtil.saveBackup(polyRegion, player);
            koordinaten = BlockVector3.at(region.getMinimumPoint().getBlockX(),region.getMinimumPoint().getY(),region.getMinimumPoint().getZ());

            List<BlockVector2> points = polyRegion.getPoints();
            int y = polyRegion.getMaximumPoint().getBlockY();

            BlockType blockType = null;
            if(plot){
                blockType = BlockTypes.get("lapis_block");
            }else {
                blockType = Converter.getBlockType(pattern);
            }

            BlockState blockState = blockType.getDefaultState();

            Pattern pat = new RandomStatePattern(new FuzzyBlockState(blockState));

            BukkitPlayer actor = BukkitAdapter.adapt(player);
            SessionManager manager = WorldEdit.getInstance().getSessionManager();
            LocalSession localSession = manager.get(actor);

            for(int i = 0; points.size()>i;i++){
                EditSession editSession = localSession.createEditSession(actor);
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
                editSession.close();
            }
            if(plot){
                player.chat("//re !22 82");
                player.sendMessage(BTEGGamemode.prefix + "Successfully prepared plot!");
            }else{
                player.sendMessage(BTEGGamemode.prefix + "Blocks successfully connected!");
            }


    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("bteg.builder")) {
            return emptyList();
        }
        // First argument: target
        if (args.length == 1) {
                return TabUtil.getMaterialBlocks(args[args.length-1]);
        }

        return emptyList();
    }
}
