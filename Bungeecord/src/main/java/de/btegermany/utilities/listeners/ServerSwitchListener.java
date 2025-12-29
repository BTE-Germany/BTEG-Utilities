package de.btegermany.utilities.listeners;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import de.btegermany.utilities.restart.Restart;
import de.btegermany.utilities.maintenance.Maintenance;
import de.btegermany.utilities.registry.MaintenancesRegistry;
import de.btegermany.utilities.registry.RestartsRegistry;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;

import de.btegermany.utilities.BTEGUtilitiesBungee;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class ServerSwitchListener implements Listener {

	private final RestartsRegistry restartsRegistry;
	private final MaintenancesRegistry maintenancesRegistry;

	public ServerSwitchListener(RestartsRegistry restartsRegistry, MaintenancesRegistry maintenancesRegistry) {
		this.restartsRegistry = restartsRegistry;
		this.maintenancesRegistry = maintenancesRegistry;
	}

	@EventHandler
	public void onServerDisconnect(ServerDisconnectEvent event) {
		ServerInfo serverInfo = event.getTarget();
		if(serverInfo.getPlayers().size() > 0) {
			return;
		}
		for(Restart restart : this.restartsRegistry.getRestarts().values()) {
			if(!restart.isWhenEmpty()) {
				continue;
			}
			restart.checkRestart();
		}
	}

	@EventHandler
	public void onServerSwitch(ServerSwitchEvent event) {
		for(Maintenance maintenance : this.maintenancesRegistry.getMaintenances().values()) {
			if(!maintenance.proxy() && maintenance.servers().stream().noneMatch(serverInfo -> serverInfo != null && serverInfo.equals(event.getPlayer().getServer().getInfo()))) {
				continue;
			}
			String date = BTEGUtilitiesBungee.convertDate(maintenance.time().getYear(), maintenance.time().getMonthValue(), maintenance.time().getDayOfMonth());
			String time = maintenance.time().getHour() + ":" + (maintenance.time().getMinute() < 10 ? "0" : "") + maintenance.time().getMinute();
			event.getPlayer().sendMessage(new ComponentBuilder("§6Wartungsarbeiten: §c" + date + " §c" + time + " §6" + maintenance.name()).create());
		}
	}
	
	@EventHandler
	public void onServerConnect(ServerConnectEvent event) {
		ProxiedPlayer player = event.getPlayer();

		if(player.getServer() == null) {
			return;
		}

		for(Maintenance maintenance : this.maintenancesRegistry.getMaintenances().values()) {
			if(maintenance.servers().stream().noneMatch(serverInfo -> serverInfo != null && serverInfo.equals(event.getTarget()))) {
				continue;
			}
			ZonedDateTime now = LocalDateTime.now(ZoneId.of("Europe/Berlin")).atZone(ZoneId.of("Europe/Berlin"));
			if(now.isAfter(maintenance.time()) && !player.hasPermission("bteg.maintenance.join")) {
				event.setCancelled(true);
				if(maintenance.servers().stream().anyMatch(serverInfo -> serverInfo != null && serverInfo.getName().equals("Lobby-1")) && player.getServer().getInfo().getName().equals("Lobby-1")) {
					player.disconnect(new ComponentBuilder("Zum aktuellen Zeitpunkt finden Wartungsarbeiten statt!").create());
					continue;
				}
				player.sendMessage(new ComponentBuilder("ᾠ §cAuf §cdiesem §cServer §cfinden §czum §caktuellen §cZeitpunkt §cWartungsarbeiten §cstatt! §cBitte §cwarte, §cbevor §cdu §cauf §cdiesen §cServer §cwechselst!").create());
				return;
			}
		}
	}

	@EventHandler
	public void onServerConnnected(ServerConnectedEvent event) {
		ProxiedPlayer player = event.getPlayer();

		for(Maintenance maintenance : this.maintenancesRegistry.getMaintenances().values()) {
			if(maintenance.servers().stream().noneMatch(serverInfo -> event.getServer().getInfo().equals(serverInfo))) {
				continue;
			}
			ZonedDateTime now = LocalDateTime.now(ZoneId.of("Europe/Berlin")).atZone(ZoneId.of("Europe/Berlin"));
			if(now.isAfter(maintenance.time()) && !player.hasPermission("bteg.maintenance.join")) {
				ServerInfo serverInfoLobby = ProxyServer.getInstance().getServerInfo("Lobby-1");
				if(serverInfoLobby == null || (maintenance.servers().stream().anyMatch(serverInfo -> serverInfo != null && serverInfo.getName().equals("Lobby-1")) && event.getServer().getInfo().getName().equals("Lobby-1"))) {
					player.disconnect(new ComponentBuilder("Zum aktuellen Zeitpunkt finden Wartungsarbeiten statt!").create());
					continue;
				}
				player.sendMessage(new ComponentBuilder("ᾠ §cAuf §cdiesem §cServer §cfinden §czum §caktuellen §cZeitpunkt §cWartungsarbeiten §cstatt! §cBitte §cwarte, §cbevor §cdu §cauf §cdiesen §cServer §cwechselst!").create());
				player.connect(serverInfoLobby);
			}
		}
	}
}
