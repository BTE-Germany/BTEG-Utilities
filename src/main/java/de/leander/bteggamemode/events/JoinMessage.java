package de.leander.bteggamemode.events;

import de.leander.bteggamemode.BTEGGamemode;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.configuration.PlaceholderAPIConfig;
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
        String joinMessage = BTEGGamemode.prefix + player.getDisplayName()+ " switched to §a%server_name%";
        joinMessage = PlaceholderAPI.setPlaceholders(player, joinMessage);
        event.setJoinMessage(joinMessage);

    }

    @EventHandler
    public static void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        event.setQuitMessage(BTEGGamemode.prefix + player.getDisplayName()+ " switched to another server");
        player.setWalkSpeed(0.2F);
        player.setFlySpeed(0.1F);
        }

}
