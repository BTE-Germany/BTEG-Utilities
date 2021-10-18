package de.leander.bteggamemode;

import de.leander.bteggamemode.events.SetGamemode;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public final class BTEGGamemode extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new SetGamemode(), this);
        getServer().getConsoleSender().sendMessage(ChatColor.BLUE + "[BTEG Gamemode]: Plugin enabled!");
    }

    @Override
    public void onDisable() {
        getServer().getConsoleSender().sendMessage(ChatColor.RED + "[BTEG Gamemode]: Plugin disabled!");
    }
}
