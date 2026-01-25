package dev.btedach.dachutility;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
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
import dev.btedach.dachutility.listener.ChangeServerListener;
import dev.btedach.dachutility.maintenance.Maintenance;
import dev.btedach.dachutility.maintenance.MaintenanceRunnable;
import dev.btedach.dachutility.registry.MaintenancesRegistry;
import dev.btedach.dachutility.utils.AccountConsoleConfig;
import dev.btedach.dachutility.utils.Constants;
import lombok.Getter;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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

@Plugin(
        id = "dach-utility",
        name = "DACH-Utility",
        version = "1.0.0-SNAPSHOT",
        description = "Proxy plugin for BTEG X BTE Alps",
        url = "https://buildthe.earth/dach",
        authors = {"Dev Team of BTEG and BTE Alps"},
        dependencies = {
                @Dependency(id = "tab")
        }
)
public class DACHUtility {

    @Getter
    public static DACHUtility instance;

    @Getter
    @Inject
    private final Logger logger;
    @Getter
    private final ProxyServer proxy;
    private final Path dataDirectoryPath;

    private MaintenancesRegistry maintenancesRegistry;

    private Algorithm algorithm;
    private AccountConsoleConfig config;

    private ScheduledExecutorService scheduledExecutorServiceMaintenance;

    @Inject
    public DACHUtility(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectoryPath) throws IOException {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectoryPath = dataDirectoryPath;
        instance = this;
        logger.info("Starting DACH Utility");
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.maintenancesRegistry = new MaintenancesRegistry(this, this.dataDirectoryPath, "maintenances.json");
        this.maintenancesRegistry.loadMaintenances();

        this.readAccountLinkConfig();

        registerCommands();

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

        TabAPI.getInstance().getPlaceholderManager().registerPlayerPlaceholder("%maintenances-display%", 3000, placeholderFunction);
    }

    private void registerListener() {
        EventManager eventManager = this.proxy.getEventManager();
        eventManager.register(this, new ChangeServerListener(this.maintenancesRegistry));

    }

    public void registerCommands() {
        CommandManager commandManager = this.proxy.getCommandManager();
        commandManager.register(commandManager.metaBuilder("dc").aliases("discord").build(), new Discord());
        commandManager.register(commandManager.metaBuilder("ping").build(), new Ping());
        //commandManager.register(commandManager.metaBuilder("report").build(), new Report()); Currently broken/not fully implemented
        commandManager.register(commandManager.metaBuilder("maintenance").build(), new MaintenanceCommand(this.maintenancesRegistry, this.proxy));
        commandManager.register(commandManager.metaBuilder("plotsystem").aliases("plotserver").build(), new PlotsCommand());
        commandManager.register(commandManager.metaBuilder("accountlink").build(), new AccountLinkCommand());
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
        File file = new File(this.dataDirectoryPath.toFile(), "config.json");

        if (!file.exists()) {
            throw new RuntimeException("Config file not found");
        }

        try {
            config = new Gson().fromJson(new FileReader(file), AccountConsoleConfig.class);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

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
