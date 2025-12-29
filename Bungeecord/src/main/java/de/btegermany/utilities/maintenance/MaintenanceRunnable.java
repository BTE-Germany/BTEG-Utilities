package de.btegermany.utilities.maintenance;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;

public class MaintenanceRunnable implements Runnable {

	private final Maintenance maintenance;
	
	public MaintenanceRunnable(Maintenance maintenance) {
		this.maintenance = maintenance;
	}

	@Override
	public void run() {
		if(this.maintenance.proxy()) {
			ProxyServer.getInstance().getPluginManager().dispatchCommand(ProxyServer.getInstance().getConsole(), "cloudnet syncproxy target Proxy maintenance true");
		}

		for(ServerInfo serverInfo : this.maintenance.servers()) {
			for(ProxiedPlayer player : serverInfo.getPlayers()) {
				if(player.hasPermission("bteg.maintenance.join")) {
					continue;
				}
				if(this.maintenance.servers().stream().anyMatch(serverInfo1 -> serverInfo1 != null && serverInfo1.getName().equals("Lobby-1"))) {
					player.disconnect(new ComponentBuilder("Zum aktuellen Zeitpunkt finden Wartungsarbeiten statt!").create());
					continue;
				}
				player.connect(ProxyServer.getInstance().getServerInfo("Lobby-1"));
				player.sendMessage(new ComponentBuilder("ᾠ §cAuf §cdiesem §cServer §cfinden §czum §caktuellen §cZeitpunkt §cWartungsarbeiten §cstatt!").create());
			}
		}
	}
	
}
