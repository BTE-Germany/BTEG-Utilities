package de.leander.bteggamemode;

import de.leander.bteggamemode.commands.*;
import de.leander.bteggamemode.events.FadeFormEvent;
import de.leander.bteggamemode.events.JoinMessage;
import de.leander.bteggamemode.events.PlayerTeleport;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public final class BTEGGamemode extends JavaPlugin {

    public static final String PREFIX = "ᾠ§7 "; // "§b§lBTEG §7» §7"

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new JoinMessage(), this);
        getServer().getPluginManager().registerEvents(new FadeFormEvent(), this);
        getServer().getPluginManager().registerEvents(new PlayerTeleport(), this);
        //getServer().getPluginManager().registerEvents(new BedrockTerraBlock(), this); Bedrock can join on 1.18+
        getCommand("terraform").setExecutor(new Terraform());
        getCommand("cover").setExecutor(new CoverCommand());
        getCommand("lidar").setExecutor(new LidarCommand());
        getCommand("rail").setExecutor(new RailCommand());
        getCommand("/side").setExecutor(new Side());
        getCommand("speed").setExecutor(new Speed());
        getCommand("cleanup").setExecutor(new CleanUpCommand());
        getCommand("connect").setExecutor(new ConnectCommand());
        getCommand("regionfile").setExecutor(new RegionFileCommand());
        getCommand("norms").setExecutor(new NormsCommand());
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getConsoleSender().sendMessage(ChatColor.BLUE + "[BTEG Gamemode]: Plugin enabled!");
    }

    @Override
    public void onDisable() {
        getServer().getConsoleSender().sendMessage(ChatColor.RED + "[BTEG Gamemode]: Plugin disabled!");
    }


    public static BTEGGamemode getPlugin() {
        return getPlugin(BTEGGamemode.class);
    }

}
