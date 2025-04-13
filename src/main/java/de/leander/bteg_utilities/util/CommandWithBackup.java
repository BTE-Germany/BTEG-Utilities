package de.leander.bteg_utilities.util;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import de.leander.bteg_utilities.BTEGUtilities;
import org.bukkit.entity.Player;

public class CommandWithBackup {

    private Player player;
    private Clipboard backup;
    private BlockVector3 coordinates;

    public synchronized void saveBackup(Player player, Region region) {
        this.player = player;
        this.coordinates = region.getMinimumPoint();

        this.backup = new BlockArrayClipboard(region);

        ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(
                BukkitAdapter.adapt(player.getWorld()), region, this.backup, region.getMinimumPoint()
        );

        Operations.complete(forwardExtentCopy);
    }

    public synchronized void pasteBackup() {
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(this.player.getWorld()))) {
            Operation operation = new ClipboardHolder(this.backup)
                    .createPaste(editSession)
                    .to(this.coordinates)
                    .ignoreAirBlocks(false)
                    .build();
            Operations.complete(operation);
        }
        this.player.sendMessage(BTEGUtilities.PREFIX + "Undo successful!");
    }

}
