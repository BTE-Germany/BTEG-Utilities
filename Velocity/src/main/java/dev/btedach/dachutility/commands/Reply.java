package dev.btedach.dachutility.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class Reply implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String args[] = invocation.arguments();

        if(source instanceof Player player){
            if(args.length >= 1){
                StringBuilder stringBuilder = new StringBuilder();
                for(String s : args){
                    stringBuilder.append(s).append(" ");
                }
                if(MSG.privateMessage.containsKey(player)){
                    Player player1 = MSG.privateMessage.get(player);
                    player1.sendMessage(Component.text(NamedTextColor.RED+"["+player.getUsername()+" -> "+ player1.getUsername() + "] " + NamedTextColor.GRAY + stringBuilder));
                    player.sendMessage(Component.text(NamedTextColor.RED+"["+player.getUsername()+" -> "+ player1.getUsername()+ "] " + NamedTextColor.GRAY + stringBuilder));
                /*}else if(MSGCMD.privateMessage.containsValue(player)){
                    for(ProxiedPlayer proxiedPlayer : MSGCMD.privateMessage.keySet()){
                        if(MSGCMD.privateMessage.get(proxiedPlayer).equals(player)){
                            player.sendMessage(new TextComponent(ChatColor.RED+"["+player.getDisplayName()+" -> "+ proxiedPlayer.getDisplayName() + "] " + ChatColor.GRAY + stringBuilder));
                            proxiedPlayer.sendMessage(new TextComponent(ChatColor.RED+"["+player.getDisplayName()+" -> "+ proxiedPlayer.getDisplayName() + "] " + ChatColor.GRAY + stringBuilder));
                            break;
                        }
                    }
                */}else{
                    player.sendMessage(Component.text("Du hast keinen Privatchat offen!"));
                }
            }else{
                source.sendMessage(Component.text(NamedTextColor.RED+"Falscher Syntax! /msg <Player> <Nachricht> <...>"));
            }
        }else{
            source.sendMessage(Component.text("Du musst ein Spieler sein, um diesen Befehl benutzen zu können"));
        }
    }
}
