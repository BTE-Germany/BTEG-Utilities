package dev.btedach.dachutility;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.btedach.dachutility.commands.*;
import dev.btedach.dachutility.implementation.LuckPermsAPI;
import dev.btedach.dachutility.listener.ChangeServerListener;
import dev.btedach.dachutility.listener.DisconnectListener;
import dev.btedach.dachutility.listener.JDAChatListener;
import dev.btedach.dachutility.listener.JoinListener;
import dev.btedach.dachutility.maintenance.Maintenance;
import dev.btedach.dachutility.maintenance.MaintenanceRunnable;
import dev.btedach.dachutility.registry.MaintenancesRegistry;
import dev.btedach.dachutility.utils.FileManager;
import lombok.Getter;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Plugin(
        id = "dach-utility",
        name = "DACH-Utility",
        version = "1.0.0-SNAPSHOT",
        description = "Proxy plugin for BTEG X BTE Alps",
        url = "https://buildthe.earth/dach",
        authors = {"Dev Team of BTEG and BTE Alps"},
        dependencies = {
                @Dependency(id = "luckperms"),
                @Dependency(id = "tab")
        }
)
public class DACHUtility {

    @Getter
    @Inject
    private final Logger logger;
    @Getter
    private final ProxyServer server;

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
    private final FileManager fileManager;

    @Getter
    private LuckPermsAPI luckPermsHook;

    @Getter
    private LuckPerms luckPerms;

    private final MaintenancesRegistry maintenancesRegistry;

    private ScheduledExecutorService scheduledExecutorServiceMaintenance;

    @Inject
    public DACHUtility(ProxyServer server, Logger logger, CommandManager commandManager, @DataDirectory Path dataDirectoryPath) throws IOException {
        this.server = server;
        this.logger = logger;
        instance = this;
        logger.info("Starting DACH Utility");
        this.fileManager = new FileManager();

        this.maintenancesRegistry = new MaintenancesRegistry(this, dataDirectoryPath, "maintenances.json");
        this.maintenancesRegistry.loadMaintenances();

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

        JDABuilder builder = JDABuilder.createDefault((String) getFileManager().fetchStringFromConfig(FileManager.FILETYPE.CONFIG, "token"));

        builder.addEventListeners(new JDAChatListener(this));

        builder.setStatus(OnlineStatus.ONLINE);
        builder.setChunkingFilter(ChunkingFilter.ALL);
        builder.enableIntents(GatewayIntent.GUILD_PRESENCES);
        builder.enableIntents(GatewayIntent.GUILD_MEMBERS);
        builder.enableIntents(GatewayIntent.MESSAGE_CONTENT);
        builder.enableCache(CacheFlag.ACTIVITY);
        builder.enableCache(CacheFlag.ONLINE_STATUS);
        builder.setMemberCachePolicy(MemberCachePolicy.ALL);
        builder.enableIntents(GatewayIntent.GUILD_MEMBERS);

        jda = builder.build();

        registerCommands(commandManager);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Loading LuckPerms API");
        try{
            luckPerms = LuckPermsProvider.get();
            luckPermsHook = new LuckPermsAPI(this);
            logger.info("Loaded LuckPerms API successfully");
        }catch (Exception exception){
            logger.error("Failed to load LuckPerms API. {}", exception.getMessage());
        }

        registerListener();

        Function<TabPlayer, String> placeholderFunction = tabPlayer -> {
            if(maintenancesRegistry.getMaintenances().isEmpty()) {
                return "";
            }

            StringBuilder builder = new StringBuilder();

            for(Maintenance maintenance : maintenancesRegistry.getMaintenances().values()) {
                if (!maintenance.proxy() && maintenance.servers().stream().noneMatch(server -> server.getPlayersConnected().contains((Player) tabPlayer.getPlayer()))) {
                    continue;
                }
                String date = DACHUtility.convertDate(maintenance.time().getYear(), maintenance.time().getMonthValue(), maintenance.time().getDayOfMonth());
                String time = maintenance.time().getHour() + ":" + (maintenance.time().getMinute() < 10 ? "0" : "") + maintenance.time().getMinute();
                builder.append("\n§6").append(maintenance.name()).append(": §c").append(date).append(" §c").append(time);
            }
            if(builder.isEmpty()) return "";

            builder.insert(0, "\n§6§lGeplante Wartungsarbeiten");
            builder.append("\n");

            return builder.toString();
        };

        TabAPI.getInstance().getPlaceholderManager().registerPlayerPlaceholder("%maintenances-display%", 1000, placeholderFunction);
    }

    private void registerListener(){
        EventManager eventManager = server.getEventManager();
        eventManager.register(this, new JoinListener());
        eventManager.register(this, new ChangeServerListener(this.maintenancesRegistry));
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
        commandManager.register(commandManager.metaBuilder("maintenance").build(), new MaintenanceCommand(this.maintenancesRegistry, this.server));
        commandManager.register(commandManager.metaBuilder("plotsystem").build(), new PlotsCommand());
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        TabAPI.getInstance().getPlaceholderManager().unregisterPlaceholder("%maintenances-display%");
    }

    public void scheduleMaintenances(MaintenancesRegistry maintenancesRegistry) {
        if(this.scheduledExecutorServiceMaintenance != null) {
            this.scheduledExecutorServiceMaintenance.shutdownNow();
        }
        this.scheduledExecutorServiceMaintenance = new ScheduledThreadPoolExecutor(maintenancesRegistry.getMaintenances().size());

        for(Maintenance maintenance : maintenancesRegistry.getMaintenances().values()) {
            ZonedDateTime now = LocalDateTime.now(ZoneId.of("Europe/Berlin")).atZone(ZoneId.of("Europe/Berlin"));
            long delay = ChronoUnit.MILLIS.between(now, maintenance.time());
            this.scheduledExecutorServiceMaintenance.schedule(new MaintenanceRunnable(maintenance), delay, TimeUnit.MILLISECONDS);
        }
    }

    public static String convertDate(int year, int month, int day) {
        LocalDate search = LocalDate.of(year, month, day);
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Berlin"));
        LocalDate tomorrow = today.plusDays(1);
        if(search.isEqual(today)) {
            return "Heute";
        } else if(search.isEqual(tomorrow)) {
            return "Morgen";
        } else {
            return day + "." + (month < 10 ? "0" : "") + month + "." + year;
        }
    }
}
