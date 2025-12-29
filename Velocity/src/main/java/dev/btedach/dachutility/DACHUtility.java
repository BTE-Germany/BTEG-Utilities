package dev.btedach.dachutility;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.btedach.dachutility.commands.*;
import dev.btedach.dachutility.implementation.LuckPermsAPI;
import dev.btedach.dachutility.listener.ChangeServerListener;
import dev.btedach.dachutility.listener.DisconnectListener;
import dev.btedach.dachutility.listener.JoinListener;
import dev.btedach.dachutility.utils.FileManager;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.slf4j.Logger;

import java.io.IOException;

@Plugin(id = "dach-utility", name = "DACH-Utility", version = "1.0.0-SNAPSHOT", description = "Proxy plugin for BTEG X BTE Alps", url = "https://buildthe.earth/dach", authors = {"Dev Team of BTEG and BTE Alps"})
public class DACHUtility {

    @Getter
    @Inject
    private Logger logger;
    @Getter
    private ProxyServer server;

    @Getter
    public static DACHUtility instance;

    public JDA jda;

    @Getter
    public long mainServerID;

    @Getter
    public long allChannelID;

    @Getter
    public long builderChatID;
    public int  playerCount;

    @Getter
    private FileManager fileManager;

    @Getter
    private LuckPermsAPI luckPermsHook;

    private LuckPerms luckPerms;


    @Inject
    public DACHUtility(ProxyServer server, Logger logger, CommandManager commandManager) throws IOException {
        this.server = server;
        this.logger = logger;
        instance = this;
        logger.info("Starting DACH Utility");
        this.fileManager = new FileManager();

        getFileManager().checkConfigFiles();

        try {
            getLogger().info("Try to load Channel and Server ID´s");
            mainServerID = Long.parseLong(String.valueOf(getFileManager().fetchStringFromConfig(FileManager.FILETYPE.CONFIG, "mainServerID")));
            allChannelID = Long.parseLong(String.valueOf(getFileManager().fetchStringFromConfig(FileManager.FILETYPE.CONFIG, "allChannelID")));
            builderChatID = Long.parseLong(String.valueOf(getFileManager().fetchStringFromConfig(FileManager.FILETYPE.CONFIG, "builderChatID")));
            getLogger().info("Successfully loaded all Channel and Server ID´s");
        } catch (NumberFormatException e) {
            getLogger().error("Could not load ServerID/ChannelID because of: {}", e.getMessage());
            getLogger().error("Check your config.yml for any mistakes;");
            getLogger().error("Stopping the Server!");
            DACHUtility.getInstance().getServer().shutdown(Component.text("MessageBridge - Could not load ServerID/ChannelID because of " + e.getMessage()));
            return;
        }

        registerCommands(commandManager);

        logger.info("Loading LuckPerms API");
        try{
            luckPerms = LuckPermsProvider.get();
            luckPermsHook = new LuckPermsAPI(this);
            logger.info("Loaded LuckPerms API successfully");
        }catch (Exception exception){
            logger.error("Failed to load LuckPerms API. {}", exception.getMessage());
        }
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        registerListener();
    }

    public static DACHUtility getInstance() {
        return instance;
    }

    public Logger getLogger() {
        return logger;
    }

    public ProxyServer getServer() {
        return server;
    }

    public LuckPermsAPI getLuckPermsHook() {
        return luckPermsHook;
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    public long getMainServerID() {
        return mainServerID;
    }

    public long getAllChannelID() {
        return allChannelID;
    }

    private void registerListener(){
        EventManager eventManager = server.getEventManager();
        eventManager.register(this, new JoinListener());
        eventManager.register(this, new ChangeServerListener());
        eventManager.register(this, new DisconnectListener());

    }

    public void registerCommands(CommandManager commandManager){
        commandManager.register(commandManager.metaBuilder("bc").build(), new BuilderChat());
        commandManager.register(commandManager.metaBuilder("cp").build(), new CheckPerm());
        commandManager.register(commandManager.metaBuilder("dc").build(), new Discord());
        commandManager.register(commandManager.metaBuilder("discord").build(), new Discord());
        commandManager.register(commandManager.metaBuilder("msg").build(), new MSG());
        commandManager.register(commandManager.metaBuilder("write").build(), new MSG());
        commandManager.register(commandManager.metaBuilder("w").build(), new MSG());
        commandManager.register(commandManager.metaBuilder("ping").build(), new Ping());
        commandManager.register(commandManager.metaBuilder("r").build(), new Reply());
        commandManager.register(commandManager.metaBuilder("reply").build(), new Reply());
        commandManager.register(commandManager.metaBuilder("report").build(), new Report());
        commandManager.register(commandManager.metaBuilder("sc").build(), new StaffChat());
        commandManager.register(commandManager.metaBuilder("tc").build(), new ToggelChat());
        commandManager.register(commandManager.metaBuilder("tdc").build(), new ToggelDcChat());
    }
}
