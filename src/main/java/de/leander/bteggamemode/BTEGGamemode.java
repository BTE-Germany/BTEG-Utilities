package de.leander.bteggamemode;

import de.leander.bteggamemode.commands.*;
import de.leander.bteggamemode.events.FadeFormEvent;
import de.leander.bteggamemode.events.JoinMessage;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public final class BTEGGamemode extends JavaPlugin {

    public static final String PREFIX = "ᾠ§7 "; // "§b§lBTEG §7» §7"

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(new JoinMessage(), this);
        this.getServer().getPluginManager().registerEvents(new FadeFormEvent(), this);
        //this.getServer().getPluginManager().registerEvents(new BedrockTerraBlock(), this); Bedrock can join on 1.18+
        this.getCommand("terraform").setExecutor(new Terraform());
        this.getCommand("cover").setExecutor(new CoverCommand());
        this.getCommand("lidar").setExecutor(new LidarCommand());
        this.getCommand("rail").setExecutor(new RailCommand());
        this.getCommand("/side").setExecutor(new Side());
        this.getCommand("speed").setExecutor(new Speed());
        this.getCommand("cleanup").setExecutor(new CleanUpCommand());
        this.getCommand("connect").setExecutor(new ConnectCommand());
        this.getCommand("regionfile").setExecutor(new RegionFileCommand());
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        this.getServer().getConsoleSender().sendMessage(ChatColor.BLUE + "[BTEG Gamemode]: Plugin enabled!");
    }

    @Override
    public void onDisable() {
        this.getServer().getConsoleSender().sendMessage(ChatColor.RED + "[BTEG Gamemode]: Plugin disabled!");
    }


    public static BTEGGamemode getPlugin() {
        return getPlugin(BTEGGamemode.class);
    }

}
