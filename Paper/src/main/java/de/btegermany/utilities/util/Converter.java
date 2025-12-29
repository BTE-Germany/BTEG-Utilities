package de.btegermany.utilities.util;


import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.registry.LegacyMapper;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


public class Converter {
    private Converter() {}

    @Contract(pure = true)
    public static boolean isLegacyID(@NotNull String input) {
        char firstChar = input.charAt(0);
        return firstChar >= '0' && firstChar <= '9';
    }

    /**
     * If there is a player it's recommended to use {@link #getBlockType(String pattern, Player player)}
     */
    public static BlockType getBlockType(String pattern) {
        BlockType blockType = null;
        if (Converter.isLegacyID(pattern)) {
            try {
                BlockStateHolder<BlockState> block = LegacyMapper.getInstance().getBlockFromLegacy(pattern);
                if (block != null) {
                    blockType = block.getBlockType();
                }
            } catch (NumberFormatException | IndexOutOfBoundsException ignored) { /* Ignored */ }
        } else {
            blockType = BlockTypes.get(pattern.toLowerCase());
        }
        return blockType;
    }

    public static BlockType getBlockType(@NotNull String pattern, Player player){
        if (pattern.equalsIgnoreCase("hand")) {
            return BlockTypes.parse(player.getInventory().getItemInMainHand().getType().toString());
        }
        return Converter.getBlockType(pattern);
    }

}
