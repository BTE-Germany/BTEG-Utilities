package dev.btedach.dachutility.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.btedach.dachutility.DACHUtility;
import dev.btedach.dachutility.maintenance.Maintenance;
import dev.btedach.dachutility.registry.MaintenancesRegistry;
import dev.btedach.dachutility.utils.Constants;
import dev.btedach.dachutility.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static dev.btedach.dachutility.DACHUtility.sendMessage;

public class ChangeServerListener {
    public static DACHUtility instance = DACHUtility.getInstance();

    public static ArrayList<UUID> playerSessionCache = new ArrayList<UUID>();
    private final MaintenancesRegistry maintenancesRegistry;

    public ChangeServerListener(MaintenancesRegistry maintenancesRegistry) {
        this.maintenancesRegistry = maintenancesRegistry;
    }

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        Player player = event.getPlayer();
        RegisteredServer previousServer = event.getPreviousServer();
        RegisteredServer targetServer = event.getOriginalServer();

        // maintenance
        if (!player.hasPermission("bteg.maintenance.join")) {
            for (Maintenance maintenance : this.maintenancesRegistry.getMaintenances().values()) {
                if (!maintenance.proxy() && maintenance.servers().stream().noneMatch(targetServer::equals)) {
                    continue;
                }

                ZonedDateTime now = LocalDateTime.now(ZoneId.of("Europe/Berlin")).atZone(ZoneId.of("Europe/Berlin"));
                if (now.isAfter(maintenance.time())) {
                    event.setResult(ServerPreConnectEvent.ServerResult.denied());

                    if (maintenance.proxy() || maintenance.servers().stream().anyMatch(serverMaintenance -> serverMaintenance != null && serverMaintenance.getServerInfo().getName().equalsIgnoreCase("Lobby-1"))) {
                        player.disconnect(Constants.prefixComponent.append(Component.text("Zum aktuellen Zeitpunkt finden Wartungsarbeiten statt!", NamedTextColor.GOLD)));
                        return;
                    }

                    Optional<RegisteredServer> lobbyServerOptional = DACHUtility.getInstance().getProxyServer().getServer("Lobby-1");
                    if (lobbyServerOptional.isEmpty()) {
                        player.disconnect(Constants.prefixComponent.append(Component.text("Zum aktuellen Zeitpunkt finden Wartungsarbeiten statt!", NamedTextColor.GOLD)));
                        return;
                    }
                    RegisteredServer lobbyServer = lobbyServerOptional.get();

                    // stay in lobby when player is currently in the lobby
                    if (previousServer != null && previousServer.equals(lobbyServer)) {
                        sendMessage(player, Component.text("Auf diesem Server finden zum aktuellen Zeitpunkt Wartungsarbeiten statt!", NamedTextColor.GOLD));
                        return;
                    }

                    Utils.connectIfOnline(player, lobbyServer, () -> player.disconnect(Constants.prefixComponent.append(Component.text("Auf diesem Server finden zum aktuellen Zeitpunkt Wartungsarbeiten statt!", NamedTextColor.GOLD))));
                    return;
                }
            }
        }

        for (Maintenance maintenance : this.maintenancesRegistry.getMaintenances().values()) {
            if (!maintenance.proxy() && maintenance.servers().stream().noneMatch(server -> server != null && server.equals(targetServer))) {
                continue;
            }
            String date = DACHUtility.convertDate(maintenance.time().getYear(), maintenance.time().getMonthValue(), maintenance.time().getDayOfMonth());
            String time = maintenance.time().getHour() + ":" + (maintenance.time().getMinute() < 10 ? "0" : "") + maintenance.time().getMinute();
            // delay so the (many) other messages sent on join don't hide it
            instance.getProxyServer().getScheduler()
                    .buildTask(instance, () -> sendMessage(event.getPlayer(), Component.text("Wartungsarbeiten auf diesem Server:", NamedTextColor.GOLD), Component.text(" %s um %s, %s".formatted(date, time, maintenance.name()), NamedTextColor.RED)))
                    .delay(1500, TimeUnit.MILLISECONDS)
                    .schedule();
        }

        // third party notice
        if(targetServer.getServerInfo().getName().startsWith("terra")){
            if(!playerSessionCache.contains(event.getPlayer().getUniqueId())){
                playerSessionCache.add(event.getPlayer().getUniqueId());

                TextComponent textComponent = Component.text("Unser Server nutzt Daten von Drittanbietern. Die entsprechenden Lizenzen findest du unter https://buildthe.earth/credits")
                        .color(NamedTextColor.GREEN).hoverEvent(HoverEvent.showText(Component.text("Klicke hier zum öffnen!")))
                        .clickEvent(ClickEvent.openUrl("https://buildthe.earth/credits"))
                        .toBuilder().build();

                sendMessage(event.getPlayer(), textComponent);
            }
        }
    }
}
