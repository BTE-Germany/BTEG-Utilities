package dev.btedach.dachutility.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
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
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        switch (args.length) {
            case 1 -> {
                source.sendMessage(Constants.prefixComponent.append(Component.text("Nutze den Command folgendermaßen:", NamedTextColor.GOLD)));
                source.sendMessage(Constants.prefixComponent.append(Component.text("/maintenance add", NamedTextColor.GOLD)
                        .append(Component.text(" [name] [server] [date (z.B. 1.2.2034)] [time (z.B. 12:34)]", NamedTextColor.RED))));
                source.sendMessage(Constants.prefixComponent.append(Component.text("/maintenance cancel", NamedTextColor.GOLD)
                        .append(Component.text(" [name]", NamedTextColor.RED))));
            }
            case 2 -> {
                if(!args[0].equalsIgnoreCase("cancel")) {
                    this.proxyServer.getCommandManager().executeAsync(source, "maintenance help");
                    return;
                }
                String name = args[1];
                if(!this.maintenancesRegistry.getMaintenances().containsKey(name)) {
                    source.sendMessage(Constants.prefixComponent.append(Component.text("Es gibt keine Wartungsarbeiten mit diesem Namen!", NamedTextColor.GOLD)));
                    return;
                }
                this.maintenancesRegistry.unregister(name);
                source.sendMessage(Constants.prefixComponent.append(Component.text(args[1], NamedTextColor.RED).append(Component.text(" wurde entfernt!", NamedTextColor.GOLD))));
            }
            case 5 -> {
                if(!args[0].equalsIgnoreCase("add")) {
                    this.proxyServer.getCommandManager().executeAsync(source, "maintenance help");
                    return;
                }
                String name = args[1];
                if(this.maintenancesRegistry.getMaintenances().containsKey(name)) {
                    source.sendMessage(Constants.prefixComponent.append(Component.text("Es gibt bereits Wartungsarbeiten mit diesem Namen!", NamedTextColor.RED)));
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
                source.sendMessage(Constants.prefixComponent.append(Component.text(args[1], NamedTextColor.RED).append(Component.text(" wurde gespeichert!", NamedTextColor.GOLD))));

                String dateFormatted = DACHUtility.convertDate(maintenance.time().getYear(), maintenance.time().getMonthValue(), maintenance.time().getDayOfMonth());
                String timeFormatted = maintenance.time().getHour() + ":" + (maintenance.time().getMinute() < 10 ? "0" : "") + maintenance.time().getMinute();
                for (RegisteredServer server : DACHUtility.getInstance().getServer().getAllServers()) {
                    if (!proxy && !servers.contains(server)) {
                        continue;
                    }
                    server.getPlayersConnected().forEach(player -> {
                        player.sendMessage(Constants.prefixComponent.append(Component.text("Wartungsarbeiten auf diesem Server:", NamedTextColor.GOLD).append(Component.text(" %s um %s, %s".formatted(dateFormatted, timeFormatted, maintenance.name()), NamedTextColor.RED))));
                    });
                }
            }
        }
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> res = new ArrayList<>();

            if (!invocation.source().hasPermission("bteg.maintenance")) {
                return res;
            }

            String[] args = invocation.arguments();

            String[] options1 = new String[] {"add", "cancel"};

            switch (args.length) {
                case 0 -> Collections.addAll(res, options1);

                case 1 -> {
                    for (String s : options1) {
                        if(s.startsWith(args[0].toLowerCase())) res.add(s);
                    }
                }

                case 2 -> {
                    if (!args[0].equalsIgnoreCase("cancel")) {
                        return res;
                    }

                    for (String name : this.maintenancesRegistry.getMaintenances().keySet()) {
                        if (!name.toLowerCase().startsWith(args[1].toLowerCase())) {
                            continue;
                        }
                        res.add(name);
                    }
                }
            }

            return res;
        });
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("bteg.maintenance");
    }
}