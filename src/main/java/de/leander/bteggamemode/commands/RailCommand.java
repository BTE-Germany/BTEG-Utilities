package de.leander.bteggamemode.commands;

import com.sk89q.worldedit.*;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;

import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;

import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionOperationException;
import com.sk89q.worldedit.session.ClipboardHolder;

import com.sk89q.worldedit.session.SessionManager;
import com.sk89q.worldedit.world.block.BlockType;
import de.leander.bteggamemode.BTEGGamemode;
import de.leander.bteggamemode.util.Converter;
import de.leander.bteggamemode.util.TabUtil;
import org.bukkit.Bukkit;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Collections.emptyList;


public class RailCommand implements TabExecutor {

    static Clipboard backup;
    static BlockVector3 koordinaten;

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
                        player.sendMessage(BTEGGamemode.prefix + "§cPlease select a WorldEdit selection!");
                        return true;
                    }
                    koordinaten = BlockVector3.at(region.getMinimumPoint().getBlockX(),region.getMinimumPoint().getY(),region.getMinimumPoint().getZ());
                    backup(region,player);
                    ArrayList<de.leander.bteggamemode.util.Block> blocks = null;
                    boolean inGround = false;
                    if(args.length==5) {
                        if(args[4].equalsIgnoreCase("y")) {
                                inGround = true;
                                blocks = new ArrayList<>();
                                for (int i = region.getMinimumPoint().getBlockX(); i <= region.getMaximumPoint().getBlockX(); i++) {
                                    //for (int j = region.getMinimumPoint().getBlockY(); j <= region.getMaximumPoint().getBlockY(); j++) {
                                    for (int k = region.getMinimumPoint().getBlockZ(); k <= region.getMaximumPoint().getBlockZ(); k++) {
                                        if (region.contains(BlockVector3.at(i, world.getHighestBlockYAt(i, k), k))) {
                                            Block block = world.getBlockAt(i, world.getHighestBlockYAt(i, k) - 1, k);
                                            blocks.add(new de.leander.bteggamemode.util.Block(block.getX(), block.getZ(), block.getType()));
                                        }
                                    }
                                    //  }
                                }
                        }
                    }
                    String anvils = "145";
                    if(getDirection(player).equalsIgnoreCase("east")||getDirection(player).equalsIgnoreCase("west")){
                        player.chat("//side "+args[0]+" air n y");
                        anvils = "anvil:1";
                    }else if(getDirection(player).equalsIgnoreCase("north")||getDirection(player).equalsIgnoreCase("south")){
                        player.chat("//side "+args[0]+" air e y");
                        anvils = "anvil";
                    }




                    for (int i = region.getMinimumPoint().getBlockX(); i <= region.getMaximumPoint().getBlockX(); i++) {
                        for (int j = region.getMinimumPoint().getBlockY(); j <= region.getMaximumPoint().getBlockY(); j++) {
                            for (int k = region.getMinimumPoint().getBlockZ(); k <= region.getMaximumPoint().getBlockZ(); k++) {
                                if (region.contains(BlockVector3.at(i, j, k))) {
                                    Block block = world.getBlockAt(i, j, k);
                                    BlockType blockType = Converter.getBlockType(args[0]);
                                    if (block.getType().toString().equalsIgnoreCase(BukkitAdapter.adapt(blockType).toString())) {
                                        switch (getDirection(player)) {
                                            case "NORTH":
                                            case "SOUTH":
                                                if(block.getLocation().getBlockZ()% 2 == 0) {
                                                    world.getBlockAt(i, j, k).setType(Material.PINK_WOOL);
                                                }else{
                                                    world.getBlockAt(i, j, k).setType(Material.CYAN_WOOL);
                                                }
                                                break;
                                            case "EAST":
                                            case "WEST":
                                                if(block.getLocation().getBlockX()% 2 == 0) {
                                                    world.getBlockAt(i, j, k).setType(Material.PINK_WOOL);
                                                }else{
                                                    world.getBlockAt(i, j, k).setType(Material.CYAN_WOOL);
                                                }
                                                break;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Schienen mit verschiedenen Ausrichtungen vom NOrmen HUb nach verschiedenen winkeln pasten


                    overheadLines(args, player, region);
                    if (getDirection(player).equalsIgnoreCase("east") || getDirection(player).equalsIgnoreCase("west")) {
                        player.chat("//side pink_wool red_glazed_terracotta n");
                        player.chat("//side pink_wool red_glazed_terracotta s");

                        player.chat("//side cyan_wool yellow_glazed_terracotta n");
                        player.chat("//side cyan_wool yellow_glazed_terracotta s");

                        if (args.length == 1 || args.length == 2 || ((args.length == 3 || args.length >= 4) && !args[2].equalsIgnoreCase("0"))) {
                            player.chat("//side red_glazed_terracotta light_gray_glazed_terracotta n");
                            player.chat("//side red_glazed_terracotta light_gray_glazed_terracotta s");

                            player.chat("//side yellow_glazed_terracotta gray_glazed_terracotta n");
                            player.chat("//side yellow_glazed_terracotta gray_glazed_terracotta s");
                        }
                    } else if (getDirection(player).equalsIgnoreCase("north") || getDirection(player).equalsIgnoreCase("south")) {
                        player.chat("//side pink_wool red_glazed_terracotta w");
                        player.chat("//side pink_wool red_glazed_terracotta e");

                        player.chat("//side cyan_wool yellow_glazed_terracotta w");
                        player.chat("//side cyan_wool yellow_glazed_terracotta e");

                        if (args.length == 1 || args.length == 2 || ((args.length == 3 || args.length >= 4) && !args[2].equalsIgnoreCase("0"))) {
                            player.chat("//side red_glazed_terracotta light_gray_glazed_terracotta w");
                            player.chat("//side red_glazed_terracotta light_gray_glazed_terracotta e");

                            player.chat("//side yellow_glazed_terracotta gray_glazed_terracotta w");
                            player.chat("//side yellow_glazed_terracotta gray_glazed_terracotta e");
                        }

                    }
                    player.chat("//re 249,239 " + anvils);
                    if (args.length > 1) {
                        if (!inGround) {
                            player.chat("//re 243,35:6 " + args[1]);
                        } else {
                            player.chat("//re 243 " + args[1]);
                        }
                    } else {
                        player.chat("//re 243 44");
                    }

                    if (!inGround) {
                        player.chat("//re 242,35:9 0");
                    } else {
                        player.chat("//re 242 0");
                        player.chat("//re 35:9,35:6 " + args[0]);
                    }
                    if (args.length >= 2) {
                        if (args[1].equalsIgnoreCase("0")) {
                            //player.chat("//re "+args[1]+" 0");
                        }
                    }
                    if (inGround) {
                        for (int i = region.getMinimumPoint().getBlockX(); i <= region.getMaximumPoint().getBlockX(); i++) {
                            for (int k = region.getMinimumPoint().getBlockZ(); k <= region.getMaximumPoint().getBlockZ(); k++) {
                                if (region.contains(BlockVector3.at(i, world.getHighestBlockYAt(i, k) - 1, k))) {
                                    for (de.leander.bteggamemode.util.Block savedBlock : blocks) {
                                        Block surfaceBlock = world.getBlockAt(i, savedBlock.getY(), k);
                                        if (savedBlock.getX() == surfaceBlock.getLocation().getBlockX() && savedBlock.getZ() == surfaceBlock.getLocation().getBlockZ()) {
                                            surfaceBlock.setType(savedBlock.getMat());
                                        }
                                    }
                                }
                            }

                        }
                    }

                }else{
                    player.sendMessage(BTEGGamemode.prefix + "Usage:");
                    player.sendMessage(BTEGGamemode.prefix + "//rail <Block-ID> <Block-ID-railway-sleepers-inside> <Block-ID-railway-sleepers-outside> <generate-overhead-line[y,n]> <rails-in-ground[y,n]>");
                }

            }
        }
        return true;
    }

    private void overheadLines(String[] args, Player player, Region region) {
        if(args.length>=4){
            if(args[3].equalsIgnoreCase("y")) {
                try {
                    region.expand(BlockVector3.at(0,8,0));
                } catch (RegionOperationException e) {
                    throw new RuntimeException(e);
                }

                player.chat("//re >35:6 250");
                player.chat("//re >35:9 250");
                player.chat("//re >250 250");
                player.chat("//re >250 250");
                player.chat("//re >250 250");
                player.chat("//re >250 250");
                player.chat("//re >250 250");
                player.chat("//re >250 101");
                player.chat("//re 250 0");


            }
        }
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
        backup = new BlockArrayClipboard(pRegion);

        ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(
                BukkitAdapter.adapt(player.getWorld()), pRegion, backup, pRegion.getMinimumPoint()
        );

        Operations.complete(forwardExtentCopy);
    }

    private void load(Player player) {
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(player.getWorld()))) {
            Operation operation = new ClipboardHolder(backup)
                    .createPaste(editSession)
                    .to(koordinaten)
                    .ignoreAirBlocks(true)
                    .build();
            Operations.complete(operation);
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("bteg.builder")) {
            return emptyList();
        }
        switch(args.length){
            case 1,2,3:
                return TabUtil.getMaterialBlocks(args[args.length-1]);
            case 4, 5: return Arrays.asList("y", "n");
        }

        return emptyList();
    }
}
