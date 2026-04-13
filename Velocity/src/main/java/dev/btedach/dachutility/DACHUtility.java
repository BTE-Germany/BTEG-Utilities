package dev.btedach.dachutility;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import dev.btedach.dachutility.commands.*;
import dev.btedach.dachutility.data.ConfigReader;
import dev.btedach.dachutility.listener.ChangeServerListener;
import dev.btedach.dachutility.listener.ChatListener;
import dev.btedach.dachutility.maintenance.Maintenance;
import dev.btedach.dachutility.maintenance.MaintenanceRunnable;
import dev.btedach.dachutility.registry.DndPlayersRegistry;
import dev.btedach.dachutility.registry.MaintenancesRegistry;
import dev.btedach.dachutility.registry.RestartsRegistry;
import dev.btedach.dachutility.restart.RestartsIDsManager;
import dev.btedach.dachutility.data.AccountLinkConfig;
import dev.btedach.dachutility.utils.Constants;
import dev.btedach.dachutility.utils.chat.MessageCache;
import lombok.Getter;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class DACHUtility {

    public static final MinecraftChannelIdentifier PLUGIN_CHANNEL = MinecraftChannelIdentifier.create("bteg", "utilities");
    @Getter
    public static DACHUtility instance;

    @Getter
    @Inject
    private final Logger logger;
    @Getter
    private final ProxyServer proxyServer;
    private final Path dataDirectoryPath;

    private ConfigReader configReader;
    private RestartsIDsManager restartsIDsManager;

    private RestartsRegistry restartsRegistry;
    private MaintenancesRegistry maintenancesRegistry;
    DndPlayersRegistry dndPlayersRegistry;
    private MessageCache messageCache;

    private Algorithm algorithm;
    private AccountLinkConfig config;

    private ScheduledExecutorService scheduledExecutorServiceMaintenance;

    @Inject
    public DACHUtility(ProxyServer proxyServer, Logger logger, @DataDirectory Path dataDirectoryPath) throws IOException {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.dataDirectoryPath = dataDirectoryPath;
        instance = this;
        logger.info("Starting DACH Utility");
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.configReader = new ConfigReader(this.dataDirectoryPath, this.logger);
        this.restartsIDsManager = new RestartsIDsManager();
        this.restartsRegistry = new RestartsRegistry(restartsIDsManager);
        this.maintenancesRegistry = new MaintenancesRegistry(this, this.dataDirectoryPath, "maintenances.json");
        this.maintenancesRegistry.loadMaintenances();
        this.dndPlayersRegistry = new DndPlayersRegistry();
        this.messageCache = new MessageCache();

        this.readAccountLinkConfig();

        this.proxyServer.getChannelRegistrar().register(PLUGIN_CHANNEL);

        registerCommands();
        registerListener();

        registerMaintenancePlaceholder();
    }

    private void registerListener() {
        EventManager eventManager = this.proxyServer.getEventManager();
        eventManager.register(this, new ChangeServerListener(this.maintenancesRegistry, this.dndPlayersRegistry));
        eventManager.register(this, new ChatListener(this.messageCache));
    }

    private void registerCommands() {
        CommandManager commandManager = this.proxyServer.getCommandManager();
        commandManager.register(commandManager.metaBuilder("dc").aliases("discord").build(), new DiscordCommand());
        commandManager.register(commandManager.metaBuilder("ping").build(), new PingCommand());
        commandManager.register(commandManager.metaBuilder("report").build(), new ReportCommand(this.proxyServer, this.configReader, this.messageCache));
        commandManager.register(commandManager.metaBuilder("maintenance").build(), new MaintenanceCommand(this.maintenancesRegistry, this.proxyServer));
        commandManager.register(commandManager.metaBuilder("bteg").build(), new RestartCommand(this, this.proxyServer, this.restartsRegistry, this.restartsIDsManager, this.configReader));
        commandManager.register(commandManager.metaBuilder("plotsystem").aliases("plotserver").build(), new PlotsCommand());
        commandManager.register(commandManager.metaBuilder("accountlink").build(), new AccountLinkCommand());
        commandManager.register(commandManager.metaBuilder("dnd").aliases("donotdisturb", "rec", "recording").build(), new DndCommand(this.proxyServer, this.dndPlayersRegistry));
    }

    private void registerMaintenancePlaceholder() {
        Function<TabPlayer, String> placeholderFunction = tabPlayer -> {
            if (maintenancesRegistry.getMaintenances().isEmpty()) {
                return "";
            }

            StringBuilder builder = new StringBuilder();

            for (Maintenance maintenance : maintenancesRegistry.getMaintenances().values()) {
                if (!maintenance.proxy() && maintenance.servers().stream().noneMatch(server -> server.getPlayersConnected().contains((Player) tabPlayer.getPlayer()))) {
                    continue;
                }
                String date = DACHUtility.convertDate(maintenance.time().getYear(), maintenance.time().getMonthValue(), maintenance.time().getDayOfMonth());
                String time = maintenance.time().getHour() + ":" + (maintenance.time().getMinute() < 10 ? "0" : "") + maintenance.time().getMinute();
                builder.append("\n§6").append(maintenance.name()).append(": §c").append(date).append(" §c").append(time);
            }

            if (builder.isEmpty()) {
                return "";
            }

            builder.insert(0, "\n§6§lGeplante Wartungsarbeiten");
            builder.append("\n");

            return builder.toString();
        };

        TabAPI.getInstance().getPlaceholderManager().registerPlayerPlaceholder("%maintenances-display%", 3000, placeholderFunction);
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        TabAPI.getInstance().getPlaceholderManager().unregisterPlaceholder("%maintenances-display%");
    }

    public void scheduleMaintenances(MaintenancesRegistry maintenancesRegistry) {
        if (this.scheduledExecutorServiceMaintenance != null) {
            this.scheduledExecutorServiceMaintenance.shutdownNow();
        }
        this.scheduledExecutorServiceMaintenance = new ScheduledThreadPoolExecutor(maintenancesRegistry.getMaintenances().size());

        for (Maintenance maintenance : maintenancesRegistry.getMaintenances().values()) {
            ZonedDateTime now = LocalDateTime.now(ZoneId.of("Europe/Berlin")).atZone(ZoneId.of("Europe/Berlin"));
            long delay = ChronoUnit.MILLIS.between(now, maintenance.time());
            this.scheduledExecutorServiceMaintenance.schedule(new MaintenanceRunnable(maintenance), delay, TimeUnit.MILLISECONDS);
        }
    }

    public static String convertDate(int year, int month, int day) {
        LocalDate search = LocalDate.of(year, month, day);
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Berlin"));
        LocalDate tomorrow = today.plusDays(1);
        if (search.isEqual(today)) {
            return "Heute";
        } else if (search.isEqual(tomorrow)) {
            return "Morgen";
        } else {
            return day + "." + (month < 10 ? "0" : "") + month + "." + year;
        }
    }

    public static Component getMessage(Component... components) {
        Component message = Constants.prefixComponent;
        for (Component component : components) {
            message = message.append(component);
        }
        return message;
    }

    public static void sendMessage(CommandSource receiver, Component... components) {
        receiver.sendMessage(DACHUtility.getMessage(components));
    }

    private void readAccountLinkConfig() {
        config = configReader.readAccountLinkConfig();

        algorithm = Algorithm.HMAC512(config.jwtSecret());
    }

    public String generateUrl(Player player) {
        String token = JWT.create()
                .withSubject(player.getUniqueId().toString())
                .withExpiresAt(new Date(System.currentTimeMillis() + 30 * 60 * 1000)) // 30 minutes expiration
                .withIssuer("AccountConsoleBungee")
                .sign(algorithm);
        return config.urlFormat().replace("{token}", token);
    }
}
