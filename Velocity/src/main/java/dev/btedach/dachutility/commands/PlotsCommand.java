package dev.btedach.dachutility.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.btedach.dachutility.DACHUtility;
import dev.btedach.dachutility.utils.Constants;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class PlotsCommand implements SimpleCommand {

    @Override
    public void execute(Invocation invocation) {
        Player player = (Player) invocation.source();

        Optional<RegisteredServer> plotServerOptional = DACHUtility.getInstance().getProxy().getServer("Plot-1");
        if (plotServerOptional.isEmpty()) {
            player.sendMessage(Constants.prefixComponent.append(Component.text("Der Plotserver ist gerade nicht verfügbar.", NamedTextColor.RED)));
            return;
        }
        RegisteredServer plotServer = plotServerOptional.get();

        plotServer.ping().orTimeout(1, TimeUnit.SECONDS)
                .exceptionally(throwable -> {
                    player.sendMessage(Constants.prefixComponent.append(Component.text("Server " + plotServer.getServerInfo().getName() + " is offline.", NamedTextColor.RED)));
                    return null;
                })
                .thenAccept(pingResult -> {
                    if (pingResult == null) {
                        return;
                    }

                    player.sendMessage(Constants.prefixComponent.append(Component.text("Verbinde zum Plotserver.", NamedTextColor.GOLD)));

                    player.createConnectionRequest(plotServer).connect();
                });
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source() instanceof Player;
    }
}