package de.leander.bteg_utilities.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;

public class FadeFormEvent implements Listener {

    @EventHandler
    public void onIce(BlockFormEvent event) {
        event.setCancelled(true);
    }
    @EventHandler
    public void onIce(BlockFadeEvent event) {
        event.setCancelled(true);
    }

}
