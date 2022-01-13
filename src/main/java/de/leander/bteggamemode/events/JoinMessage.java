package de.leander.bteggamemode.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinMessage implements Listener {

    @EventHandler
    public static void onPLayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        event.setJoinMessage("§b§lBTEG §7» " + player.getDisplayName()+ " switched to §a"+Bukkit.getServerName()+"");

    }

    @EventHandler
    public static void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        event.setQuitMessage("§b§lBTEG §7» " + player.getDisplayName()+ " switched to another server");
    }
}
