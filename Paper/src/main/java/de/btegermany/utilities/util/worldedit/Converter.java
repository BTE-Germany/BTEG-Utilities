package de.btegermany.utilities.util.worldedit;


import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.factory.parser.DefaultBlockParser;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;


public class Converter {
    private Converter() {}
    private static DefaultBlockParser defaultBlockParser;
    private static ParserContext defaultParserContext;

    /**
     * If there is a player it's recommended to use {@link #getBlockType(String pattern, Player player)}
     */
    public static BlockType getBlockType(String pattern) {
        return getBlockParser().parseFromInput(pattern, getParserContext()).getBlockType();
    }

    public static BlockType getBlockType(@NotNull String pattern, Player player){
        if (pattern.equalsIgnoreCase("hand")) {
            return BlockTypes.parse(player.getInventory().getItemInMainHand().getType().toString());
        }
        return Converter.getBlockType(pattern);
    }

    private static ParserContext getParserContext() {
        if (defaultParserContext == null) {
            defaultParserContext = new ParserContext();
            defaultParserContext.setRestricted(false);
        }
        return defaultParserContext;
    }

    private static DefaultBlockParser getBlockParser() {
        if (defaultBlockParser == null) {
            defaultBlockParser = new DefaultBlockParser(WorldEdit.getInstance());
        }
        return defaultBlockParser;
    }

}
