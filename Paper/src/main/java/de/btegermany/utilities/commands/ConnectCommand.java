package de.btegermany.utilities.commands;

import static java.util.Collections.emptyList;

import java.util.Arrays;
import java.util.List;

import com.fastasyncworldedit.core.function.mask.InverseMask;
import com.sk89q.worldedit.function.mask.BlockTypeMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;

import de.btegermany.utilities.BTEGUtilities;
import de.btegermany.utilities.util.TabUtil;
import de.btegermany.utilities.util.worldedit.Converter;
import de.btegermany.utilities.util.worldedit.EditSessionWithHistory;
import de.btegermany.utilities.util.worldedit.SelectionEditSession;
import de.btegermany.utilities.util.worldedit.WorldEditUtil;


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
        if (args.length < 1 || args.length > 2) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cWrong usage");
            player.sendMessage(BTEGUtilities.PREFIX + "/connect <Block-ID> [open|closed]");
            return true;
        }

        if (!args[0].equalsIgnoreCase("plot")) {
            try {
                Converter.getBlockState(args[0], player);
            } catch (RuntimeException exception) {
                player.sendMessage(BTEGUtilities.PREFIX + "§cInvalid block type: " + args[0]);
                return true;
            }
        }

        var open = args.length == 2 && args[1].equalsIgnoreCase("open");

        try {
            WorldEditUtil.findSelection(player, session -> {
                this.createLine(session, args[0], args[0].equalsIgnoreCase("plot"), open);
            });
        } catch (MaxChangedBlocksException | EmptyClipboardException e) {
            BTEGUtilities.getPlugin().getComponentLogger().warn("Connect Failed because of empty Clipboard or too much blocks.", e);
        }

        return true;
    }

    private void createLine(SelectionEditSession session, String pattern, boolean plot, boolean open) throws MaxChangedBlocksException, EmptyClipboardException {
        Player player = session.player();

        // Check if WorldEdit selection is polygonal
        if (!(session.region() instanceof Polygonal2DRegion polyRegion)) {
            player.sendMessage("§7§l>> §cPlease use poly selection to connect!");
            return;
        }

        List<BlockVector2> points = polyRegion.getPoints();
        int y = polyRegion.getMaximumPoint().y();

        BlockState blockState;
        if (plot) {
            assert BlockTypes.LAPIS_BLOCK != null;
            blockState = BlockTypes.LAPIS_BLOCK.getDefaultState();
        } else {
            blockState = Converter.getBlockState(pattern, player);
        }

        assert blockState != null;

        // Use a single session for all lines, so drawing (and, if needed, undoing) the whole
        // connection only takes one operation instead of one per line segment.
        try (EditSessionWithHistory editSessionWithHistory = new EditSessionWithHistory(session.localSession(), player)) {
            EditSession editSession = editSessionWithHistory.getWeEditSession();

            if (plot) {
                BlockTypeMask lapisMask = new BlockTypeMask(editSession, BlockTypes.LAPIS_BLOCK);
                Mask nonLapisMask = new InverseMask(lapisMask);

                assert BlockTypes.CLAY != null;
                Pattern clayPattern = BlockTypes.CLAY.getDefaultState();

                editSession.replaceBlocks(session.region(), nonLapisMask, clayPattern);
            }

            int maxCount = open ? points.size() - 1 : points.size();

            for (int i = 0; maxCount > i; i++) {
                BlockVector3 vector = BlockVector3.at(points.get(i).x(), y, points.get(i).z());
                BlockVector3 vector1;
                if (i == points.size() - 1) {
                    vector1 = BlockVector3.at(points.getFirst().x(), y, points.getFirst().z());
                } else {
                    vector1 = BlockVector3.at(points.get(i + 1).x(), y, points.get(i + 1).z());
                }
                editSession.drawLine(blockState, vector, vector1, 0, true);
            }
        }

        if (plot) {
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
            return TabUtil.getBlockPatternSuggestions(args[0], true);
        }

        // Second argument: whether the lines should be a closed loop
        if (args.length == 2) {
            return Arrays.asList("open", "closed");
        }

        return emptyList();
    }
}
