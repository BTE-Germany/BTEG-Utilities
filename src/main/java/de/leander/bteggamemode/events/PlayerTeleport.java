package de.leander.bteggamemode.events;

import de.leander.bteggamemode.BTEGGamemode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerTeleport implements Listener {

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        if (!((event.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE) && !player.hasPermission("teleportation.tp.player"))) {
            return;
        }
        event.setCancelled(true);
        player.sendMessage(BTEGGamemode.PREFIX + "§cDu bist dazu nicht berechtigt.");
    }

}
