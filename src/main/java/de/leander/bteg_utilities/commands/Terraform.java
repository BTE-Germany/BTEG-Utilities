package de.leander.bteg_utilities.commands;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import de.leander.bteg_utilities.BTEGUtilities;
import de.leander.bteg_utilities.util.CommandWithBackup;
import net.kyori.adventure.text.Component;
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

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player) || !command.getName().equalsIgnoreCase("terraform")) {
            return true;
        }
        if(!player.hasPermission("bteg.builder")) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cNo permission for /terraform");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("No arguments specified. You need to specify a height or undo."));
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
            player.sendMessage(BTEGUtilities.PREFIX + "§cAn error occurred.");
            BTEGUtilities.getPlugin().getComponentLogger().error("An error occurred.", e);
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
            BTEGUtilities.getPlugin().getComponentLogger().error("No WorldEdit Selection.", ex);
            player.sendMessage(BTEGUtilities.PREFIX + "§cPlease select a WorldEdit selection!");
            return;
        }
        try {
            // Check if WorldEdit selection is polygonal
            if (!(plotRegion instanceof Polygonal2DRegion polyRegion))  {
                player.sendMessage(BTEGUtilities.PREFIX + "§cPlease use poly selection to terraform!");
                return;
            }

                // Cast WorldEdit region to polygonal region
                if (polyRegion.getLength() > 300 || polyRegion.getWidth() > 300 || polyRegion.getHeight() > 60) {
                    player.sendMessage(BTEGUtilities.PREFIX + "§cPlease adjust your selection size!");
                    return;
                }
                // Set minimum selection height under player location
                this.preHeight[0] = polyRegion.getMinimumY();
                this.preHeight[1] = polyRegion.getMaximumY();

                polyRegion.setMinimumY(this.height);
                polyRegion.setMaximumY(this.height + 35);

            replaceEmerald(polyRegion, player);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        } catch (Exception ex) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cAn error occurred while select this area!");
            BTEGUtilities.getPlugin().getComponentLogger().error("An error occurred while select this area!", ex);
        }
    }

    private void replaceEmerald(@NotNull Polygonal2DRegion region, @NotNull Player player) throws MaxChangedBlocksException, EmptyClipboardException {
        this.world1 = player.getWorld();
        BlockVector3 centerBlock = region.getCenter().toBlockPoint();
        boolean terraformDown = this.height < player.getWorld().getHighestBlockAt(centerBlock.x(), centerBlock.z()).getY() - 1;
        if (region.getHeight() > 50) {
            player.sendMessage(BTEGUtilities.PREFIX + "You cannot terraform areas with height more than 50 blocks difference!");
        }
        player.sendMessage(BTEGUtilities.PREFIX + "Terraforming started. Please wait a short moment!");
        //WorldEdit Clipboard backup
        this.saveBackup(player, region);

        for (int i = region.getMinimumPoint().x(); i <= region.getMaximumPoint().x(); i++) {
            for (int j = region.getMinimumPoint().y(); j <= region.getMaximumPoint().y(); j++) {
                for (int k = region.getMinimumPoint().z(); k <= region.getMaximumPoint().z(); k++) {
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
                            for (int z = j; z >= region.getMinimumPoint().y(); z--) {
                                if (block.getLocation().getBlockY() == (region.getMinimumPoint().y()) && !(block.getType().equals(Material.LAPIS_BLOCK) || block.getType().equals(Material.GRAY_CONCRETE) || block.getType().equals(Material.BRICK))) {
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
        region.setMinimumY(this.preHeight[0]);
        region.setMaximumY(this.preHeight[1]);
        player.sendMessage(BTEGUtilities.PREFIX + "Area succesfully terraformed to height §l" + (this.height + 1) + "! Type </terraform undo> for undo.");
    }

}