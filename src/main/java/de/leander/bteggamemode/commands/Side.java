package de.leander.bteggamemode.commands;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;

import com.sk89q.worldedit.session.SessionManager;
import com.sk89q.worldedit.session.SessionOwner;
import com.sk89q.worldedit.world.block.BlockType;
import de.leander.bteggamemode.BTEGGamemode;
import de.leander.bteggamemode.util.Converter;
import de.leander.bteggamemode.util.TabUtil;
import de.leander.bteggamemode.util.WorldEditUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

public class Side implements TabExecutor {

    static World world1;
    private static Plugin plugin;
    private static Polygonal2DRegion polyRegion;
    private static CuboidRegion cuboidRegion;

    Clipboard clipboard;

    static ClipboardHolder clipboardHolder;
    static BlockType preBlock;
    static BlockType postBlock;
    static String direction;
    static boolean ignoreSameBlock;
    static ArrayList<String> masks;
    static byte blockData;

    static Clipboard backup;
    static BlockVector3 koordinaten;

    public Side(JavaPlugin pPlugin) {
        plugin = pPlugin;
    }

    public Side() {

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { return true; }
        Player player = (Player) sender;
        if (command.getName().equalsIgnoreCase("side")||command.getName().equalsIgnoreCase("/side")) {
            if (player.hasPermission("bteg.builder")) {
                if(args.length == 0) {
                    player.sendMessage(BTEGGamemode.prefix + "Usage:");
                    player.sendMessage(BTEGGamemode.prefix + "//side <Block-ID> <Block-ID> <Direction[n,e,s,w]> <ignoreSameBlocks[y,n]> <Mask1> <Mask2> <...>");
                    player.sendMessage(BTEGGamemode.prefix + "//side <undo>");
                    return true;
                }
                else if(args.length == 1){
                    if (args[0].equals("undo")) {
                        WorldEditUtil.pasteBackup(player, backup, koordinaten);
                        return true;
                    }else{
                        player.sendMessage(BTEGGamemode.prefix + "Usage:");
                        player.sendMessage(BTEGGamemode.prefix + "//side <Block-ID> <Block-ID> <Direction[n,e,s,w]> <ignoreSameBlocks[y,n]> <Mask1> <Mask2> <...>");
                        player.sendMessage(BTEGGamemode.prefix + "//side <undo>");
                        return true;
                    }
                }
                else if(args.length >= 3){

                    preBlock = Converter.getBlockType(args[0].toUpperCase());

                    postBlock = Converter.getBlockType(args[1].toUpperCase());

                    direction = args[2];
                    if(args.length>=4) {
                            if (args[3].equalsIgnoreCase("y") || args[3].equalsIgnoreCase("yes")) {
                                ignoreSameBlock = true;
                            } else if (args[3].equalsIgnoreCase("n") || args[3].equalsIgnoreCase("no")) {
                                ignoreSameBlock = false;
                            }else{
                                player.sendMessage(BTEGGamemode.prefix + "§cWrong usage:");
                                player.sendMessage(BTEGGamemode.prefix + "//side <Block-ID> <Block-ID> <Direction[n,e,s,w]> <ignoreSameBlocks[y,n]> <Mask1> <Mask2> <...>");
                                return true;
                            }

                        if (args.length >= 5) {
                            masks = new ArrayList<>();
                            for (int i = 4; i < args.length; i++) {
                                masks.add(args[i]);
                            }
                        }
                    }
                    try {
                        setSelection(player);
                    } catch (MaxChangedBlocksException | EmptyClipboardException e) {
                        e.printStackTrace();
                    }

                    world1 = player.getWorld();
                    return true;
                }
                else{
                    player.sendMessage(BTEGGamemode.prefix + "Usage:");
                    player.sendMessage(BTEGGamemode.prefix + "//side <Block-ID> <Block-ID> <Direction[n,e,s,w]> <ignoreSameBlocks[y,n]> <Mask1> <Mask2> <...>");
                    player.sendMessage(BTEGGamemode.prefix + "//side <undo>");
                    return true;
                }

            }
        }else{
            player.sendMessage(BTEGGamemode.prefix + "§cNo permission for //side");
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
            if (plotRegion instanceof Polygonal2DRegion) {
                // Cast WorldEdit region to polygonal region
                polyRegion = (Polygonal2DRegion) plotRegion;
                if(!player.hasPermission("bteg.advanced")) {
                    if (polyRegion.getLength() > 500 || polyRegion.getWidth() > 500 || polyRegion.getHeight() > 200) {
                        player.sendMessage("§7§l>> §cPlease adjust your selection size!");
                        return;
                    }
                }
                // Set minimum selection height under player location

            } else if(plotRegion instanceof CuboidRegion) {
                cuboidRegion = (CuboidRegion) plotRegion;
            }
        } catch (Exception ex) {
            player.sendMessage("§7§l>> §cAn error occurred while select area!");
            return;
        }
        if (plotRegion instanceof Polygonal2DRegion) {
            replace(polyRegion, player);
        }else if(plotRegion instanceof CuboidRegion){
            replace(cuboidRegion, player);
        }
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

    }


