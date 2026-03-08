package de.btegermany.utilities.util.worldedit;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import de.btegermany.utilities.BTEGUtilities;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class EditSessionWithHistory implements AutoCloseable {

    private final LocalSession localSession;
    private final EditSession editSession;

    public EditSessionWithHistory(LocalSession localSession, Player player) {
        this.localSession = localSession;
        this.editSession = localSession.createEditSession(BukkitAdapter.adapt(player));
    }

    public EditSession getWeEditSession() {
        return editSession;
    }

    @Override
    public void close() {
        this.editSession.close();

        // without delay in rare cases a rejoin would be required so //undo works
        Bukkit.getScheduler().runTaskLater(BTEGUtilities.getPlugin(), () -> {
            this.localSession.remember(this.editSession);
            this.localSession.save();
        }, 5L);
    }
}
