package dev.btedach.dachutility.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.btedach.dachutility.registry.DndPlayersRegistry;
import dev.btedach.dachutility.utils.DndUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static dev.btedach.dachutility.DACHUtility.sendMessage;

public class DndCommand implements SimpleCommand {

    private static final int FIRST_ARG_INDEX_EXCEPTIONS = 1;

    private final ProxyServer proxyServer;
    private final DndPlayersRegistry dndPlayersRegistry;

    public DndCommand(ProxyServer proxyServer, DndPlayersRegistry dndPlayersRegistry) {
        this.proxyServer = proxyServer;
        this.dndPlayersRegistry = dndPlayersRegistry;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            sendMessage(invocation.source(), Component.text("Dieser Befehl kann nur von einem Spieler ausgeführt werden.", NamedTextColor.RED));
            return;
        }

        String[] args = invocation.arguments();

        Runnable sendHelp = () -> {
            sendMessage(player, Component.text("Usage:", NamedTextColor.GOLD));
            sendMessage(player, Component.text("/dnd enable (optional: [Exception 1] [Exception ...])", NamedTextColor.GOLD));
            sendMessage(player, Component.text("/dnd disable", NamedTextColor.GOLD));
        };

        switch (args.length) {
            case 0 -> {
                sendHelp.run();
                return;
            }

            case 1 -> {
                switch (args[0].toLowerCase()) {
                    case "enable" -> {
                        this.dndPlayersRegistry.register(player, Collections.emptySet());
                        DndUtils.hideAllPlayersForPlayerExcept(player, Collections.emptySet());
                        sendMessage(player, Component.text("Dnd enabled.", NamedTextColor.GOLD));
                        return;
                    }
                    case "disable" -> {
                        this.dndPlayersRegistry.unregister(player);
                        DndUtils.showAllPlayersForPlayer(player);
                        sendMessage(player, Component.text("Dnd disabled.", NamedTextColor.GOLD));
                        return;
                    }
                }
            }

            default -> {
                if (args[0].equalsIgnoreCase("enable")) {
                    Set<UUID> exceptions = new HashSet<>();
                    for (int i = FIRST_ARG_INDEX_EXCEPTIONS; i < args.length; i++) {
                        Optional<Player> exceptionPlayerOptional = this.proxyServer.getPlayer(args[i]);
                        if (exceptionPlayerOptional.isEmpty()) {
                            sendMessage(player, Component.text("Player %s not found. Dnd is not enabled".formatted(args[i]), NamedTextColor.RED));
                            return;
                        }
                        exceptions.add(exceptionPlayerOptional.get().getUniqueId());
                    }

                    this.dndPlayersRegistry.register(player, exceptions);
                    DndUtils.hideAllPlayersForPlayerExcept(player, exceptions);
                    sendMessage(player, Component.text("Dnd enabled.", NamedTextColor.GOLD));
                    return;
                }
            }
        }

        sendHelp.run();
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> result = new ArrayList<>();

            if (!this.hasPermission(invocation)) {
                return result;
            }

            String[] args = invocation.arguments();

            String[] options1 = new String[] {"enable", "disable"};

            return switch (args.length) {
                case 0 -> List.of(options1);

                case 1 -> {
                    for (String option : options1) {
                        if (option.toLowerCase().startsWith(args[0].toLowerCase())) {
                            result.add(option);
                        }
                    }
                    yield result;
                }

                default -> this.proxyServer.getAllPlayers().stream()
                            .map(Player::getUsername)
                            .filter(playerName -> playerName.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                            .toList();
            };
        });
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("bteg.builder");
    }

}
