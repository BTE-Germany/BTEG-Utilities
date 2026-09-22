package de.btegermany.utilities.util.worldedit;


import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.factory.parser.DefaultBlockParser;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class Converter {
    private Converter() {}
    private static DefaultBlockParser defaultBlockParser;
    private static ParserContext defaultParserContext;

    /**
     * If there is a player it's recommended to use {@link #getBlockType(String pattern, Player player)}
     */
    public static BlockType getBlockType(String pattern) {
        return getBlockParser().parseFromInput(normalizeBrackets(pattern), getParserContext()).getBlockType();
    }

    public static BlockType getBlockType(@NotNull String pattern, Player player){
        if (pattern.equalsIgnoreCase("hand")) {
            return BlockTypes.parse(player.getInventory().getItemInMainHand().getType().toString());
        }
        return Converter.getBlockType(pattern);
    }

    /**
     * Parses a block pattern, keeping any block states attached with the WorldEdit/FAWE bracket
     * syntax (e.g. {@code stone_stairs[facing=east,half=top]}), unlike {@link #getBlockType}
     * which discards them. If there is a player it's recommended to use
     * {@link #getBaseBlock(String pattern, Player player)}.
     */
    public static BaseBlock getBaseBlock(String pattern) {
        return getBlockParser().parseFromInput(normalizeBrackets(pattern), getParserContext());
    }

    public static BaseBlock getBaseBlock(@NotNull String pattern, Player player) {
        if (pattern.equalsIgnoreCase("hand")) {
            return BlockTypes.parse(player.getInventory().getItemInMainHand().getType().toString()).getDefaultState().toBaseBlock();
        }
        return Converter.getBaseBlock(pattern);
    }

    /**
     * Like {@link #getBaseBlock(String pattern, Player player)}, but returns just the
     * {@link BlockState} (block type plus any attached block states), without NBT data.
     */
    public static BlockState getBlockState(@NotNull String pattern, Player player) {
        return Converter.getBaseBlock(pattern, player).toImmutableState();
    }

    /**
     * Just like WorldEdit/FAWE's own commands, the trailing {@code ]} of a block state pattern
     * (e.g. {@code stone_stairs[facing=east}) can be left out - WorldEdit's own parser is strict
     * about this and would otherwise reject the pattern with a "missing-rbracket" error, so we
     * add it back before parsing if it's missing.
     */
    private static String normalizeBrackets(String pattern) {
        int openBracket = pattern.indexOf('[');
        if (openBracket >= 0 && pattern.indexOf(']', openBracket) < 0) {
            return pattern + "]";
        }
        return pattern;
    }

    /**
     * Returns tab-completion suggestions for a (partial) block pattern, covering block IDs as
     * well as the WorldEdit/FAWE block state bracket syntax (property keys and values), e.g.
     * suggesting {@code stone_stairs[facing=} for input {@code stone_stairs[faci}. Filters out
     * internal/non-placeable entries like {@code __reserved__} that WorldEdit's registry exposes
     * but that aren't actual usable blocks.
     */
    public static List<String> getBlockSuggestions(String pattern) {
        return getBlockParser().getSuggestions(pattern, getParserContext())
                .filter(suggestion -> !suggestion.contains("__reserved__"))
                .toList();
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


