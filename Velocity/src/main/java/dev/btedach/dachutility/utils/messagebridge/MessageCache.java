package dev.btedach.dachutility.utils.messagebridge;

import com.velocitypowered.api.proxy.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MessageCache {
    private static final int MESSAGE_HISTORY_SIZE = 10;     // Adjust the history size as needed

    private static final Map<String, String[]> messageHistoryByPlayer = new HashMap<>();

    public static void newMessage(Player player, String newMessage){
        if (!messageHistoryByPlayer.containsKey(player.getUniqueId().toString())) {
            messageHistoryByPlayer.put(player.getUniqueId().toString(), new String[MESSAGE_HISTORY_SIZE]);
        }
        String[] history = messageHistoryByPlayer.get(player.getUniqueId().toString());
        System.arraycopy(history, 0, history, 1, history.length - 1);
        history[0] = newMessage;
    }

    public static String[] getLastMessages(Player player){
        String[] history = messageHistoryByPlayer.get(player.getUniqueId().toString());
        if(history == null){
            return null;
        }
        return new ArrayList<>(Arrays.asList(history).subList(1, history.length)).toArray(new String[0]);
    }
}
