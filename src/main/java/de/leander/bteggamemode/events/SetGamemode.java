package de.leander.bteggamemode.events;

import de.leander.bteggamemode.BTEGGamemode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
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
            player.sendMessage(BTEGGamemode.prefix + "Gamemode set to creative!");
        }
        else{
            Component message = Component.text(BTEGGamemode.prefix + "Gamemode set to §9spectator§7! As a \uE363 can only load already generated chunks!");
            Component hover = message.hoverEvent(Component.text("Click here to understand why").color(NamedTextColor.GREEN));
            Component click = hover.clickEvent(ClickEvent.openUrl("https://docs.google.com/document/d/e/2PACX-1vS0IGQX5lcjkf4vuIsjdHPPjvpdRfTCPOdvnu_n5udBATQKdcp4nelO3Q2eM8vTPKJ3oIlPNL5LIifF/pub"));
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(click);
        }
    }

}
