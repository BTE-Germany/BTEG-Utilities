package dev.btedach.dachutility.utils.chat;

import com.velocitypowered.api.proxy.Player;

import java.util.HashMap;
import java.util.Map;

public class MessageCache {
    private static final int MESSAGE_HISTORY_SIZE = 10;

    private final Map<String, String[]> messageHistoryByPlayer = new HashMap<>();

    public void storeMessage(Player player, String message){
        if (!this.messageHistoryByPlayer.containsKey(player.getUniqueId().toString())) {
            this.messageHistoryByPlayer.put(player.getUniqueId().toString(), new String[MESSAGE_HISTORY_SIZE]);
        }
        String[] history = this.messageHistoryByPlayer.get(player.getUniqueId().toString());
        System.arraycopy(history, 0, history, 1, history.length - 1);
        history[0] = message;
    }

    public String[] getLastMessages(Player player) {
        return this.messageHistoryByPlayer.get(player.getUniqueId().toString());
    }
}
