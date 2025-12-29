package dev.btedach.dachutility.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import dev.btedach.dachutility.utils.Constants;
import dev.btedach.dachutility.utils.messagebridge.ChatManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ToggelDcChat implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String args[] = invocation.arguments();

        if (source instanceof Player player) {
            if(player.hasPermission("group.staff")){
                if(ChatManager.isPlayerInDisableDCChat(player)){
                    ChatManager.removePlayerFromDisableDCChat(player);
                    player.sendMessage(Component.text(Constants.prefix +"Deine Nachrichten werden nun wieder in Discord angezeigt."));
                }else{
                    ChatManager.addPlayerToDisableDCChat(player);
                    player.sendMessage(Component.text(Constants.prefix+"Deine Nachrichten werden nicht mehr in Discord angezeigt."));
                }
            }else{
                player.sendMessage(Component.text(Constants.prefix+ NamedTextColor.RED + "Du hast keine Rechte dafür!"));
            }
        } else {
            source.sendMessage(Component.text("Du musst ein Spieler sein, um diesen Befehl benutzen zu können"));
        }
    }
}
