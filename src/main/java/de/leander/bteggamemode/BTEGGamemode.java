package de.leander.bteggamemode;

import de.leander.bteggamemode.commands.GuessGuessing;
import de.leander.bteggamemode.commands.Speed;
import de.leander.bteggamemode.commands.Terraform;
import de.leander.bteggamemode.events.BedrockTerraBlock;
import de.leander.bteggamemode.events.JoinMessage;
import de.leander.bteggamemode.events.SetGamemode;
import de.leander.bteggamemode.commands.RestartTimer;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public final class BTEGGamemode extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new SetGamemode(), this);
        getServer().getPluginManager().registerEvents(new JoinMessage(), this);
        getServer().getPluginManager().registerEvents(new BedrockTerraBlock(), this);
        getCommand("bteg").setExecutor(new RestartTimer(this));
        getCommand("terraform").setExecutor(new Terraform(this));
        getCommand("speed").setExecutor(new Speed(this));
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getConsoleSender().sendMessage(ChatColor.BLUE + "[BTEG Gamemode]: Plugin enabled!");
    }

    @Override
    public void onDisable() {
        getServer().getConsoleSender().sendMessage(ChatColor.RED + "[BTEG Gamemode]: Plugin disabled!");
    }
}
