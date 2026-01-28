package dev.btedach.dachutility.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.btedach.dachutility.DACHUtility;
import dev.btedach.dachutility.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Optional;

import static dev.btedach.dachutility.DACHUtility.sendMessage;

public class PlotsCommand implements SimpleCommand {

    @Override
    public void execute(Invocation invocation) {
        Player player = (Player) invocation.source();

        Optional<RegisteredServer> plotServerOptional = DACHUtility.getInstance().getProxyServer().getServer("Plot-1");
        if (plotServerOptional.isEmpty()) {
            sendMessage(player, Component.text("Der Plotserver ist gerade nicht verfügbar.", NamedTextColor.RED));
            return;
        }
        RegisteredServer plotServer = plotServerOptional.get();

        Utils.connectIfOnline(player, plotServer);
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source() instanceof Player;
    }
}