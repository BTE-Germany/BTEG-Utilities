package de.btegermany.utilities;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import de.btegermany.utilities.commands.MaintenanceCommand;
import de.btegermany.utilities.commands.PlotsCommand;
import de.btegermany.utilities.commands.RestartCommand;
import de.btegermany.utilities.data.ConfigReader;
import de.btegermany.utilities.listeners.ServerSwitchListener;
import de.btegermany.utilities.registry.MaintenancesRegistry;
import de.btegermany.utilities.registry.RestartsRegistry;
import de.btegermany.utilities.restart.RestartsIDsManager;
import de.btegermany.utilities.maintenance.Maintenance;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import de.btegermany.utilities.maintenance.MaintenanceRunnable;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

public class BTEGUtilitiesBungee extends Plugin {
	
	private ScheduledExecutorService scheduledExecutorServiceMaintenance;

	@Override
	public void onEnable() {
		ConfigReader configReader = new ConfigReader(this);
		RestartsIDsManager restartsIDsManager = new RestartsIDsManager();
		RestartsRegistry restartsRegistry = new RestartsRegistry(restartsIDsManager);
		MaintenancesRegistry maintenancesRegistry = new MaintenancesRegistry(this, "maintenances.json");
		maintenancesRegistry.loadMaintenances();

		ProxyServer.getInstance().getPluginManager().registerCommand(this, new MaintenanceCommand(maintenancesRegistry));
		ProxyServer.getInstance().getPluginManager().registerCommand(this, new RestartCommand(this, restartsRegistry, restartsIDsManager, configReader));
		ProxyServer.getInstance().getPluginManager().registerCommand(this, new PlotsCommand());

		ProxyServer.getInstance().registerChannel("Restart");
		
		ProxyServer.getInstance().getPluginManager().registerListener(this, new ServerSwitchListener(restartsRegistry, maintenancesRegistry));
		
		this.scheduleMaintenances(maintenancesRegistry);

		Function<TabPlayer, String> placeholderFunction = tabPlayer -> {
			if(maintenancesRegistry.getMaintenances().isEmpty()) {
				return "";
			}

			StringBuilder builder = new StringBuilder();

			for(Maintenance maintenance : maintenancesRegistry.getMaintenances().values()) {
				if (!maintenance.proxy() && maintenance.servers().stream().noneMatch(serverInfo -> serverInfo.equals(((ProxiedPlayer) tabPlayer.getPlayer()).getServer().getInfo()))) {
					continue;
				}
				String date = BTEGUtilitiesBungee.convertDate(maintenance.time().getYear(), maintenance.time().getMonthValue(), maintenance.time().getDayOfMonth());
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
	
	@Override
	public void onDisable() {
		TabAPI.getInstance().getPlaceholderManager().unregisterPlaceholder("%maintenances-display%");
		this.scheduledExecutorServiceMaintenance.shutdownNow();
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
		LocalDate tomorrow = today.plus(1, ChronoUnit.DAYS);
		if(search.isEqual(today)) {
			return "Heute";
		} else if(search.isEqual(tomorrow)) {
			return "Morgen";
		} else {
			return day + "." + (month < 10 ? "0" : "") + month + "." + year;
		}
	}
	
}
