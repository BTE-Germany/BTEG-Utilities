package de.btegermany.utilities.commands;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import de.btegermany.utilities.maintenance.Maintenance;
import de.btegermany.utilities.registry.MaintenancesRegistry;
import net.md_5.bungee.api.ProxyServer;

import de.btegermany.utilities.Servers;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public class MaintenanceCommand extends Command implements TabExecutor {

	private final MaintenancesRegistry maintenancesRegistry;

	public MaintenanceCommand(MaintenancesRegistry maintenancesRegistry) {
		super("maintenance");
		this.maintenancesRegistry = maintenancesRegistry;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		
		if(!sender.hasPermission("bteg.maintenance")) {
			sender.sendMessage(new ComponentBuilder("ᾠ §cDu §cbist §cnicht §cberechtigt, §cdiesen §cCommand §causzuführen!").create());
			return;
		}

		switch (args.length) {
			case 1 -> {
				sender.sendMessage(new ComponentBuilder("ᾠ §6Nutze den Command folgendermaßen:").create());
				sender.sendMessage(new ComponentBuilder("ᾠ §6/maintenance add §c[name] §c[server] §c[date (z.B. 1.2.2034)] §c[time (z.B. 12:34)]").create());
				sender.sendMessage(new ComponentBuilder("ᾠ §6/maintenance cancel §c[name]").create());
			}
			case 2 -> {
				if(!args[0].equalsIgnoreCase("cancel")) {
					ProxyServer.getInstance().getPluginManager().dispatchCommand(sender, "maintenance help");
					return;
				}
				String name = args[1];
				if(!this.maintenancesRegistry.getMaintenances().containsKey(name)) {
					sender.sendMessage(new ComponentBuilder("ᾠ §6Es §6gibt §6keine §6Wartungsarbeiten §6mit §6diesem §6Namen!").create());
					return;
				}
				this.maintenancesRegistry.unregister(name);
				sender.sendMessage(new ComponentBuilder("ᾠ §c" + args[1] + " §6wurde §6entfernt!").create());
			}
			case 5 -> {
				if(!args[0].equalsIgnoreCase("add")) {
					ProxyServer.getInstance().getPluginManager().dispatchCommand(sender, "maintenance help");
					return;
				}
				String name = args[1];
				if(this.maintenancesRegistry.getMaintenances().containsKey(name)) {
					sender.sendMessage(new ComponentBuilder("ᾠ §cEs gibt bereits Wartungsarbeiten mit diesem Namen!").create());
					return;
				}

				Set<ServerInfo> servers = new HashSet<>(Servers.fromInput(args[2].split(",")).values());
				String[] date = args[3].split("\\.");
				String[] time = args[4].split(":");

				boolean proxy = servers.stream().anyMatch(Objects::isNull);
				Maintenance maintenance = new Maintenance(name, servers, LocalDateTime.of(
						Integer.parseInt(date[2]),
						Integer.parseInt(date[1]),
						Integer.parseInt(date[0]),
						Integer.parseInt(time[0]),
						Integer.parseInt(time[1])
				).atZone(ZoneId.of("Europe/Berlin")), proxy);
				servers.removeIf(Objects::isNull);
				this.maintenancesRegistry.register(maintenance);
				sender.sendMessage(new ComponentBuilder("ᾠ §c" + args[1] + " §6wurde §6gespeichert!").create());
			}
		}
	}
	
	@Override
	public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
		List<String> res = new ArrayList<>();
		
		if(!sender.hasPermission("bteg.maintenance")) {
			return res;
		}

		String[] options1 = new String[] {"add", "cancel"};
		
		if(args.length == 1) {
			for(String s : options1) {
				if(s.startsWith(args[0].toLowerCase())) res.add(s);
			}
		}
		
		if(args.length == 2 && args[0].equalsIgnoreCase("cancel")) {
			for(String name : this.maintenancesRegistry.getMaintenances().keySet()) {
				if(!name.toLowerCase().startsWith(args[1].toLowerCase())) {
					continue;
				}
				res.add(name);
			}
		}
		
		return res;
	}

}
