package dev.btedach.dachutility.utils.messagebridge;

import com.velocitypowered.api.proxy.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatManager {
    static List<UUID> staffChat = new ArrayList<>();
    static List<UUID> builderChat = new ArrayList<>();

    static List<UUID> mutedChat = new ArrayList<>();

    static List<UUID> disableDCChat = new ArrayList<>();





    public static void addPlayerToStaffChat(Player player){
        staffChat.add(player.getUniqueId());
    }

    public static void removePlayerFromStaffChat(Player player){
        staffChat.remove(player.getUniqueId());
    }

    public static boolean isPlayerInStaffChat(Player player){
        return staffChat.contains(player.getUniqueId());
    }

    public static void addPlayerToBuilderChat(Player player){
        builderChat.add(player.getUniqueId());
    }

    public static void removePlayerFromBuilderChat(Player player){
        builderChat.remove(player.getUniqueId());
    }

    public static boolean isPlayerInBuilderChat(Player player){
        return builderChat.contains(player.getUniqueId());
    }

    public static void addPlayerToMutedChat(Player player){
        mutedChat.add(player.getUniqueId());
    }

    public static void removePlayerFromMutedChat(Player player){
        mutedChat.remove(player.getUniqueId());
    }

    public static boolean isPlayerInMutedChat(Player player){
        return mutedChat.contains(player.getUniqueId());
    }

    public static void addPlayerToDisableDCChat(Player player){
        disableDCChat.add(player.getUniqueId());
    }

    public static void removePlayerFromDisableDCChat(Player player){
        disableDCChat.remove(player.getUniqueId());
    }

    public static boolean isPlayerInDisableDCChat(Player player){
        return disableDCChat.contains(player.getUniqueId());
    }
}
