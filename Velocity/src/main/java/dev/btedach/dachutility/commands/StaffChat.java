package dev.btedach.dachutility.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import dev.btedach.dachutility.utils.Constants;
import dev.btedach.dachutility.utils.messagebridge.ChatManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class StaffChat implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String args[] = invocation.arguments();
        if (source instanceof Player player) {
            String uuid = player.getUniqueId().toString();
            if(player.hasPermission("group.staff")){
                if(ChatManager.isPlayerInStaffChat(player)){
                    ChatManager.removePlayerFromStaffChat(player);
                    player.sendMessage(Component.text(Constants.prefix +"StaffChat wurde deaktiviert"));
                }else{
                    ChatManager.addPlayerToStaffChat(player);
                    if(ChatManager.isPlayerInBuilderChat(player)){
                        ChatManager.removePlayerFromBuilderChat(player);
                        player.sendMessage(Component.text(Constants.prefix+"BuilderChat wurde deaktiviert und StaffChat wurde aktiviert"));
                    }else{
                        player.sendMessage(Component.text(Constants.prefix+"StaffChat wurde aktiviert"));
                    }
                }
            }else{
                player.sendMessage(Component.text(Constants.prefix+ NamedTextColor.RED + "Du hast keine Rechte dafür!"));
            }
        } else {
            source.sendMessage(Component.text("Du musst ein Spieler sein, um diesen Befehl benutzen zu können"));
        }
    }
}
