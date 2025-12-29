package dev.btedach.dachutility.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import dev.btedach.dachutility.utils.Constants;
import dev.btedach.dachutility.utils.messagebridge.ChatManager;
import net.kyori.adventure.text.Component;

public class ToggelChat implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String args[] = invocation.arguments();

        if (source instanceof Player player) {
            if(ChatManager.isPlayerInMutedChat(player)){
                ChatManager.removePlayerFromMutedChat(player);
                player.sendMessage(Component.text(Constants.prefix +"Der normale Chat wurde wieder aktiviert"));
            }else{
                ChatManager.addPlayerToMutedChat(player);
                player.sendMessage(Component.text(Constants.prefix+"Der normale Chat wurde deaktiviert"));
            }
        } else {
            source.sendMessage(Component.text("Du musst ein Spieler sein, um diesen Befehl benutzen zu können"));
        }
    }
}
