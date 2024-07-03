package de.leander.bteggamemode.util;


import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.registry.LegacyMapper;
import org.bukkit.entity.Player;


public class Converter {

   // private static final EarthGeneratorSettings bteGeneratorSettings = EarthGeneratorSettings.parse(EarthGeneratorSettings.BTE_DEFAULT_SETTINGS);

    public static double[] regionFileToMcCoords(String fileName) {
        int regionX;
        int regionZ;
        if (fileName.startsWith("r.")) {
            String[] parts = fileName.split("\\.");
            regionX = Integer.parseInt(parts[1]);
            regionZ = Integer.parseInt(parts[2]);
        } else {
            String[] parts = fileName.split("\\.");
            regionX = Integer.parseInt(parts[0]);
            regionZ = Integer.parseInt(parts[2]);
        }

        int chunkX = regionX << 5;
        int chunkZ = regionZ << 5;
        double x = (double) (chunkX << 4);
        double z = (double) (chunkZ << 4);
        if (fileName.startsWith("r.")) {
            return new double[]{x, z};
        } else {
            return new double[]{x / 2, z / 2};
        }
    }

    public static String mcCoordsToRegionFile(double x, double z, boolean isVanilla) {
        int chunkX = (int) x >> 4;
        int chunkZ = (int) z >> 4;
        int regionX = chunkX >> 5;
        int regionZ = chunkZ >> 5;
        return isVanilla ? "r." + regionX + "." + regionZ + ".mca" : (regionX * 2) + "." + "0" + "." + (regionZ * 2) + ".3dr\n" + regionX + "." + regionZ + ".2dr";
    }
/*
    public static double[] fromGeo(double[] coordinates) throws OutOfProjectionBoundsException {
        return bteGeneratorSettings.projection().fromGeo(coordinates[0], coordinates[1]);
    }

    public static double[] toGeo(double[] mccoordinates) throws OutOfProjectionBoundsException {
        double[] coords = bteGeneratorSettings.projection().toGeo(mccoordinates[0], mccoordinates[1]);
        return new double[]{coords[1],coords[0]};
    }

 */

    public static boolean isLegacyID(String input) {
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
            } catch (NumberFormatException | IndexOutOfBoundsException ignored) {}
        } else {
            blockType = BlockTypes.get(pattern.toLowerCase());
        }
        return blockType;
    }

    public static BlockType getBlockType(String pattern, Player player){
        if (pattern.equalsIgnoreCase("hand")) {
            return BlockTypes.parse(player.getInventory().getItemInMainHand().getType().toString());
        }
        return Converter.getBlockType(pattern);
    }

}
