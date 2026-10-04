package de.btegermany.utilities.util;

import de.btegermany.utilities.BTEGUtilities;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Stores and plays the optional success sound for custom commands. */
public final class CommandSound {
    private static final String PATH_PREFIX = "command-sound.";

    private CommandSound() { }

    public static boolean isEnabled(Player player) {
        return BTEGUtilities.getPlugin().getConfig().getBoolean(PATH_PREFIX + player.getUniqueId(), true);
    }

    public static boolean toggle(Player player) {
        boolean enabled = !isEnabled(player);
        BTEGUtilities plugin = BTEGUtilities.getPlugin();
        plugin.getConfig().set(PATH_PREFIX + player.getUniqueId(), enabled);
        plugin.saveConfig();
        return enabled;
    }

    public static void playSuccess(Player player) {
        if (isEnabled(player)) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        }
    }
}
