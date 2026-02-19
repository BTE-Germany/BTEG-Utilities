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
           blockType = switch (pattern.toLowerCase()) {
            case "black" -> BlockTypes.get("black_wool");
            case "blue" -> BlockTypes.get("blue_wool");
            case "brown" -> BlockTypes.get("brown_wool");
            case "cyan" -> BlockTypes.get("cyan_wool");
            case "gray" -> BlockTypes.get("gray_wool");
            case "grey" -> BlockTypes.get("gray_wool");
            case "green" -> BlockTypes.get("green_wool");
            case "light_blue" -> BlockTypes.get("light_blue_wool");
            case "light_gray" -> BlockTypes.get("light_gray_wool");
            case "light_grey" -> BlockTypes.get("light_gray_wool");
            case "lime" -> BlockTypes.get("lime_wool");
            case "magenta" -> BlockTypes.get("magenta_wool");
            case "orange" -> BlockTypes.get("orange_wool");
            case "pink" -> BlockTypes.get("pink_wool");
            case "purple" -> BlockTypes.get("purple_wool");
            case "red" -> BlockTypes.get("red_wool");
            case "white" -> BlockTypes.get("white_wool");
            case "yellow" -> BlockTypes.get("yellow_wool");
            default -> BlockTypes.get(pattern.toLowerCase());
           };
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
