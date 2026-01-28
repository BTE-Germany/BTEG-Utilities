package dev.btedach.dachutility.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import dev.btedach.dachutility.DACHUtility;
import dev.btedach.dachutility.utils.messagebridge.ReportUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Arrays;
import java.util.Optional;

import static dev.btedach.dachutility.DACHUtility.sendMessage;

public class Report implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String args[] = invocation.arguments();

        if(source instanceof Player player){
            if(args.length > 1){
                Optional<Player> player1;
                if((player1 = DACHUtility.getInstance().getProxyServer().getPlayer(args[0])).isPresent()){
                    String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
                    StringBuilder stringBuilder = new StringBuilder();
                    for(String s : newArgs){
                        stringBuilder.append(s);
                    }
                    ReportUtil reportUtil = new ReportUtil(player, player1.get(), stringBuilder.toString());
                    reportUtil.report();
                }else{
                    sendMessage(player, Component.text("Spieler " +args[0]+ " nicht online!", NamedTextColor.RED));
                }
            }else{
                sendMessage(player, Component.text("Falscher Syntax. /report <Spielername> <Reason...>", NamedTextColor.RED));
            }
        }else{
            sendMessage(source, Component.text("Du musst ein Spieler sein, um diesen Befehl benutzen zu können", NamedTextColor.RED));
        }
    }
}
