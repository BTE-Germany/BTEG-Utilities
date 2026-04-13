package de.btegermany.utilities.events;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import de.btegermany.utilities.BTEGUtilities;
import de.btegermany.utilities.util.DndPlayersRegistry;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PluginMessageListener implements org.bukkit.plugin.messaging.PluginMessageListener {

    private final BTEGUtilities plugin;
    private final DndPlayersRegistry dndPlayersRegistry;

    public PluginMessageListener(BTEGUtilities plugin, DndPlayersRegistry dndPlayersRegistry) {
        this.plugin = plugin;
        this.dndPlayersRegistry = dndPlayersRegistry;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!channel.equals(BTEGUtilities.PLUGIN_CHANNEL)) {
            return;
        }

        ByteArrayDataInput dataInput = ByteStreams.newDataInput(message);

        switch (dataInput.readUTF()) {
            case "hide_players_for_player" -> {
                String uuidsPlayersToHideRaw = dataInput.readUTF();
                UUID playerUUID = UUID.fromString(dataInput.readUTF());
                Player playerReceived = this.plugin.getServer().getPlayer(playerUUID);
                if (playerReceived == null) {
                    return;
                }

                this.dndPlayersRegistry.register(player);

                if (uuidsPlayersToHideRaw.isEmpty()) {
                    return;
                }
                String[] uuidsPlayersToHide = uuidsPlayersToHideRaw.split(",");

                for (String uuid : uuidsPlayersToHide) {
                    Player playerToHide = this.plugin.getServer().getPlayer(UUID.fromString(uuid));
                    if (playerToHide == null) {
                        continue;
                    }

                    playerReceived.hidePlayer(this.plugin, playerToHide);
                }
            }

            case "hide_player_for_players" -> {
                UUID playerUUID = UUID.fromString(dataInput.readUTF());
                Player playerToHide = this.plugin.getServer().getPlayer(playerUUID);
                if (playerToHide == null) {
                    return;
                }
                String uuidsPlayersRaw = dataInput.readUTF();

                if (uuidsPlayersRaw.isEmpty()) {
                    return;
                }
                String[] uuidsPlayers = uuidsPlayersRaw.split(",");

                for (String uuid : uuidsPlayers) {
                    Player player1 = this.plugin.getServer().getPlayer(UUID.fromString(uuid));
                    if (player1 == null) {
                        continue;
                    }

                    player1.hidePlayer(this.plugin, playerToHide);
                }
            }

            case "show_players" -> {
                UUID playerUUID = UUID.fromString(dataInput.readUTF());
                Player playerReceived = this.plugin.getServer().getPlayer(playerUUID);
                if (playerReceived == null) {
                    return;
                }

                this.dndPlayersRegistry.unregister(player);

                for (Player playerToShow : this.plugin.getServer().getOnlinePlayers()) {
                    playerReceived.showPlayer(this.plugin, playerToShow);
                }
            }
        }
    }

}
