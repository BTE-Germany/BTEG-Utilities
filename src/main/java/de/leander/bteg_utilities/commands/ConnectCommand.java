package de.leander.bteg_utilities.commands;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.SessionManager;
import com.sk89q.worldedit.world.block.*;
import de.leander.bteg_utilities.BTEGUtilities;
import de.leander.bteg_utilities.util.CommandWithBackup;
import de.leander.bteg_utilities.util.Converter;
import de.leander.bteg_utilities.util.TabUtil;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;


public class ConnectCommand extends CommandWithBackup implements TabExecutor {

    private Polygonal2DRegion polyRegion;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player) || !(command.getName().equalsIgnoreCase("connect") || command.getName().equalsIgnoreCase("/connect"))) {
            return true;
        }
        if (!player.hasPermission("bteg.builder")) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cNo permission for //connect");
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cWrong usage");
            player.sendMessage(BTEGUtilities.PREFIX + "/connect <Block-ID>");
            player.sendMessage(BTEGUtilities.PREFIX + "/connect <undo>");
        }

        if (args[0].equals("undo")) {
            this.pasteBackup();
        } else {
            try {
                this.terraform(player, args[0], args[0].equalsIgnoreCase("plot"));
            } catch (MaxChangedBlocksException | EmptyClipboardException e) {
                BTEGUtilities.getPlugin().getComponentLogger().warn("Connect Failed because of empty Clipboard or too much blocks.", e);
            }
        }

        return true;
    }

    private void terraform(Player player, String pattern, boolean plot) throws MaxChangedBlocksException, EmptyClipboardException {
        Region plotRegion;
        // Get WorldEdit selection of player
        try {
            LocalSession localSession = WorldEdit.getInstance().getSessionManager().findByName(player.getName());
            if(localSession == null) {
                return;
            }
            plotRegion = localSession.getSelection(localSession.getSelectionWorld());
        } catch (NullPointerException | IncompleteRegionException ex) {
            BTEGUtilities.getPlugin().getComponentLogger().warn("Connect failed because there is no WorldEdit selection (we assume). ", ex);
            player.sendMessage("§7§l>> §cPlease select a WorldEdit selection!");
            return;
        }
        try {
            // Check if WorldEdit selection is polygonal
            if (!(plotRegion instanceof Polygonal2DRegion)) {
                player.sendMessage("§7§l>> §cPlease use poly selection to connect!");
                return;
            }

            // Cast WorldEdit region to polygonal region
            this.polyRegion = (Polygonal2DRegion) plotRegion;
            if (this.polyRegion.getLength() > 500 || this.polyRegion.getWidth() > 500 || this.polyRegion.getHeight() > 30) {
                player.sendMessage("§7§l>> §cPlease adjust your selection size!");
                return;
            }
            // Set minimum selection height under player location

        } catch (Exception ex) {
            player.sendMessage("§7§l>> §cAn error occurred while selection area!");
            return;
        }

        this.createLine(this.polyRegion, player, pattern, plot);

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

    }

    private void createLine(Region region, Player player, String pattern, boolean plot) throws MaxChangedBlocksException, EmptyClipboardException {
            this.saveBackup(player, region);

            List<BlockVector2> points = this.polyRegion.getPoints();
            int y = this.polyRegion.getMaximumPoint().y();

            BlockType blockType;
            if (plot) {
                blockType = BlockTypes.get("lapis_block");
            } else {
                blockType = Converter.getBlockType(pattern, player);
            }

        assert blockType != null;
        BlockState blockState = blockType.getDefaultState();

        BukkitPlayer actor = BukkitAdapter.adapt(player);
            SessionManager manager = WorldEdit.getInstance().getSessionManager();
            LocalSession localSession = manager.get(actor);

            for (int i = 0; points.size() > i; i++){
                EditSession editSession = localSession.createEditSession(actor);
                BlockVector3 vector = BlockVector3.at(points.get(i).x(), y, points.get(i).z());
                BlockVector3 vector1;
                if (i == points.size() - 1) {
                    vector1 = BlockVector3.at(points.get(i + 1 - points.size()).x(), y, points.get(i + 1 - points.size()).z());
                } else {
                    vector1 = BlockVector3.at(points.get(i + 1).x(), y, points.get(i + 1).z());
                }
                editSession.drawLine(blockState, vector, vector1,0,true);
                localSession.remember(editSession);
                editSession.close();
            }
            if (plot) {
                player.chat("//re !22 82");
                player.sendMessage(BTEGUtilities.PREFIX + "Successfully prepared plot!");
            } else {
                player.sendMessage(BTEGUtilities.PREFIX + "Blocks successfully connected!");
            }


    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("bteg.builder")) {
            return emptyList();
        }
        // First argument: target
        if (args.length == 1) {
            List<String> list = new ArrayList<>(TabUtil.getMaterialBlocks(args[0], true));
            if("undo".contains(args[0].toLowerCase())) {
                list.add("undo");
            }
            return list;
        }

        return emptyList();
    }
}
