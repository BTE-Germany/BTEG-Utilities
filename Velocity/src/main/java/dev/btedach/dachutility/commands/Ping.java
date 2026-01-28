package dev.btedach.dachutility.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import dev.btedach.dachutility.DACHUtility;
import dev.btedach.dachutility.utils.Constants;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Optional;

import static dev.btedach.dachutility.DACHUtility.sendMessage;

public class Ping implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof Player player)) {
            sendMessage(source, Component.text("Du musst ein Spieler sein, um diesen Befehl benutzen zu können", NamedTextColor.RED));
            return;
        }

        if (args.length == 0){
            sendMessage(player, Constants.ping, Component.text("Dein Ping beträgt " + player.getPing() + "ms", NamedTextColor.GRAY));
        } else {
            if (player.hasPermission("ping")){
                Optional<Player> player1 = DACHUtility.getInstance().getProxyServer().getPlayer(args[0]);
                if (player1.isEmpty()){
                    sendMessage(source, Component.text("Dieser Spieler ist nicht online!", NamedTextColor.RED));
                } else {
                    sendMessage(player, Constants.ping, Component.text(player1.get().getUsername() + "´s Ping beträgt " + player1.get().getPing() + "ms", NamedTextColor.GRAY));
                }
            } else {
                sendMessage(player, Component.text("Du hast keine Rechte dafür!", NamedTextColor.RED));
            }
        }
    }
}
