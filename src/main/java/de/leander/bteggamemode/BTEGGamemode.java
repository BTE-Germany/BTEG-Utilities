package de.leander.bteggamemode;

import de.leander.bteggamemode.commands.*;
import de.leander.bteggamemode.events.FadeFormEvent;
import de.leander.bteggamemode.events.JoinMessage;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public final class BTEGGamemode extends JavaPlugin {

    public static final String prefix = "ᾠ§7 "; // "§b§lBTEG §7» §7"

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new JoinMessage(), this);
        getServer().getPluginManager().registerEvents(new FadeFormEvent(), this);
        //getServer().getPluginManager().registerEvents(new BedrockTerraBlock(), this); Bedrock can join on 1.18+
        getCommand("terraform").setExecutor(new Terraform(this));
        getCommand("cover").setExecutor(new CoverCommand());
        getCommand("lidar").setExecutor(new LidarCommand());
        getCommand("rail").setExecutor(new RailCommand());
        getCommand("/side").setExecutor(new Side(this));
        getCommand("speed").setExecutor(new Speed(this));
        getCommand("cleanup").setExecutor(new CleanUpCommand());
        getCommand("connect").setExecutor(new ConnectCommand());
        getCommand("regionfile").setExecutor(new RegionFileCommand());
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getConsoleSender().sendMessage(ChatColor.BLUE + "[BTEG Gamemode]: Plugin enabled!");
    }

    @Override
    public void onDisable() {
        getServer().getConsoleSender().sendMessage(ChatColor.RED + "[BTEG Gamemode]: Plugin disabled!");
    }


}
