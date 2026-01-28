package dev.btedach.dachutility.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.btedach.dachutility.DACHUtility;
import dev.btedach.dachutility.maintenance.Maintenance;
import dev.btedach.dachutility.registry.MaintenancesRegistry;
import dev.btedach.dachutility.utils.Servers;
import dev.btedach.dachutility.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static dev.btedach.dachutility.DACHUtility.sendMessage;

public class MaintenanceCommand implements SimpleCommand {

    private final MaintenancesRegistry maintenancesRegistry;
    private final ProxyServer proxyServer;

    public MaintenanceCommand(MaintenancesRegistry maintenancesRegistry, ProxyServer proxyServer) {
        this.maintenancesRegistry = maintenancesRegistry;
        this.proxyServer = proxyServer;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        switch (args.length) {
            case 1 -> {
                sendMessage(source, Component.text("Nutze den Command folgendermaßen:", NamedTextColor.GOLD));
                sendMessage(source, Component.text("/maintenance add", NamedTextColor.GOLD),
                        Component.text(" [name] [server] [date (z.B. 1.2.2034)] [time (z.B. 12:34)]", NamedTextColor.RED));
                sendMessage(source, Component.text("/maintenance cancel", NamedTextColor.GOLD),
                        Component.text(" [name]", NamedTextColor.RED));
            }
            case 2 -> {
                if(!args[0].equalsIgnoreCase("cancel")) {
                    this.proxyServer.getCommandManager().executeAsync(source, "maintenance help");
                    return;
                }
                String name = args[1];
                if(!this.maintenancesRegistry.getMaintenances().containsKey(name)) {
                    sendMessage(source, Component.text("Es gibt keine Wartungsarbeiten mit diesem Namen!", NamedTextColor.GOLD));
                    return;
                }
                this.maintenancesRegistry.unregister(name);
                sendMessage(source, Component.text(args[1], NamedTextColor.RED), Component.text(" wurde entfernt!", NamedTextColor.GOLD));
            }
            case 5 -> {
                if(!args[0].equalsIgnoreCase("add")) {
                    this.proxyServer.getCommandManager().executeAsync(source, "maintenance help");
                    return;
                }
                String name = args[1];
                if(this.maintenancesRegistry.getMaintenances().containsKey(name)) {
                    sendMessage(source, Component.text("Es gibt bereits Wartungsarbeiten mit diesem Namen!", NamedTextColor.RED));
                    return;
                }

                Map<String, RegisteredServer> serversInput = Servers.fromInput(args[2].split(","));
                Set<RegisteredServer> servers = new HashSet<>(serversInput.values());
                String[] date = args[3].split("\\.");
                String[] time = args[4].split(":");

                boolean proxy = serversInput.containsKey("Proxy-1");
                Maintenance maintenance = new Maintenance(name, servers, LocalDateTime.of(
                        Integer.parseInt(date[2]),
                        Integer.parseInt(date[1]),
                        Integer.parseInt(date[0]),
                        Integer.parseInt(time[0]),
                        Integer.parseInt(time[1])
                ).atZone(ZoneId.of("Europe/Berlin")), proxy);
                servers.removeIf(Objects::isNull);

                this.maintenancesRegistry.register(maintenance);
                sendMessage(source, Component.text(args[1], NamedTextColor.RED), Component.text(" wurde gespeichert!", NamedTextColor.GOLD));

                String dateFormatted = DACHUtility.convertDate(maintenance.time().getYear(), maintenance.time().getMonthValue(), maintenance.time().getDayOfMonth());
                String timeFormatted = maintenance.time().getHour() + ":" + (maintenance.time().getMinute() < 10 ? "0" : "") + maintenance.time().getMinute();
                for (RegisteredServer server : DACHUtility.getInstance().getProxyServer().getAllServers()) {
                    if (!proxy && !servers.contains(server)) {
                        continue;
                    }
                    server.getPlayersConnected().forEach(player -> {
                        sendMessage(player, Component.text("Wartungsarbeiten auf diesem Server:", NamedTextColor.GOLD), Component.text(" %s um %s, %s".formatted(dateFormatted, timeFormatted, maintenance.name()), NamedTextColor.RED));
                    });
                }
            }
        }
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> result = new ArrayList<>();

            if (!this.hasPermission(invocation)) {
                return result;
            }

            String[] args = invocation.arguments();

            String[] options1 = new String[] {"add", "cancel"};

            switch (args.length) {
                case 0 -> Collections.addAll(result, options1);

                case 1 -> {
                    for (String s : options1) {
                        if(s.startsWith(args[0].toLowerCase())) result.add(s);
                    }
                }

                case 2 -> {
                    if (!args[0].equalsIgnoreCase("cancel")) {
                        return result;
                    }

                    for (String name : this.maintenancesRegistry.getMaintenances().keySet()) {
                        if (!name.toLowerCase().startsWith(args[1].toLowerCase())) {
                            continue;
                        }
                        result.add(name);
                    }
                }

                case 3 -> {
                    if (!args[0].equalsIgnoreCase("add")) {
                        return result;
                    }

                    result.addAll(Utils.getServersTabCompletion(args[2], this.proxyServer));
                }
            }

            return result;
        });
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("bteg.maintenance");
    }
}