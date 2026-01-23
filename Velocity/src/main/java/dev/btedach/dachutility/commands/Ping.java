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

        if (source instanceof Player player) {
            if(args.length == 0){
                sendMessage(player, Component.text(Constants.ping +"Dein Ping beträgt " + player.getPing() + "ms"));
            }else{
                if(player.hasPermission("ping")){
                    Optional<Player> player1 = DACHUtility.getInstance().getProxy().getPlayer(args[0]);
                    if(player1.isEmpty()){
                        sendMessage(source, Component.text(NamedTextColor.RED +"Dieser Spieler ist nicht online!"));
                    }else {
                        sendMessage(player, Component.text(Constants.ping + player1.get().getUsername()+"´s Ping beträgt " + player1.get().getPing() + "ms"));
                    }
                }else{
                    sendMessage(player, Component.text(Constants.prefix+ NamedTextColor.RED +"Du hast keine Rechte dafür!"));
                }
            }
        } else {
            sendMessage(source, Component.text("Du musst ein Spieler sein, um diesen Befehl benutzen zu können"));
        }

    }
}
