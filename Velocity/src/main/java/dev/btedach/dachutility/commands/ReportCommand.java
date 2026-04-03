package dev.btedach.dachutility.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.btedach.dachutility.DACHUtility;
import dev.btedach.dachutility.data.ConfigReader;
import dev.btedach.dachutility.utils.chat.MessageCache;
import dev.btedach.dachutility.utils.chat.Report;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static dev.btedach.dachutility.DACHUtility.sendMessage;

public class ReportCommand implements SimpleCommand {

    private final ProxyServer proxyServer;
    private final String webhookUrl;
    private final MessageCache messageCache;

    public ReportCommand(ProxyServer proxyServer, ConfigReader configReader, MessageCache messageCache) {
        this.proxyServer = proxyServer;
        this.webhookUrl = configReader.readReportConfig().webhookUrl();
        this.messageCache = messageCache;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof Player player)) {
            sendMessage(source, Component.text("Du musst ein Spieler sein, um diesen Befehl benutzen zu können", NamedTextColor.RED));
            return;
        }

        if(args.length < 2) {
            sendMessage(player, Component.text("Falscher Syntax. /report <Spielername> <Reason...>", NamedTextColor.RED));
            return;
        }


        Optional<Player> player1 = DACHUtility.getInstance().getProxyServer().getPlayer(args[0]);
        if (player1.isEmpty()) {
            sendMessage(player, Component.text("Spieler " + args[0] + " nicht online!", NamedTextColor.RED));
            return;
        }


        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) {
                stringBuilder.append(" ");
            }
            stringBuilder.append(args[i]);
        }
        Report report = new Report(player, player1.get(), stringBuilder.toString(), this.webhookUrl, this.messageCache);
        report.submit();
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> result = new ArrayList<>();

            String[] args = invocation.arguments();

            switch (args.length) {
                case 0 -> this.proxyServer.getAllPlayers().stream()
                        .map(Player::getUsername)
                        .forEach(result::add);

                case 1 -> this.proxyServer.getAllPlayers().stream()
                        .map(Player::getUsername)
                        .filter(playerName -> playerName.toLowerCase().startsWith(args[0].toLowerCase()))
                        .forEach(result::add);
            }

            return result;
        });
    }
}
