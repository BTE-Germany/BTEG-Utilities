package de.leander.bteg_utilities;

import de.leander.bteg_utilities.commands.*;
import de.leander.bteg_utilities.events.FadeFormEvent;
import de.leander.bteg_utilities.events.PlayerJoinLeaveGamemode;
import de.leander.bteg_utilities.events.PlayerTeleport;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class BTEGUtilities extends JavaPlugin {

    public static final String PREFIX = "ᾠ§7 "; // "§b§lBTEG §7» §7"

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new PlayerJoinLeaveGamemode(), this);
        getServer().getPluginManager().registerEvents(new FadeFormEvent(), this);
        getServer().getPluginManager().registerEvents(new PlayerTeleport(), this);
        Objects.requireNonNull(getCommand("cover")).setExecutor(new CoverCommand());
        Objects.requireNonNull(getCommand("lidar")).setExecutor(new LidarCommand());
        Objects.requireNonNull(getCommand("rail")).setExecutor(new RailCommand());
        Objects.requireNonNull(getCommand("/side")).setExecutor(new Side());
        Objects.requireNonNull(getCommand("speed")).setExecutor(new Speed());
        Objects.requireNonNull(getCommand("cleanup")).setExecutor(new CleanUpCommand());
        Objects.requireNonNull(getCommand("connect")).setExecutor(new ConnectCommand());
        Objects.requireNonNull(getCommand("regionfile")).setExecutor(new RegionFileCommand());
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getComponentLogger().info(Component.text("Plugin enabled!", NamedTextColor.BLUE));
    }

    @Override
    public void onDisable() {
        getComponentLogger().info(Component.text("Plugin disabled!", NamedTextColor.BLUE));
    }


    public static BTEGUtilities getPlugin() {
        return getPlugin(BTEGUtilities.class);
    }

}
