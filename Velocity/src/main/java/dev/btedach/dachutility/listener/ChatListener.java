package dev.btedach.dachutility.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import dev.btedach.dachutility.utils.chat.MessageCache;

public class ChatListener {

    private final MessageCache messageCache;

    public ChatListener(MessageCache messageCache) {
        this.messageCache = messageCache;
    }

    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
        this.messageCache.storeMessage(event.getPlayer(), event.getMessage());
    }

}
