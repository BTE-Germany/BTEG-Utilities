package dev.btedach.dachutility.registry;

import com.velocitypowered.api.proxy.Player;

import java.util.*;

public class DndPlayersRegistry {

    private final Map<UUID, Set<UUID>> playersExceptions = new HashMap<>();

    public void register(Player player, Set<UUID> exceptions) {
        this.playersExceptions.put(player.getUniqueId(), exceptions);
    }

    public void unregister(Player player) {
        this.playersExceptions.remove(player.getUniqueId());
    }

    public boolean isRegistered(Player player) {
        return this.playersExceptions.containsKey(player.getUniqueId());
    }

    public Set<UUID> getExceptions(Player player) {
        return this.playersExceptions.get(player.getUniqueId());
    }

}
