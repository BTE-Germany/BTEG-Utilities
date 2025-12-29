package dev.btedach.dachutility.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import dev.btedach.dachutility.DACHUtility;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Optional;

public class CheckPerm implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String args[] = invocation.arguments();
        if(source.hasPermission("checkperm")){
            if(args.length == 2){
                Optional<Player> player = DACHUtility.getInstance().getServer().getPlayer(args[0]);
                if(!player.isPresent()){
                    source.sendMessage(Component.text(NamedTextColor.RED +"Dieser Spieler ist nicht online!"));
                }else{
                    if(player.get().hasPermission(args[1])){
                        source.sendMessage(Component.text(NamedTextColor.GREEN+args[0]+" hat die Berechtigung "+args[1]));
                    }else{
                        source.sendMessage(Component.text(NamedTextColor.RED+args[0]+" hat die Berechtigung "+args[1]));
                    }
                }
            }else{
                source.sendMessage(Component.text(NamedTextColor.RED+"Falscher Syntax! /checkperm <username> <permission>"));
            }
        }else{
            source.sendMessage(Component.text(NamedTextColor.RED+"Du hast dafür keine Rechte"));
        }
    }
}
