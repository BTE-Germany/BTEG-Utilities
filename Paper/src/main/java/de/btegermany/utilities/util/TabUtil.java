package de.btegermany.utilities.util;

import com.destroystokyo.paper.MaterialSetTag;
import de.btegermany.utilities.util.worldedit.Converter;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TabUtil {

    public static List<String> getMaterialBlocks(String arg, boolean hand) {
        // get all materials from Material enum and convert it to a list of lowercase strings
        List<String> list = new ArrayList<>(Arrays.stream((Material.values()))
                .filter(Material::isBlock)
                .map(material -> material.name().toLowerCase())
                .toList());
        if(hand) {
            list.add("hand");
        }
        return list.stream()
                .filter(name -> name.contains(arg.toLowerCase()))
                .toList();
    }

    public static List<String> getMaterialBlocks(String arg, String startOfArg, boolean hand) {
        return getMaterialBlocks(arg, hand).stream().map(block -> startOfArg + block).toList();
    }

    /**
     * Like {@link #getMaterialBlocks(String, boolean)}, but also suggests the WorldEdit/FAWE
     * block state bracket syntax (e.g. completing {@code stone_stairs[faci} to
     * {@code stone_stairs[facing=}), for use with block-ID arguments that support block states.
     */
    public static List<String> getBlockPatternSuggestions(String arg, boolean hand) {
        List<String> suggestions = new ArrayList<>(Converter.getBlockSuggestions(arg));
        if (hand && !arg.contains("[") && "hand".startsWith(arg.toLowerCase())) {
            suggestions.add("hand");
        }
        return suggestions;
    }

    public static List<String> getWallBlocks(String arg) {
        return MaterialSetTag.WALLS.getValues().stream()
                .map(material -> material.toString().toLowerCase())
                .filter(wallType -> wallType.contains(arg.toLowerCase()))
                .toList();
    }
}

