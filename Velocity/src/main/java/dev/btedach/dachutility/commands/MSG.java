package dev.btedach.dachutility.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import dev.btedach.dachutility.DACHUtility;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.platform.Platform;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

public class MSG implements SimpleCommand {

    public static HashMap<Player, Player> privateMessage = new HashMap<>();

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String args[] = invocation.arguments();

        if(source instanceof Player player){
            if(args.length >= 2){
                Optional<Player> player1;

                if((player1 = DACHUtility.getInstance().getServer().getPlayer(args[0])).isPresent()){
                    String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
                    StringBuilder stringBuilder = new StringBuilder();
                    for(String s : newArgs){
                        stringBuilder.append(s).append(" ");
                    }
                    privateMessage.remove(player1);
                    privateMessage.remove(player1);
                    privateMessage.put(player, player1.get());
                    privateMessage.put(player1.get(), player);
                    player.sendMessage(Component.text(NamedTextColor.RED +"["+player.getUsername()+" -> "+ player1.get().getUsername() + "] " + NamedTextColor.GRAY + stringBuilder));
                    player1.get().sendMessage(Component.text(NamedTextColor.RED +"["+player.getUsername()+" -> "+ player1.get().getUsername() + "] " + NamedTextColor.GRAY + stringBuilder));
                }else{
                    player.sendMessage(Component.text("Spieler " + args[0] +" ist nicht online!"));
                }
            }else{
                player.sendMessage(Component.text("Falscher Syntax! /msg <Player> <Nachricht> <...>"));
            }
        }else{
            source.sendMessage(Component.text("Du musst ein Spieler sein, um diesen Befehl benutzen zu können"));
        }
    }
}
