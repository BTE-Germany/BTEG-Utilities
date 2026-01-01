package de.btegermany.utilities.util;

import com.destroystokyo.paper.MaterialSetTag;
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

    public static List<String> getWallBlocks(String arg) {
        return MaterialSetTag.WALLS.getValues().stream()
                .map(material -> material.toString().toLowerCase())
                .filter(wallType -> wallType.contains(arg.toLowerCase()))
                .toList();
    }
}
