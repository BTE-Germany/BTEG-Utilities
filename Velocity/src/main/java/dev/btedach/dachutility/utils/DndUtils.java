package dev.btedach.dachutility.utils;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.proxy.Player;
import dev.btedach.dachutility.DACHUtility;
import dev.btedach.dachutility.registry.DndPlayersRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

import static dev.btedach.dachutility.DACHUtility.sendMessage;

public class DndUtils {

    public static void hideAllPlayersForPlayerExcept(Player player, Collection<UUID> exceptions) {
        player.getCurrentServer().ifPresentOrElse(serverConnection -> {
            hidePlayersForPlayerExcept(player, serverConnection.getServer().getPlayersConnected(), exceptions);
        }, () -> sendMessage(player, Component.text("Bitte erneut versuchen.", NamedTextColor.RED)));
    }

    public static void hidePlayersForPlayerExcept(Player player, Collection<Player> playersToHide, Collection<UUID> exceptions) {
        player.getCurrentServer().ifPresentOrElse(serverConnection -> {
            String playersToHideJoined = playersToHide.stream()
                    .filter(player1 -> !exceptions.contains(player1.getUniqueId()) && !player1.equals(player))
                    .map(player1 -> player1.getUniqueId().toString())
                    .collect(Collectors.joining(","));

            ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
            dataOutput.writeUTF("hide_players_for_player");
            dataOutput.writeUTF(playersToHideJoined);
            dataOutput.writeUTF(player.getUniqueId().toString());

            serverConnection.sendPluginMessage(DACHUtility.PLUGIN_CHANNEL, dataOutput.toByteArray());
        }, () -> sendMessage(player, Component.text("[dnd] Bitte erneut versuchen.", NamedTextColor.RED)));
    }

    public static void showAllPlayersForPlayer(Player player) {
        player.getCurrentServer().ifPresentOrElse(serverConnection -> {
            ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
            dataOutput.writeUTF("show_players");
            dataOutput.writeUTF(player.getUniqueId().toString());

            serverConnection.sendPluginMessage(DACHUtility.PLUGIN_CHANNEL, dataOutput.toByteArray());
        }, () -> sendMessage(player, Component.text("[dnd] Bitte erneut versuchen.", NamedTextColor.RED)));
    }

    public static void hidePlayerForDndPlayers(Player playerToHide, DndPlayersRegistry dndPlayersRegistry) {
        playerToHide.getCurrentServer().ifPresentOrElse(serverConnection -> {
            String playersJoined = serverConnection.getServer().getPlayersConnected().stream()
                    .filter(player1 -> !player1.equals(playerToHide) && dndPlayersRegistry.isRegistered(player1) && !dndPlayersRegistry.getExceptions(player1).contains(playerToHide.getUniqueId()))
                    .map(player1 -> player1.getUniqueId().toString())
                    .collect(Collectors.joining(","));

            ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
            dataOutput.writeUTF("hide_player_for_players");
            dataOutput.writeUTF(playerToHide.getUniqueId().toString());
            dataOutput.writeUTF(playersJoined);

            serverConnection.sendPluginMessage(DACHUtility.PLUGIN_CHANNEL, dataOutput.toByteArray());
        }, () -> sendMessage(playerToHide, Component.text("[dnd] Bitte erneut versuchen.", NamedTextColor.RED)));
    }

}
