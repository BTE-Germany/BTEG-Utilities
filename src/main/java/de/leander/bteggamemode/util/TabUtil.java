package de.leander.bteggamemode.util;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TabUtil {

    public static List<String> getMaterialBlocks(String arg){
        // get all materials from Material enum and convert it to a list of lowercase strings
        List<String> list = Arrays.stream((Material.values()))
                .filter(material -> material.isBlock())
                .map(material -> material.name().toLowerCase())
                .filter(name -> name.contains(arg.toLowerCase()))
                .collect(Collectors.toList());
        return list;
    }
}
