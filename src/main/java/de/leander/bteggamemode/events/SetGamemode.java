package de.leander.bteggamemode.events;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class SetGamemode implements Listener {

    @EventHandler
    public static void onPLayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("bteg.builder")) {
            player.setGameMode(GameMode.CREATIVE);
            player.sendMessage("§b§lBTEG §7» Gamemode set to creative!");
        }
        else{
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage("§b§lBTEG §7» Gamemode set to spectator! You can only load already generated chunks!");
        }
    }

}
