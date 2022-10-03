package de.leander.bteggamemode;

import de.leander.bteggamemode.commands.*;
import de.leander.bteggamemode.events.BedrockTerraBlock;
import de.leander.bteggamemode.events.JoinMessage;
import de.leander.bteggamemode.events.SetGamemode;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public final class BTEGGamemode extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new SetGamemode(), this);
        getServer().getPluginManager().registerEvents(new JoinMessage(), this);
        getServer().getPluginManager().registerEvents(new BedrockTerraBlock(), this);
        getCommand("terraform").setExecutor(new Terraform(this));
        getCommand("cover").setExecutor(new CoverCommand());
        getCommand("lidar").setExecutor(new LidarCommand());
        getCommand("rail").setExecutor(new RailCommand());
        getCommand("/side").setExecutor(new Side(this));
        getCommand("speed").setExecutor(new Speed(this));
        getCommand("cleanup").setExecutor(new CleanUpCommand());
        getCommand("connect").setExecutor(new ConnectCommand());
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getConsoleSender().sendMessage(ChatColor.BLUE + "[BTEG Gamemode]: Plugin enabled!");
    }

    @Override
    public void onDisable() {
        getServer().getConsoleSender().sendMessage(ChatColor.RED + "[BTEG Gamemode]: Plugin disabled!");
    }


}
