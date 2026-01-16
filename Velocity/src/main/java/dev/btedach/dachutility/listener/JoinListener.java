package dev.btedach.dachutility.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import dev.btedach.dachutility.DACHUtility;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class JoinListener {

    public static DACHUtility instance = DACHUtility.getInstance();

    @Subscribe
    public void onLoginEvent(LoginEvent event) {
        DACHUtility.getInstance().getServer().getAllPlayers().forEach(player -> player.sendMessage(Component.text("֍ "+ NamedTextColor.GRAY +event.getPlayer().getUsername()).toBuilder().build()));
        instance.playerCount++;
    }
}
