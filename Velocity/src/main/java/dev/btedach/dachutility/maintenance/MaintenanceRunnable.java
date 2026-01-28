package dev.btedach.dachutility.maintenance;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.btedach.dachutility.DACHUtility;
import dev.btedach.dachutility.utils.Constants;
import dev.btedach.dachutility.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Collection;
import java.util.Optional;

public class MaintenanceRunnable implements Runnable {

    private final Maintenance maintenance;

    public MaintenanceRunnable(Maintenance maintenance) {
        this.maintenance = maintenance;
    }

    @Override
    public void run() {
        if (this.maintenance.proxy()) {
            this.sendToLobbyOrDisconnect(DACHUtility.getInstance().getProxyServer().getAllServers());
        }

        this.sendToLobbyOrDisconnect(this.maintenance.servers());
    }

    private void sendToLobbyOrDisconnect(Collection<RegisteredServer> servers) {
        for (RegisteredServer server : servers) {
            for (Player player : server.getPlayersConnected()) {
                if (player.hasPermission("bteg.maintenance.join")) {
                    continue;
                }
                if (servers.stream().anyMatch(serverMaintenance -> serverMaintenance != null && serverMaintenance.getServerInfo().getName().equalsIgnoreCase("Lobby-1"))) {
                    player.disconnect(Constants.prefixComponent.append(Component.text("Zum aktuellen Zeitpunkt finden Wartungsarbeiten statt!", NamedTextColor.GOLD)));
                    continue;
                }

                Optional<RegisteredServer> lobbyServerOptional = DACHUtility.getInstance().getProxyServer().getServer("Lobby-1");
                if (lobbyServerOptional.isEmpty()) {
                    player.disconnect(Constants.prefixComponent.append(Component.text("Zum aktuellen Zeitpunkt finden Wartungsarbeiten statt!", NamedTextColor.GOLD)));
                    return;
                }
                RegisteredServer lobbyServer = lobbyServerOptional.get();

                Utils.connectIfOnline(player, lobbyServer, () -> player.disconnect(Constants.prefixComponent.append(Component.text("Auf diesem Server finden zum aktuellen Zeitpunkt Wartungsarbeiten statt!", NamedTextColor.GOLD))));
            }
        }
    }

}
