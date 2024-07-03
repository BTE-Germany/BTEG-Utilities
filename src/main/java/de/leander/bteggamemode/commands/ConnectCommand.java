package de.leander.bteggamemode.commands;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.SessionManager;
import com.sk89q.worldedit.world.block.*;
import de.leander.bteggamemode.BTEGGamemode;
import de.leander.bteggamemode.util.CommandWithBackup;
import de.leander.bteggamemode.util.Converter;
import de.leander.bteggamemode.util.TabUtil;
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
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player) || !(command.getName().equalsIgnoreCase("connect") || command.getName().equalsIgnoreCase("/connect"))) {
            return true;
        }
        if (!player.hasPermission("bteg.builder")) {
            player.sendMessage(BTEGGamemode.PREFIX + "§cNo permission for //connect");
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(BTEGGamemode.PREFIX + "§cWrong usage");
            player.sendMessage(BTEGGamemode.PREFIX + "/connect <Block-ID>");
            player.sendMessage(BTEGGamemode.PREFIX + "/connect <undo>");
        }

        if (args[0].equals("undo")) {
            this.pasteBackup();
        } else {
            try {
                if(args[0].equalsIgnoreCase("plot")){
                    this.terraform(player, args[0],true);
                } else {
                    this.terraform(player, args[0],false);
                }
            } catch (MaxChangedBlocksException | EmptyClipboardException e) {
                e.printStackTrace();
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
            ex.printStackTrace();
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
            //WorldEdit CLipboard backup
            this.saveBackup(player, region);

            List<BlockVector2> points = this.polyRegion.getPoints();
            int y = this.polyRegion.getMaximumPoint().getBlockY();

            BlockType blockType;
            if (plot) {
                blockType = BlockTypes.get("lapis_block");
            } else {
                blockType = Converter.getBlockType(pattern, player);
            }

            BlockState blockState = blockType.getDefaultState();

            Pattern pat = blockState; //new RandomStatePattern(new FuzzyBlockState(blockState));

            BukkitPlayer actor = BukkitAdapter.adapt(player);
            SessionManager manager = WorldEdit.getInstance().getSessionManager();
            LocalSession localSession = manager.get(actor);

            for (int i = 0; points.size() > i; i++){
                EditSession editSession = localSession.createEditSession(actor);
                if (i == points.size() - 1) {
                    BlockVector3 vector = BlockVector3.at(points.get(i).getBlockX(), y, points.get(i).getBlockZ());
                    BlockVector3 vector1 = BlockVector3.at(points.get(i + 1 - points.size()).getBlockX(), y, points.get(i + 1 - points.size()).getBlockZ());
                    editSession.drawLine(pat,vector, vector1,0,true);
                    localSession.remember(editSession);
                } else {
                    BlockVector3 vector = BlockVector3.at(points.get(i).getBlockX(), y, points.get(i).getBlockZ());
                    BlockVector3 vector1 = BlockVector3.at(points.get(i + 1).getBlockX(), y, points.get(i + 1).getBlockZ());
                    editSession.drawLine(pat, vector, vector1, 0, true);
                    localSession.remember(editSession);
                }
                editSession.close();
            }
            if (plot) {
                player.chat("//re !22 82");
                player.sendMessage(BTEGGamemode.PREFIX + "Successfully prepared plot!");
            } else {
                player.sendMessage(BTEGGamemode.PREFIX + "Blocks successfully connected!");
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
