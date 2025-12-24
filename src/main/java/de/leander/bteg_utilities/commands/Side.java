package de.leander.bteg_utilities.commands;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;

import com.sk89q.worldedit.world.block.BlockType;
import de.leander.bteg_utilities.BTEGUtilities;
import de.leander.bteg_utilities.util.CommandWithBackup;
import de.leander.bteg_utilities.util.Converter;
import de.leander.bteg_utilities.util.TabUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
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

public class Side extends CommandWithBackup implements TabExecutor {

    private World world1;
    private Polygonal2DRegion polyRegion;
    private CuboidRegion cuboidRegion;

    private BlockType preBlock;
    private BlockType postBlock;
    private String direction;
    private boolean ignoreSameBlock;
    private ArrayList<String> masks;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player) || !(command.getName().equalsIgnoreCase("side")||command.getName().equalsIgnoreCase("/side"))) {
            return true;
        }
        if (!player.hasPermission("bteg.builder")) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cNo permission for //side");
            return true;
        }
        if (args.length == 0 || args.length == 2) {
            player.sendMessage(BTEGUtilities.PREFIX + "Usage:");
            player.sendMessage(BTEGUtilities.PREFIX + "//side <Block-ID> <Block-ID> <Direction[n,e,s,w]> <ignoreSameBlocks[y,n]> <Mask1> <Mask2> <...>");
            player.sendMessage(BTEGUtilities.PREFIX + "//side <undo>");
            return true;
        }

        if (args.length == 1) {
            if (args[0].equals("undo")) {
                this.pasteBackup();
                return true;
            }
            player.sendMessage(BTEGUtilities.PREFIX + "Usage:");
            player.sendMessage(BTEGUtilities.PREFIX + "//side <Block-ID> <Block-ID> <Direction[n,e,s,w]> <ignoreSameBlocks[y,n]> <Mask1> <Mask2> <...>");
            player.sendMessage(BTEGUtilities.PREFIX + "//side <undo>");
            return true;
        }

        this.preBlock = Converter.getBlockType(args[0].toUpperCase(), player);
        this.postBlock = Converter.getBlockType(args[1].toUpperCase(), player);

        this.direction = args[2];
        if (args.length >= 4) {
            if (args[3].equalsIgnoreCase("y") || args[3].equalsIgnoreCase("yes")) {
                this.ignoreSameBlock = true;
            } else if (args[3].equalsIgnoreCase("n") || args[3].equalsIgnoreCase("no")) {
                this.ignoreSameBlock = false;
            } else {
                player.sendMessage(BTEGUtilities.PREFIX + "§cWrong usage:");
                player.sendMessage(BTEGUtilities.PREFIX + "//side <Block-ID> <Block-ID> <Direction[n,e,s,w]> <ignoreSameBlocks[y,n]> <Mask1> <Mask2> <...>");
                return true;
            }

            if (args.length >= 5) {
                this.masks = new ArrayList<>();
                this.masks.addAll(Arrays.asList(args).subList(4, args.length));
            }
        }
        try {
            this.setSelection(player);
        } catch (MaxChangedBlocksException | EmptyClipboardException e) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cAn error occurred.");
            e.printStackTrace();
        }

        this.world1 = player.getWorld();

        return true;
    }

    private void setSelection(Player player) throws MaxChangedBlocksException, EmptyClipboardException {
        Region plotRegion;
        // Get WorldEdit selection of player
        try {
            LocalSession localSession = WorldEdit.getInstance().getSessionManager().findByName(player.getName());
            if (localSession == null) {
                return;
            }
            plotRegion = localSession.getSelection(localSession.getSelectionWorld());
        } catch (NullPointerException | IncompleteRegionException ex) {
            ex.printStackTrace();
            player.sendMessage(BTEGUtilities.PREFIX + " §cPlease select a WorldEdit selection!");
            return;
        }
        try {
            if (plotRegion instanceof Polygonal2DRegion) {
                // Cast WorldEdit region to polygonal region
                this.polyRegion = (Polygonal2DRegion) plotRegion;
                if (!player.hasPermission("bteg.advanced") && (this.polyRegion.getVolume() > (700 * 700 * 300))) {
                        player.sendMessage("§7§l>> §cPlease adjust your selection size!");
                        return;
                    }

                // Set minimum selection height under player location

            } else if (plotRegion instanceof CuboidRegion) {
                this.cuboidRegion = (CuboidRegion) plotRegion;
            }
        } catch (Exception ex) {
            player.sendMessage(BTEGUtilities.PREFIX + " §cAn error occurred while select area!");
            return;
        }
        if (plotRegion instanceof Polygonal2DRegion) {
            this.replace(this.polyRegion, player);
        } else if (plotRegion instanceof CuboidRegion) {
            this.replace(this.cuboidRegion, player);
        }
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

    }


    private void replace(Region region, @NotNull Player player) throws MaxChangedBlocksException, EmptyClipboardException {

        this.world1 = player.getWorld();

        if (region instanceof Polygonal2DRegion) {
            this.saveBackup(player, this.polyRegion);
        } else if (region instanceof CuboidRegion){
            this.saveBackup(player, this.cuboidRegion);
        }

        int blocks = 0;

        for (int i = region.getMinimumPoint().x(); i <= region.getMaximumPoint().x(); i++) {
            for (int j = region.getMinimumPoint().y(); j <= region.getMaximumPoint().y(); j++) {
                for (int k = region.getMinimumPoint().z(); k <= region.getMaximumPoint().z(); k++) {
                    if (region.contains(BlockVector3.at(i, j, k))) {
                        Block block = this.world1.getBlockAt(i, j, k);
                        if (block.getType().toString().equalsIgnoreCase(BukkitAdapter.adapt(this.preBlock).toString())) {
                            Material materialPreBlock = BukkitAdapter.adapt(this.preBlock);
                            Material materialPostBlock = BukkitAdapter.adapt(this.postBlock);
                            if (this.masks == null) {
                                switch (this.direction) {
                                    case "n" -> {
                                        if (!this.ignoreSameBlock) {
                                            if (!this.world1.getBlockAt(i, j, k - 1).getType().toString().equalsIgnoreCase(materialPreBlock.toString())) {
                                                this.world1.getBlockAt(i, j, k - 1).setType(materialPostBlock);
                                                blocks++;
                                            }
                                        } else {
                                                this.world1.getBlockAt(i, j, k - 1).setType(materialPostBlock);
                                                blocks++;
                                        }
                                    }
                                    case "e" -> {
                                        if (!this.ignoreSameBlock) {
                                            if (!this.world1.getBlockAt(i + 1, j, k).getType().toString().equalsIgnoreCase(materialPreBlock.toString())) {
                                                this.world1.getBlockAt(i + 1, j, k).setType(materialPostBlock);

                                                blocks++;
                                            }
                                        } else {
                                            this.world1.getBlockAt(i + 1, j, k).setType(materialPostBlock);
                                            blocks++;
                                        }
                                    }
                                    case "s" -> {
                                        if(!this.ignoreSameBlock) {
                                            if (!this.world1.getBlockAt(i, j, k + 1).getType().toString().equalsIgnoreCase(materialPreBlock.toString())) {
                                                this.world1.getBlockAt(i, j, k + 1).setType(materialPostBlock);
                                                blocks++;
                                            }
                                        } else {
                                            this.world1.getBlockAt(i, j, k + 1).setType(materialPostBlock);
                                            blocks++;
                                        }
                                    }
                                    case "w" -> {
                                        if(!this.ignoreSameBlock) {
                                            if (!this.world1.getBlockAt(i - 1, j, k).getType().toString().equalsIgnoreCase(materialPreBlock.toString())) {
                                                this.world1.getBlockAt(i - 1, j, k).setType(materialPostBlock);
                                                blocks++;
                                            }
                                        } else {
                                            this.world1.getBlockAt(i - 1, j, k).setType(materialPostBlock);
                                            blocks++;
                                        }
                                    }

                                }
                            } else {
                                for (String mask : this.masks) {

                                    switch (this.direction) {
                                        case "n" -> {

                                            if (!this.ignoreSameBlock) {
                                                if (!this.world1.getBlockAt(i, j, k - 1).getType().toString().equalsIgnoreCase(materialPreBlock.toString()) && this.world1.getBlockAt(i, j, k - 1).getType().toString().equalsIgnoreCase(mask)) {
                                                    this.world1.getBlockAt(i, j, k - 1).setType(materialPostBlock);
                                                    blocks++;
                                                }
                                            } else {
                                                if (this.world1.getBlockAt(i, j, k - 1).getType().toString().equalsIgnoreCase(mask)) {
                                                    this.world1.getBlockAt(i, j, k - 1).setType(materialPostBlock);
                                                    blocks++;
                                                }
                                            }
                                        }
                                        case "e" -> {
                                            if (!this.ignoreSameBlock) {
                                                if (!this.world1.getBlockAt(i + 1, j, k).getType().toString().equalsIgnoreCase(materialPreBlock.toString()) && this.world1.getBlockAt(i + 1, j, k).getType().toString().equalsIgnoreCase(mask)) {
                                                    this.world1.getBlockAt(i + 1, j, k).setType(materialPostBlock);
                                                    blocks++;
                                                }
                                            } else {
                                                if (this.world1.getBlockAt(i + 1, j, k).getType().toString().equalsIgnoreCase(mask)) {
                                                    this.world1.getBlockAt(i + 1, j, k).setType(materialPostBlock);
                                                    blocks++;
                                                }
                                            }
                                        }
                                        case "s" -> {
                                            if (!this.ignoreSameBlock) {
                                                if (!this.world1.getBlockAt(i, j, k + 1).getType().toString().equalsIgnoreCase(materialPreBlock.toString()) && this.world1.getBlockAt(i, j, k + 1).getType().toString().equalsIgnoreCase(mask)) {
                                                    this.world1.getBlockAt(i, j, k + 1).setType(materialPostBlock);
                                                    blocks++;
                                                }
                                            } else {
                                                if (this.world1.getBlockAt(i, j, k + 1).getType().toString().equalsIgnoreCase(mask)) {
                                                    this.world1.getBlockAt(i, j, k + 1).setType(materialPostBlock);
                                                    blocks++;
                                                }
                                            }
                                        }
                                        case "w" -> {
                                            if (!this.ignoreSameBlock) {
                                                if (!this.world1.getBlockAt(i - 1, j, k).getType().toString().equalsIgnoreCase(materialPreBlock.toString()) && this.world1.getBlockAt(i - 1, j, k).getType().toString().equalsIgnoreCase(mask)) {
                                                    this.world1.getBlockAt(i - 1, j, k).setType(materialPostBlock);
                                                    blocks++;
                                                }
                                            } else {
                                                if (this.world1.getBlockAt(i - 1, j, k).getType().toString().equalsIgnoreCase(mask)) {
                                                    this.world1.getBlockAt(i - 1, j, k).setType(materialPostBlock);
                                                    blocks++;
                                                }
                                            }
                                        }

                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        player.sendMessage(BTEGUtilities.PREFIX + "Successfully replaced §6§l" + blocks + " §r§7blocks sideways!");
        this.ignoreSameBlock = false;
        this.masks = null;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("bteg.builder")) {
            return emptyList();
        }
        // First argument: target
        if (args.length == 1 || args.length == 2) {
            return TabUtil.getMaterialBlocks(args[args.length-1], true);
        }

        return emptyList();
    }
}
