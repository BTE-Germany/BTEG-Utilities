package de.leander.bteggamemode.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public class BedrockTerraBlock implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerLoginEvent event){

        if(event.getPlayer().getUniqueId().toString().startsWith("00000000-0000-0000-")){
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "§b§lBTEG §7» Dieser Server ist leider nicht auf der Bedrock-Edition verfügbar!");
        }

    }

}