    private static void replace(Region region, Player player) throws MaxChangedBlocksException, EmptyClipboardException {

            world1 = player.getWorld();

        if (region instanceof Polygonal2DRegion) {
            WorldEditUtil.saveBackup(polyRegion, player);
        }else if(region instanceof CuboidRegion){
            WorldEditUtil.saveBackup(cuboidRegion, player);
        }

            koordinaten = BlockVector3.at(region.getMinimumPoint().getBlockX(),region.getMinimumPoint().getY(),region.getMinimumPoint().getZ());
            int blocks = 0;

            for (int i = region.getMinimumPoint().getBlockX(); i <= region.getMaximumPoint().getBlockX(); i++) {
                for (int j = region.getMinimumPoint().getBlockY(); j <= region.getMaximumPoint().getBlockY(); j++) {
                    for (int k = region.getMinimumPoint().getBlockZ(); k <= region.getMaximumPoint().getBlockZ(); k++) {
                        if (region.contains(BlockVector3.at(i, j, k))) {
                            Block block = world1.getBlockAt(i, j, k);
                            if (block.getType().toString().equalsIgnoreCase(BukkitAdapter.adapt(preBlock).toString())) {
                                Material materialPreBlock = BukkitAdapter.adapt(preBlock);
                                Material materialPostBlock = BukkitAdapter.adapt(postBlock);
                                if(masks==null) {
                                    switch (direction) {
                                        case "n": {
                                            if(!ignoreSameBlock) {
                                                if (!world1.getBlockAt(i, j, k - 1).getType().toString().equalsIgnoreCase(materialPreBlock.toString())) {
                                                    world1.getBlockAt(i, j, k - 1).setType(materialPostBlock);
                                                    blocks++;
                                                }
                                            }else{
                                                    world1.getBlockAt(i, j, k - 1).setType(materialPostBlock);
                                                    blocks++;
                                            }
                                            break;
                                        }
                                        case "e": {
                                            if(!ignoreSameBlock) {
                                                if (!world1.getBlockAt(i + 1, j, k).getType().toString().equalsIgnoreCase(materialPreBlock.toString())) {
                                                    world1.getBlockAt(i + 1, j, k).setType(materialPostBlock);

                                                    blocks++;
                                                }
                                            }else{
                                                world1.getBlockAt(i + 1, j, k).setType(materialPostBlock);
                                                blocks++;
                                            }
                                            break;
                                        }
                                        case "s": {
                                            if(!ignoreSameBlock) {
                                                if (!world1.getBlockAt(i, j, k + 1).getType().toString().equalsIgnoreCase(materialPreBlock.toString())) {
                                                    world1.getBlockAt(i, j, k + 1).setType(materialPostBlock);
                                                    blocks++;
                                                }
                                            }else{
                                                world1.getBlockAt(i, j, k + 1).setType(materialPostBlock);
                                                blocks++;
                                            }
                                            break;
                                        }
                                        case "w": {
                                            if(!ignoreSameBlock) {
                                                if (!world1.getBlockAt(i - 1, j, k).getType().toString().equalsIgnoreCase(materialPreBlock.toString())) {
                                                    world1.getBlockAt(i - 1, j, k).setType(materialPostBlock);
                                                    blocks++;
                                                }
                                            }else{
                                                world1.getBlockAt(i - 1, j, k).setType(materialPostBlock);
                                                blocks++;
                                            }
                                            break;
                                        }

                                    }
                                }else{
                                    for(String maske : masks) {

                                        switch (direction) {
                                            case "n": {

                                                if (!ignoreSameBlock) {
                                                    if (!world1.getBlockAt(i, j, k - 1).getType().toString().equalsIgnoreCase(materialPreBlock.toString()) && world1.getBlockAt(i, j, k - 1).getType().toString().equalsIgnoreCase(maske)) {
                                                        world1.getBlockAt(i, j, k - 1).setType(materialPostBlock);
                                                        blocks++;
                                                    }
                                                } else {
                                                    if (world1.getBlockAt(i, j, k - 1).getType().toString().equalsIgnoreCase(maske)) {
                                                        world1.getBlockAt(i, j, k - 1).setType(materialPostBlock);
                                                        blocks++;
                                                    }
                                                }
                                                break;
                                            }
                                            case "e": {
                                                if (!ignoreSameBlock) {
                                                    if (!world1.getBlockAt(i + 1, j, k).getType().toString().equalsIgnoreCase(materialPreBlock.toString()) && world1.getBlockAt(i + 1, j, k).getType().toString().equalsIgnoreCase(maske)) {
                                                        world1.getBlockAt(i + 1, j, k).setType(materialPostBlock);
                                                        blocks++;
                                                    }
                                                } else {
                                                    if (world1.getBlockAt(i + 1, j, k).getType().toString().equalsIgnoreCase(maske)) {
                                                        world1.getBlockAt(i + 1, j, k).setType(materialPostBlock);
                                                        blocks++;
                                                    }
                                                }
                                                break;
                                            }
                                            case "s": {
                                                if (!ignoreSameBlock) {
                                                    if (!world1.getBlockAt(i, j, k + 1).getType().toString().equalsIgnoreCase(materialPreBlock.toString()) && world1.getBlockAt(i, j, k + 1).getType().toString().equalsIgnoreCase(maske)) {
                                                        world1.getBlockAt(i, j, k + 1).setType(materialPostBlock);
                                                        blocks++;
                                                    }
                                                } else {
                                                    if (world1.getBlockAt(i, j, k + 1).getType().toString().equalsIgnoreCase(maske)) {
                                                        world1.getBlockAt(i, j, k + 1).setType(materialPostBlock);
                                                        blocks++;
                                                    }
                                                }
                                                break;
                                            }
                                            case "w": {
                                                if (!ignoreSameBlock) {
                                                    if (!world1.getBlockAt(i - 1, j, k).getType().toString().equalsIgnoreCase(materialPreBlock.toString()) && world1.getBlockAt(i - 1, j, k).getType().toString().equalsIgnoreCase(maske)) {
                                                        world1.getBlockAt(i - 1, j, k).setType(materialPostBlock);
                                                        blocks++;
                                                    }
                                                } else {
                                                    if (world1.getBlockAt(i - 1, j, k).getType().toString().equalsIgnoreCase(maske)) {
                                                        world1.getBlockAt(i - 1, j, k).setType(materialPostBlock);
                                                        blocks++;
                                                    }
                                                }
                                                break;
                                            }

                                        }
                                    }
                                }

                            }




                        }
                    }

            }

        }
        player.sendMessage(BTEGGamemode.prefix + "Successfully replaced §6§l"+blocks+" §r§7blocks sideways!");
        ignoreSameBlock = false;
        masks = null;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("bteg.builder")) {
            return emptyList();
        }
        // First argument: target
        if (args.length == 1 || args.length == 2) {
            return TabUtil.getMaterialBlocks(args[args.length-1]);
        }

        return emptyList();
    }
}
