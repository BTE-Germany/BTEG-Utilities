package dev.btedach.dachutility.maintenance;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.btedach.dachutility.DACHUtility;
import dev.btedach.dachutility.utils.Constants;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class MaintenanceRunnable implements Runnable {

    private final Maintenance maintenance;

    public MaintenanceRunnable(Maintenance maintenance) {
        this.maintenance = maintenance;
    }

    @Override
    public void run() {
        if (this.maintenance.proxy()) {
            DACHUtility.getInstance().getServer().getCommandManager().executeAsync(DACHUtility.getInstance().getServer().getConsoleCommandSource(), "cloudnet syncproxy target Proxy maintenance true");
        }

        for (RegisteredServer server : this.maintenance.servers()) {
            for (Player player : server.getPlayersConnected()) {
                if (player.hasPermission("bteg.maintenance.join")) {
                    continue;
                }
                if (this.maintenance.servers().stream().anyMatch(serverMaintenance -> serverMaintenance != null && serverMaintenance.getServerInfo().getName().equals("Lobby-1"))) {
                    player.disconnect(Component.text(Constants.prefix + "Zum aktuellen Zeitpunkt finden Wartungsarbeiten statt!", NamedTextColor.GOLD));
                    continue;
                }

                Optional<RegisteredServer> lobbyServerOptional = DACHUtility.getInstance().getServer().getServer("Lobby-1");
                if (lobbyServerOptional.isEmpty()) {
                    player.disconnect(Component.text(Constants.prefix + "Zum aktuellen Zeitpunkt finden Wartungsarbeiten statt!", NamedTextColor.GOLD));
                    return;
                }
                RegisteredServer lobbyServer = lobbyServerOptional.get();

                lobbyServer.ping().orTimeout(1, TimeUnit.SECONDS)
                        .exceptionally(throwable -> {
                            player.disconnect(Component.text(Constants.prefix + "Zum aktuellen Zeitpunkt finden Wartungsarbeiten statt!", NamedTextColor.GOLD));
                            return null;
                        })
                        .thenAccept(pingResult -> {
                            if (pingResult == null) {
                                return;
                            }

                            player.sendMessage(Component.text(Constants.prefix + "Auf diesem Server finden zum aktuellen Zeitpunkt Wartungsarbeiten statt!", NamedTextColor.GOLD));

                            player.createConnectionRequest(lobbyServer).connect();
                        });
            }
        }
    }

}
