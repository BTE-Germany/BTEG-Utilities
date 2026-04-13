package de.btegermany.utilities.util;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UtilitiesPlaceholderExpansion extends PlaceholderExpansion {

    private final DndPlayersRegistry dndPlayersRegistry;

    public UtilitiesPlaceholderExpansion(DndPlayersRegistry dndPlayersRegistry) {
        this.dndPlayersRegistry = dndPlayersRegistry;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "bteg-utilities";
    }

    @Override
    public @NotNull String getAuthor() {
        return "BTE Germany Utilities authors";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return null;
        }

        if (params.equalsIgnoreCase("dnd")) {
            return this.dndPlayersRegistry.isRegistered(player) ? "yes" : "no";
        }
        return null;
    }

}
