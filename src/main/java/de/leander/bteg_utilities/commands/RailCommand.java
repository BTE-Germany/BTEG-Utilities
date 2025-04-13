package de.leander.bteg_utilities.commands;

import com.sk89q.worldedit.*;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionOperationException;

import com.sk89q.worldedit.world.block.BlockType;
import de.leander.bteg_utilities.BTEGUtilities;
import de.leander.bteg_utilities.util.CommandWithBackup;
import de.leander.bteg_utilities.util.Converter;
import de.leander.bteg_utilities.util.TabUtil;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;


public class RailCommand extends CommandWithBackup implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player) || !(command.getName().equalsIgnoreCase("rail") || command.getName().equalsIgnoreCase("/rail"))) {
            return true;
        }
        if (!player.hasPermission("bteg.builder")) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cNo permission for //rail");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(BTEGUtilities.PREFIX + "Usage:");
            player.sendMessage(BTEGUtilities.PREFIX + "//rail <Block-ID> <Wall-ID-railway-sleepers> <generate-overhead-line[y,n]> <rails-in-ground[y,n]>");
            player.sendMessage(BTEGUtilities.PREFIX + "//rail <undo>");
            return true;
        }

        if (args[0].equals("undo")) {
            this.pasteBackup();
            return true;
        }

        BlockType middleBlockType = Converter.getBlockType(args[0], player);
        String railwaySleepersMaterial = args.length >= 2 ? args[1] : "andesite_wall";
        boolean overheadLine = args.length >= 3 && args[2].equalsIgnoreCase("y");
        boolean inGround = args.length >= 4 && args[3].equalsIgnoreCase("y");

        Region region;
        // Get WorldEdit selection of player
        try {
            LocalSession localSession = WorldEdit.getInstance().getSessionManager().findByName(player.getName());
            if(localSession == null) {
                return true;
            }
            region = localSession.getSelection(localSession.getSelectionWorld());

            if (overheadLine) {
                region.expand(BlockVector3.at(0, 8, 0));
            }
        } catch (NullPointerException | IncompleteRegionException | RegionOperationException ex) {
            ex.printStackTrace();
            player.sendMessage(BTEGUtilities.PREFIX + "§cPlease select a WorldEdit selection!");
            return true;
        }
        this.saveBackup(player, region);

        Direction playerDirection = this.getDirection(player);
        String anvils;
        if (playerDirection.isHorizontal()) {
            player.chat("//side " + args[0] + " air n y");
            anvils = "anvil:1";
        } else {
            player.chat("//side " + args[0] + " air e y");
            anvils = "anvil";
        }

        if (overheadLine) {
            this.createOverheadLines(player, middleBlockType);
        }

        String[] placeholderBlocks = new String[] {"light_gray_glazed_terracotta", "red_glazed_terracotta", middleBlockType.toString(), "yellow_glazed_terracotta", "cyan_glazed_terracotta"};
        if (playerDirection.isHorizontal()) {
            player.chat(String.format("//side %s %s n", placeholderBlocks[2], placeholderBlocks[1]));
            player.chat(String.format("//side %s %s s", placeholderBlocks[2], placeholderBlocks[3]));

            if (!inGround) {
                player.chat(String.format("//side %s %s n", placeholderBlocks[1], placeholderBlocks[0]));
                player.chat(String.format("//side %s %s s", placeholderBlocks[3], placeholderBlocks[4]));
            }
        } else {
            player.chat(String.format("//side %s %s w", placeholderBlocks[2], placeholderBlocks[1]));
            player.chat(String.format("//side %s %s e", placeholderBlocks[2], placeholderBlocks[3]));

            if (!inGround) {
                player.chat(String.format("//side %s %s w", placeholderBlocks[1], placeholderBlocks[0]));
                player.chat(String.format("//side %s %s e", placeholderBlocks[3], placeholderBlocks[4]));
            }
        }
        player.chat(String.format("//re %s,%s " + anvils, placeholderBlocks[1], placeholderBlocks[3]));

        if (!inGround) {
            if (playerDirection.isHorizontal()) {
                player.chat(String.format("//re %s %s[north=none,south=low,west=none,east=none,up=false]", placeholderBlocks[0], railwaySleepersMaterial));
                player.chat(String.format("//re %s %s[north=low,south=none,west=none,east=none,up=false]", placeholderBlocks[4], railwaySleepersMaterial));
                player.chat(String.format("//re %s %s[north=low,south=low,west=none,east=none,up=false]", placeholderBlocks[2], railwaySleepersMaterial));
            } else {
                player.chat(String.format("//re %s %s[north=none,south=none,west=none,east=low,up=false]", placeholderBlocks[0], railwaySleepersMaterial));
                player.chat(String.format("//re %s %s[north=none,south=none,west=low,east=low,up=false]", placeholderBlocks[2], railwaySleepersMaterial));
                player.chat(String.format("//re %s %s[north=none,south=none,west=low,east=none,up=false]", placeholderBlocks[4], railwaySleepersMaterial));
            }
        }

        return true;
    }

    private void createOverheadLines(Player player, BlockType middleBlockType) {
        String overheadLineBlock;
        if (this.getDirection(player).isHorizontal()) {
            overheadLineBlock = "black_stained_glass_pane[north=false,south=false,west=true,east=true]";
        } else {
            overheadLineBlock = "black_stained_glass_pane[north=true,south=true,west=false,east=false]";
        }

        player.chat(String.format("//re >%s %s", middleBlockType, overheadLineBlock));
        for (int i = 0; i < 5; i++) {
            player.chat(String.format("//re >%s %s", overheadLineBlock, overheadLineBlock));
        }
        player.chat(String.format("//re >%s lime_glazed_terracotta", middleBlockType));
        for (int i = 0; i < 4; i++) {
            player.chat("//re >lime_glazed_terracotta lime_glazed_terracotta");
        }
        player.chat("//re lime_glazed_terracotta air");
    }

    private Direction getDirection(Player player) {
        float yaw = player.getLocation().getYaw();
        if (yaw < 0) {
            yaw += 360;
        }
        if (yaw >= 315 || yaw < 45) {
            return Direction.SOUTH;
        } else if (yaw < 135) {
            return Direction.WEST;
        } else if (yaw < 225) {
            return Direction.NORTH;
        } else if (yaw < 315) {
            return Direction.EAST;
        }
        return Direction.NORTH;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("bteg.builder")) {
            return emptyList();
        }
        return switch (args.length) {
            case 1 -> {
                List<String> list = new ArrayList<>(TabUtil.getMaterialBlocks(args[0], true));
                if("undo".contains(args[0].toLowerCase())) {
                    list.add("undo");
                }
                yield list;
            }
            case 2 -> TabUtil.getWallBlocks(args[1]);
            case 3, 4 -> Arrays.asList("y", "n");
            default -> emptyList();
        };
    }

    enum Direction {
        NORTH (false),
        SOUTH (false),
        EAST (true),
        WEST (true);

        private final boolean horizontal;

        Direction(boolean horizontal) {
            this.horizontal = horizontal;
        }

        public boolean isHorizontal() {
            return horizontal;
        }
    }
}
