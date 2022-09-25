package de.leander.bteggamemode;

import de.leander.bteggamemode.commands.*;
import de.leander.bteggamemode.events.BedrockTerraBlock;
import de.leander.bteggamemode.events.JoinMessage;
import de.leander.bteggamemode.events.SetGamemode;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class BTEGGamemode extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new SetGamemode(), this);
        getServer().getPluginManager().registerEvents(new JoinMessage(), this);
        getServer().getPluginManager().registerEvents(new BedrockTerraBlock(), this);
        getCommand("terraform").setExecutor(new Terraform(this));
        getCommand("cover").setExecutor(new CoverCommand());
        getCommand("lidar").setExecutor(new LidarCommand());
        getCommand("/side").setExecutor(new Side(this));
        getCommand("speed").setExecutor(new Speed(this));
        getCommand("cleanup").setExecutor(new CleanUpCommand());
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        //create config
        this.saveDefaultConfig();
        FileConfiguration config = this.getConfig();

        //Schematic API
        String webappDirLocation = "src/main/java/de/leander/bteggamemode/util/conf/";
        Tomcat tomcat = new Tomcat();
        String webPort = config.getString("webport");
        StandardContext ctx = (StandardContext) tomcat.addWebapp("/", new File(webappDirLocation).getAbsolutePath());
        if(webPort == null || webPort.isEmpty()) {
            try {
                throw(new Exception("No port for Schematic API defined"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        assert webPort != null;
        tomcat.setPort(Integer.parseInt(webPort));
        try {
            tomcat.start();
            System.out.println("Schematic ding läuft oder so auf: " + webPort);
        } catch (LifecycleException e) {
            e.printStackTrace();
        }
        tomcat.getServer().await();



        getServer().getConsoleSender().sendMessage(ChatColor.BLUE + "[BTEG Gamemode]: Plugin enabled!");
    }

    @Override
    public void onDisable() {
        getServer().getConsoleSender().sendMessage(ChatColor.RED + "[BTEG Gamemode]: Plugin disabled!");
    }


}
