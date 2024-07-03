package de.leander.bteggamemode.commands;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import de.leander.bteggamemode.BTEGGamemode;
import de.leander.bteggamemode.util.CommandWithBackup;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;


public class Terraform extends CommandWithBackup implements CommandExecutor {

    private final int[] preHeight = new int[2];
    private int height;
    private World world1;
    private Polygonal2DRegion polyRegion;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player) || !command.getName().equalsIgnoreCase("terraform")) {
            return true;
        }
        if(!player.hasPermission("bteg.builder")) {
            player.sendMessage(BTEGGamemode.PREFIX + "§cNo permission for /terraform");
            return true;
        }

        if (args[0].equals("undo")) {
            this.pasteBackup();
            return true;
        }
        this.height = (Integer.parseInt(args[0]) - 1);
        try {
            this.terraform(player);
        } catch (MaxChangedBlocksException | EmptyClipboardException e) {
            player.sendMessage(BTEGGamemode.PREFIX + "§cAn error occurred.");
            e.printStackTrace();
        }

        this.world1 = player.getWorld();

        return true;
    }

    private void terraform(Player player) throws MaxChangedBlocksException, EmptyClipboardException {
        Region plotRegion;
        // Get WorldEdit selection of player
        try {
            LocalSession localSession = WorldEdit.getInstance().getSessionManager().findByName(player.getName());
            if(localSession == null) {
                return;
            }
            plotRegion = localSession.getSelection(localSession.getSelectionWorld());
        } catch (NullPointerException | IncompleteRegionException ex) {
            ex.printStackTrace();
            player.sendMessage(BTEGGamemode.PREFIX + "§cPlease select a WorldEdit selection!");
            return;
        }
        try {
            // Check if WorldEdit selection is polygonal
            if (plotRegion instanceof Polygonal2DRegion) {
                // Cast WorldEdit region to polygonal region
                this.polyRegion = (Polygonal2DRegion) plotRegion;
                if (this.polyRegion.getLength() > 300 || this.polyRegion.getWidth() > 300 || this.polyRegion.getHeight() > 60) {
                    player.sendMessage(BTEGGamemode.PREFIX + "§cPlease adjust your selection size!");
                    return;
                }
                // Set minimum selection height under player location
                this.preHeight[0] = this.polyRegion.getMinimumY();
                this.preHeight[1] = this.polyRegion.getMaximumY();

                this.polyRegion.setMinimumY(this.height);
                this.polyRegion.setMaximumY(this.height + 35);

            } else {
                player.sendMessage(BTEGGamemode.PREFIX + "§cPlease use poly selection to terraform!");
                return;
            }

        } catch (Exception ex) {
            player.sendMessage(BTEGGamemode.PREFIX + "§cAn error occurred while select this area!");
            ex.printStackTrace();
            return;
        }

        this.replaceEmerald(this.polyRegion, player);

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

    }

    private void replaceEmerald(Region region, Player player) throws MaxChangedBlocksException, EmptyClipboardException {
        this.world1 = player.getWorld();
        BlockVector3 centerBlock = region.getCenter().toBlockPoint();
        boolean terraformDown = this.height < player.getWorld().getHighestBlockAt(centerBlock.getBlockX(), centerBlock.getBlockZ()).getY() - 1;
        if (region.getHeight() > 50) {
            player.sendMessage(BTEGGamemode.PREFIX + "You cannot terraform areas with height more than 50 blocks difference!");
        }
        player.sendMessage(BTEGGamemode.PREFIX + "Terraforming started. Please wait a short moment!");
      //  player.performCommand("/copy"); // Am anfang benutzt aber speichern des clipboards funktioniert jetzt nur mit der worldedit api
        //WorldEdit Clipboard backup
        this.saveBackup(player, this.polyRegion);
       // backup.setOrigin(koordinaten);
        //

        for (int i = region.getMinimumPoint().getBlockX(); i <= region.getMaximumPoint().getBlockX(); i++) {
            for (int j = region.getMinimumPoint().getBlockY(); j <= region.getMaximumPoint().getBlockY(); j++) {
                for (int k = region.getMinimumPoint().getBlockZ(); k <= region.getMaximumPoint().getBlockZ(); k++) {
                    if (region.contains(BlockVector3.at(i, j, k))) {
                        Block block = this.world1.getBlockAt(i, j, k);
                        if (block.getType().equals(Material.DIRT_PATH)) {
                            block.setType(Material.LAPIS_BLOCK);
                        }

                        if (terraformDown) {
                            for (int z = j; z >= this.height; z--) {

                                if (block.getLocation().getBlockY() == (this.height + 1) && !(block.getType().equals(Material.LAPIS_BLOCK) || block.getType().equals(Material.GRAY_CONCRETE) || block.getType().equals(Material.BRICK))) {
                                    this.world1.getBlockAt(i, this.height, k).setType(Material.EMERALD_BLOCK);
                                }

                                if (block.getType().equals(Material.LAPIS_BLOCK)) {
                                    this.world1.getBlockAt(i, z, k).setType(Material.LAPIS_BLOCK);
                                }
                                if (block.getType().equals(Material.GRAY_CONCRETE)) {
                                    this.world1.getBlockAt(i, z, k).setType(Material.GRAY_CONCRETE);
                                }
                                if (block.getType().equals(Material.BRICKS)) {
                                    this.world1.getBlockAt(i, z, k).setType(Material.BRICKS);
                                }
                            }
                            for (int z = j; z > this.height; z--) {
                                if (block.getLocation().getBlockY() > this.height + 2) {
                                    this.world1.getBlockAt(i, z, k).setType(Material.AIR);
                                }
                            }

                        }

                        if (!terraformDown) {
                            for (int z = j; z >= region.getMinimumPoint().getBlockY(); z--) {
                                if (block.getLocation().getBlockY() == (region.getMinimumPoint().getBlockY()) && !(block.getType().equals(Material.LAPIS_BLOCK) || block.getType().equals(Material.GRAY_CONCRETE) || block.getType().equals(Material.BRICK))) {
                                    this.world1.getBlockAt(i, z, k).setType(Material.EMERALD_BLOCK);
                                }
                            }
                            for (int z = j; z <= this.height; z++) {
                                if (block.getLocation().getBlockY() < this.height) {
                                    if (block.getType().equals(Material.EMERALD_BLOCK)) {
                                        this.world1.getBlockAt(i, z, k).setType(Material.EMERALD_BLOCK);
                                    }
                                    if (block.getType().equals(Material.LAPIS_BLOCK)) {
                                        this.world1.getBlockAt(i, z, k).setType(Material.LAPIS_BLOCK);
                                    }
                                    if (block.getType().equals(Material.GRAY_CONCRETE)) {
                                        this.world1.getBlockAt(i, z, k).setType(Material.GRAY_CONCRETE);
                                    }
                                    if (block.getType().equals(Material.BRICK)) {
                                        this.world1.getBlockAt(i, z, k).setType(Material.BRICKS);
                                    }
                                }
                            }

                        }
                    }
                }
            }
        }
        this.polyRegion.setMinimumY(this.preHeight[0]);
        this.polyRegion.setMaximumY(this.preHeight[1]);
        player.sendMessage(BTEGGamemode.PREFIX + "Area succesfully terraformed to height §l" + (this.height + 1) + "! Type </terraform undo> for undo.");
    }

}