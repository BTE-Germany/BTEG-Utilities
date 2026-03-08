package de.btegermany.utilities.util.worldedit;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import de.btegermany.utilities.BTEGUtilities;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class WorldEditUtil {

    private static final int MAX_SELECTION_SIZE = 700 * 700 * 300;

    public static void findSelection(Player player, Consumer<SelectionEditSession> callback) throws MaxChangedBlocksException, EmptyClipboardException {
        LocalSession localSession = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));

        Region region;
        try {
            region = localSession.getSelection(localSession.getSelectionWorld());
        } catch (NullPointerException | IncompleteRegionException ex) {
            ex.printStackTrace();
            player.sendMessage(BTEGUtilities.PREFIX + " §cPlease select a WorldEdit selection!");
            return;
        }

        if (!((region instanceof CuboidRegion) || (region instanceof Polygonal2DRegion))) {
            player.sendMessage("§7§l>> §cPlease use cuboid or poly selections!");
            return;
        }

        if ((region.getVolume() > MAX_SELECTION_SIZE) && !player.hasPermission("bteg.advanced")) {
            player.sendMessage("§7§l>> §cPlease adjust your selection size!");
            return;
        }

        try (EditSessionWithHistory editSession = new EditSessionWithHistory(localSession, player)) {
            callback.accept(new SelectionEditSession(player, region, editSession.getWeEditSession(), localSession, new HashMap<>()));
        }
    }

    public static void replaceAll(SelectionEditSession session, List<ReplaceArgs> allReplaceArgs) throws MaxChangedBlocksException {
        Region region = session.region();
        EditSession editSession = session.editSession();
        Map<BlockVector3, BlockState> changedBlocks = session.changedBlocks();
        
        for (int i = region.getMinimumPoint().x(); i <= region.getMaximumPoint().x(); i++) {
            for (int j = region.getMinimumPoint().y(); j <= region.getMaximumPoint().y(); j++) {
                for (int k = region.getMinimumPoint().z(); k <= region.getMaximumPoint().z(); k++) {
                    
                    BlockVector3 blockVector = BlockVector3.at(i, j, k);
                    if (region.contains(blockVector)) {

                        BlockType blockType = (changedBlocks.containsKey(blockVector) ? changedBlocks.get(blockVector) : editSession.getBlock(blockVector)).getBlockType();
                        for (ReplaceArgs replaceArgs : allReplaceArgs) {
                            if (replaceArgs.maskToReplace().test(blockType)) {
                                editSession.setBlock(i, j, k, replaceArgs.blockToReplaceWith());
                                changedBlocks.put(blockVector, replaceArgs.blockToReplaceWith());
                            }
                        }
                    }
                }
            }
        }
    }

    public static BlockType[] getBlockTypesFromInput(Player player, String input) {
        return Arrays.stream(input.split(","))
                .map(blockName -> Converter.getBlockType(blockName, player))
                .toArray(BlockType[]::new);
    }

}
