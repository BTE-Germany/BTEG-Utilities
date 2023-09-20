package de.leander.bteggamemode.events;

import de.leander.bteggamemode.BTEGGamemode;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.configuration.PlaceholderAPIConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Objects;

public class JoinMessage implements Listener {

    @EventHandler
    public static void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        event.joinMessage(Component.text(BTEGGamemode.prefix).append(player.displayName().append(Component.text(" switched to ")).append(Component.text(PlaceholderAPI.setPlaceholders(player, "%server_name%")).color(NamedTextColor.GREEN)).color(NamedTextColor.GRAY)));

        if (player.hasPermission("bteg.builder")) {
            player.setGameMode(GameMode.CREATIVE);
            player.sendMessage(BTEGGamemode.prefix + "Gamemode set to creative!"+player.getGameMode().name());
        }
        else{
            Component message = Component.text(BTEGGamemode.prefix + "Gamemode set to §9spectator§7! As a \uE363 can only load already generated chunks!");
            Component hover = message.hoverEvent(Component.text("Click here to understand why").color(NamedTextColor.GREEN));
            Component click = hover.clickEvent(ClickEvent.openUrl("https://docs.google.com/document/d/e/2PACX-1vS0IGQX5lcjkf4vuIsjdHPPjvpdRfTCPOdvnu_n5udBATQKdcp4nelO3Q2eM8vTPKJ3oIlPNL5LIifF/pub"));
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(click);
        }
    }

    @EventHandler
    public static void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        event.setQuitMessage(BTEGGamemode.prefix + player.getDisplayName()+ " switched to another server");
        player.setWalkSpeed(0.2F);
        player.setFlySpeed(0.1F);
    }


    /*
           Kein Plan warum alles andere nicht geht, aber das hier fixt das GM on join problem:
     */
    @EventHandler
    public static void onGMChange(PlayerGameModeChangeEvent event) {
        if(event.getNewGameMode() == GameMode.ADVENTURE || event.getNewGameMode() == GameMode.SURVIVAL) {
            event.setCancelled(true);
        }

    }


}
