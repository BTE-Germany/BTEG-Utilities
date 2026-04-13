package de.btegermany.utilities;

import de.btegermany.utilities.commands.*;
import de.btegermany.utilities.events.FadeFormEvent;
import de.btegermany.utilities.events.PlayerJoinLeaveGamemode;
import de.btegermany.utilities.events.PlayerTeleport;
import de.btegermany.utilities.events.PluginMessageListener;
import de.btegermany.utilities.util.DndPlayersRegistry;
import de.btegermany.utilities.util.UtilitiesPlaceholderExpansion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class BTEGUtilities extends JavaPlugin {

    public static final String PLUGIN_CHANNEL = "bteg:utilities";
    public static final String PREFIX = "ᾠ§7 ";

    private final DndPlayersRegistry dndPlayersRegistry = new DndPlayersRegistry();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new PlayerJoinLeaveGamemode(this.dndPlayersRegistry), this);
        getServer().getPluginManager().registerEvents(new FadeFormEvent(), this);
        getServer().getPluginManager().registerEvents(new PlayerTeleport(), this);

        Objects.requireNonNull(getCommand("cover")).setExecutor(new CoverCommand());
        Objects.requireNonNull(getCommand("lidar")).setExecutor(new LidarCommand());
        Objects.requireNonNull(getCommand("rail")).setExecutor(new RailCommand());
        Objects.requireNonNull(getCommand("/side")).setExecutor(new SideCommand());
        Objects.requireNonNull(getCommand("speed")).setExecutor(new SpeedCommand());
        Objects.requireNonNull(getCommand("cleanup")).setExecutor(new CleanUpCommand());
        Objects.requireNonNull(getCommand("connect")).setExecutor(new ConnectCommand());
        Objects.requireNonNull(getCommand("regionfile")).setExecutor(new RegionFileCommand());

        this.getServer().getMessenger().registerIncomingPluginChannel(this, PLUGIN_CHANNEL, new PluginMessageListener(this, this.dndPlayersRegistry));
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new UtilitiesPlaceholderExpansion(this.dndPlayersRegistry).register();
        }

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
