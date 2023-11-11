package de.leander.bteggamemode.util;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import de.leander.bteggamemode.BTEGGamemode;
import org.bukkit.entity.Player;

public class WorldEditUtil {

    public static Clipboard saveBackup(Region pRegion, Player player){
        Clipboard backup = new BlockArrayClipboard(pRegion);

        ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(
                BukkitAdapter.adapt(player.getWorld()), pRegion, backup, pRegion.getMinimumPoint()
        );

        Operations.complete(forwardExtentCopy);

        return backup;
    }

    public static void pasteBackup(Player player, Clipboard backup, BlockVector3 koordinaten) {
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(player.getWorld()))) {
            Operation operation = new ClipboardHolder(backup)
                    .createPaste(editSession)
                    .to(koordinaten)
                    .ignoreAirBlocks(true)
                    .build();
            Operations.complete(operation);
        }
        player.sendMessage(BTEGGamemode.prefix + "Undo succesful!");
    }

}
