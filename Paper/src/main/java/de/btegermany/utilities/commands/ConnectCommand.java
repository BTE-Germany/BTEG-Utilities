package de.btegermany.utilities.commands;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.world.block.*;
import de.btegermany.utilities.BTEGUtilities;
import de.btegermany.utilities.util.*;
import de.btegermany.utilities.util.worldedit.Converter;
import de.btegermany.utilities.util.worldedit.EditSessionWithHistory;
import de.btegermany.utilities.util.worldedit.SelectionEditSession;
import de.btegermany.utilities.util.worldedit.WorldEditUtil;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static java.util.Collections.emptyList;


public class ConnectCommand implements TabExecutor {

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
        }

        try {
            WorldEditUtil.findSelection(player, session -> {
                this.createLine(session, args[0], args[0].equalsIgnoreCase("plot"));
            });
        } catch (MaxChangedBlocksException | EmptyClipboardException e) {
            BTEGUtilities.getPlugin().getComponentLogger().warn("Connect Failed because of empty Clipboard or too much blocks.", e);
        }

        return true;
    }

    private void createLine(SelectionEditSession session, String pattern, boolean plot) throws MaxChangedBlocksException, EmptyClipboardException {
        Player player = session.player();

        // Check if WorldEdit selection is polygonal
        if (!(session.region() instanceof Polygonal2DRegion polyRegion)) {
            player.sendMessage("§7§l>> §cPlease use poly selection to connect!");
            return;
        }

        List<BlockVector2> points = polyRegion.getPoints();
        int y = polyRegion.getMaximumPoint().y();

        BlockType blockType;
        if (plot) {
            blockType = BlockTypes.get("lapis_block");
        } else {
            blockType = Converter.getBlockType(pattern, player);
        }

        assert blockType != null;
        BlockState blockState = blockType.getDefaultState();

        for (int i = 0; points.size() > i; i++) {
            try (EditSessionWithHistory editSessionWithHistory = new EditSessionWithHistory(session.localSession(), player)) {
                EditSession editSession = editSessionWithHistory.getWeEditSession();

                BlockVector3 vector = BlockVector3.at(points.get(i).x(), y, points.get(i).z());
                BlockVector3 vector1;
                if (i == points.size() - 1) {
                    vector1 = BlockVector3.at(points.get(i + 1 - points.size()).x(), y, points.get(i + 1 - points.size()).z());
                } else {
                    vector1 = BlockVector3.at(points.get(i + 1).x(), y, points.get(i + 1).z());
                }
                editSession.drawLine(blockState, vector, vector1, 0, true);
            }
        }

        if (plot) {
            player.chat("//re !22 82");
            player.sendMessage(BTEGUtilities.PREFIX + "Successfully prepared plot!");
        } else {
            player.sendMessage(BTEGUtilities.PREFIX + "Blocks successfully connected!");
        }

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("bteg.builder")) {
            return emptyList();
        }
        // First argument: target
        if (args.length == 1) {
            return TabUtil.getMaterialBlocks(args[0], true);
        }

        return emptyList();
    }
}
