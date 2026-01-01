package dev.btedach.dachutility.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.btedach.dachutility.DACHUtility;
import dev.btedach.dachutility.maintenance.Maintenance;
import dev.btedach.dachutility.registry.MaintenancesRegistry;
import dev.btedach.dachutility.utils.Constants;
import dev.btedach.dachutility.utils.Servers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class MaintenanceCommand implements SimpleCommand {

    private final MaintenancesRegistry maintenancesRegistry;
    private final ProxyServer proxyServer;

    public MaintenanceCommand(MaintenancesRegistry maintenancesRegistry, ProxyServer proxyServer) {
        this.maintenancesRegistry = maintenancesRegistry;
        this.proxyServer = proxyServer;
    }

    @Override
    public void execute(Invocation invocation) {
        Player player = (Player) invocation.source();
        String[] args = invocation.arguments();

        switch (args.length) {
            case 1 -> {
                player.sendMessage(Constants.prefixComponent.append(Component.text("Nutze den Command folgendermaßen:", NamedTextColor.GOLD)));
                player.sendMessage(Constants.prefixComponent.append(Component.text("/maintenance add", NamedTextColor.GOLD)
                        .append(Component.text(" [name] [server] [date (z.B. 1.2.2034)] [time (z.B. 12:34)]", NamedTextColor.RED))));
                player.sendMessage(Constants.prefixComponent.append(Component.text("/maintenance cancel", NamedTextColor.GOLD)
                        .append(Component.text(" [name]", NamedTextColor.RED))));
            }
            case 2 -> {
                if(!args[0].equalsIgnoreCase("cancel")) {
                    //TODO: test
                    this.proxyServer.getCommandManager().executeAsync(player, "maintenance help");
                    return;
                }
                String name = args[1];
                if(!this.maintenancesRegistry.getMaintenances().containsKey(name)) {
                    player.sendMessage(Constants.prefixComponent.append(Component.text("Es gibt keine Wartungsarbeiten mit diesem Namen!", NamedTextColor.GOLD)));
                    return;
                }
                if(this.maintenancesRegistry.getMaintenances().get(name).proxy()) {
                    DACHUtility.getInstance().getServer().getCommandManager().executeAsync(DACHUtility.getInstance().getServer().getConsoleCommandSource(), "cloudnet syncproxy target Proxy maintenance false");
                }
                this.maintenancesRegistry.unregister(name);
                player.sendMessage(Constants.prefixComponent.append(Component.text(args[1], NamedTextColor.RED).append(Component.text(" wurde entfernt!", NamedTextColor.GOLD))));
            }
            case 5 -> {
                if(!args[0].equalsIgnoreCase("add")) {
                    //TODO: test
                    this.proxyServer.getCommandManager().executeAsync(player, "maintenance help");
                    return;
                }
                String name = args[1];
                if(this.maintenancesRegistry.getMaintenances().containsKey(name)) {
                    player.sendMessage(Constants.prefixComponent.append(Component.text("Es gibt bereits Wartungsarbeiten mit diesem Namen!", NamedTextColor.RED)));
                    return;
                }

                Set<RegisteredServer> servers = new HashSet<>(Servers.fromInput(args[2].split(",")).values());
                String[] date = args[3].split("\\.");
                String[] time = args[4].split(":");

                boolean proxy = servers.stream().anyMatch(Objects::isNull);
                Maintenance maintenance = new Maintenance(name, servers, LocalDateTime.of(
                        Integer.parseInt(date[2]),
                        Integer.parseInt(date[1]),
                        Integer.parseInt(date[0]),
                        Integer.parseInt(time[0]),
                        Integer.parseInt(time[1])
                ).atZone(ZoneId.of("Europe/Berlin")), proxy);
                servers.removeIf(Objects::isNull);
                this.maintenancesRegistry.register(maintenance);
                player.sendMessage(Constants.prefixComponent.append(Component.text(args[1], NamedTextColor.RED).append(Component.text(" wurde gespeichert!", NamedTextColor.GOLD))));
            }
        }
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        //TODO: tab complete
        return CompletableFuture.completedFuture(new ArrayList<>());
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("bteg.maintenance") && (invocation.source() instanceof Player);
    }
}