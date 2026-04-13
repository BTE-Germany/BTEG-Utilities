package de.btegermany.utilities.util;

import org.bukkit.entity.Player;

import java.util.*;

public class DndPlayersRegistry {

    private final Set<UUID> playersWithDndEnabled = new HashSet<>();

    public void register(Player player) {
        this.playersWithDndEnabled.add(player.getUniqueId());
    }

    public void unregister(Player player) {
        this.playersWithDndEnabled.remove(player.getUniqueId());
    }

    public boolean isRegistered(Player player) {
        return this.playersWithDndEnabled.contains(player.getUniqueId());
    }

}
