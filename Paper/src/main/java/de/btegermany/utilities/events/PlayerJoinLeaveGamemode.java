package de.btegermany.utilities.events;

import de.btegermany.utilities.BTEGUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerJoinLeaveGamemode implements Listener {

    @EventHandler
    public static void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission("bteg.builder")) {
            player.sendMessage(BTEGUtilities.PREFIX + "Gamemode set to creative!");
            BTEGUtilities.getPlugin().getServer().getScheduler().runTask(BTEGUtilities.getPlugin(), () -> player.setGameMode(GameMode.CREATIVE));
        } else {
            Component message = Component.text(BTEGUtilities.PREFIX + "Gamemode set to §9spectator§7! As a \uE363 can only load already generated chunks!");
            Component hover = message.hoverEvent(Component.text("Click here to understand why").color(NamedTextColor.GREEN));
            Component click = hover.clickEvent(ClickEvent.openUrl("https://docs.google.com/document/d/e/2PACX-1vS0IGQX5lcjkf4vuIsjdHPPjvpdRfTCPOdvnu_n5udBATQKdcp4nelO3Q2eM8vTPKJ3oIlPNL5LIifF/pub"));
            BTEGUtilities.getPlugin().getServer().getScheduler().runTask(BTEGUtilities.getPlugin(), () -> player.setGameMode(GameMode.SPECTATOR));
            player.sendMessage(click);
        }
    }

    @EventHandler
    public static void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        Player player = event.getPlayer();
        player.setWalkSpeed(0.2F);
        player.setFlySpeed(0.1F);
    }


    /*
        Spieler sollen generell nicht in Gamemode Survival und Adventure gehen können - https://discord.com/channels/692825222373703772/1343974800074342532
        (Kein Plan warum alles andere nicht geht, aber das hier fixt das GM on join problem)
     */
    @EventHandler
    public static void onGMChange(@NotNull PlayerGameModeChangeEvent event) {
        if(event.getNewGameMode() == GameMode.ADVENTURE || event.getNewGameMode() == GameMode.SURVIVAL) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text(BTEGUtilities.PREFIX).append(Component.text("Gamemode reset to ")).append(Component.translatable(event.getPlayer().getGameMode().translationKey())).append(Component.text(" you can only switch between Creative and Spectator!")));
        }
    }
}
