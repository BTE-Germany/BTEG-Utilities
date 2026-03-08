package de.btegermany.utilities.commands;

import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import de.btegermany.utilities.BTEGUtilities;
import de.btegermany.utilities.util.Direction;
import de.btegermany.utilities.util.worldedit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class CoverCommand implements CommandExecutor {

    private static final Map<BlockType, BlockType> GLASS_TYPES_BACKGROUNDS = Map.of(
            Converter.getBlockType("gray_stained_glass"), Converter.getBlockType("cyan_terracotta"),
            Converter.getBlockType("light_gray_stained_glass"), Converter.getBlockType("gray_wool")
    );

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player) || !(command.getName().equalsIgnoreCase("cover") || command.getName().equalsIgnoreCase("/cover"))) {
            return true;
        }
        if (!player.hasPermission("bteg.builder")) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cNo permission for //cover");
            return true;
        }

        WorldEditUtil.findSelection(player, session -> {
            GLASS_TYPES_BACKGROUNDS.forEach((glass, background) -> {
                SideCommand.replaceSide(session, new ReplaceSideArgs(glass, background, Direction.DOWN, false, new TypeOnlyMask(BlockTypes.AIR)));
                SideCommand.replaceSide(session, new ReplaceSideArgs(glass, background, Direction.UP, false, new TypeOnlyMask(BlockTypes.AIR)));
                SideCommand.replaceSide(session, new ReplaceSideArgs(glass, background, Direction.NORTH, false, new TypeOnlyMask(BlockTypes.AIR)));
                SideCommand.replaceSide(session, new ReplaceSideArgs(glass, background, Direction.EAST, false, new TypeOnlyMask(BlockTypes.AIR)));
                SideCommand.replaceSide(session, new ReplaceSideArgs(glass, background, Direction.SOUTH, false, new TypeOnlyMask(BlockTypes.AIR)));
                SideCommand.replaceSide(session, new ReplaceSideArgs(glass, background, Direction.WEST, false, new TypeOnlyMask(BlockTypes.AIR)));
            });
        });

        return true;
    }
}
