package dev.btedach.dachutility.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import dev.btedach.dachutility.DACHUtility;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.UUID;

public class DisconnectListener {
    public static DACHUtility instance = DACHUtility.getInstance();

    @Subscribe
    public void onPlayerChat(DisconnectEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        String name = event.getPlayer().getUsername();

        DACHUtility.getInstance().getServer().getAllPlayers().forEach(player -> player.sendMessage(Component.text("╜ "+ NamedTextColor.GRAY+name)));

        ChangeServerListener.playerSessionCache.remove(UUID.fromString(uuid));

        if(instance.playerCount>0){
            instance.playerCount--;
        }
    }
}
