package dev.btedach.dachutility.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.btedach.dachutility.DACHUtility;
import dev.btedach.dachutility.utils.Servers;
import dev.btedach.dachutility.data.ConfigReader;
import dev.btedach.dachutility.data.PortainerConfig;
import dev.btedach.dachutility.registry.RestartsRegistry;
import dev.btedach.dachutility.restart.Restart;
import dev.btedach.dachutility.restart.RestartsIDsManager;
import dev.btedach.dachutility.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.btedach.dachutility.DACHUtility.sendMessage;

public class RestartCommand implements SimpleCommand {

	private final DACHUtility plugin;
	private final ProxyServer proxyServer;
	private final RestartsRegistry restartsRegistry;
	private final RestartsIDsManager restartsIDsManager;
	private final PortainerConfig portainerConfig;
	
	public RestartCommand(DACHUtility plugin, ProxyServer proxyServer, RestartsRegistry restartsRegistry, RestartsIDsManager restartsIDsManager, ConfigReader configReader) {
		this.plugin = plugin;
		this.proxyServer = proxyServer;
		this.restartsRegistry = restartsRegistry;
		this.restartsIDsManager = restartsIDsManager;
		this.portainerConfig = configReader.readPortainerConfig();
	}

	@Override
	public void execute(Invocation invocation) {
		CommandSource source = invocation.source();
		String[] args = invocation.arguments();

		if (args.length == 1 && args[0].equalsIgnoreCase("restarts")) {
			if (this.restartsRegistry.getRestarts().isEmpty()) {
				sendMessage(source, Component.text("Es wurden noch keine Restarts hinzugefügt.", NamedTextColor.RED));
				return;
			}
			for (Restart restart : this.restartsRegistry.getRestarts().values()) {
				sendMessage(source, Component.text("ID: %d".formatted(restart.getId()) + (restart.getName() != null ? String.format(", %s", restart.getName()) : ""), NamedTextColor.GOLD));
			}
			return;
		}

		if(args.length == 2 && args[0].equalsIgnoreCase("restart") && args[1].equalsIgnoreCase("help")) {
			sendMessage(source, Component.text("Nutze den Command folgendermaßen:", NamedTextColor.GOLD));
			sendMessage(source, Component.text("/bteg server §c[server] §6restart §c[optional: Delay (z.B. 1h2m3s) oder \"empty\" (wenn Server leer restarten)] §c[optional: name]", NamedTextColor.GOLD));
			sendMessage(source, Component.text("/bteg restarts", NamedTextColor.GOLD));
			sendMessage(source, Component.text("/bteg restart §c[id] §6cancel", NamedTextColor.GOLD));
			return;
		}

		if(args.length < 3) {
			this.sendHelp(source);
			return;
		}

		switch (args[0].toLowerCase()) {
			case "restart" -> {
				if(!(args[1].matches("\\d+") && args[2].equalsIgnoreCase("cancel"))) {
					this.sendHelp(source);
					return;
				}
				int id = Integer.parseInt(args[1]);
				Restart restart = this.restartsRegistry.getRestart(id);
				if(restart == null) {
					sendMessage(source, Component.text("Die ID ist ungültig!", NamedTextColor.RED));
					return;
				}
				restart.cancel();
				this.restartsRegistry.unregister(restart);
				sendMessage(source, Component.text("Der Restart wurde abgebrochen.", NamedTextColor.GOLD));
				return;
			}
			case "server" -> {
				if(!args[2].equalsIgnoreCase("restart")) {
					this.sendHelp(source);
					return;
				}
				String[] serversArgs = args[1].split(",");
				Map<String, RegisteredServer> servers = Servers.fromInput(serversArgs);

				switch (args.length) {
					case 3 -> {
						int id = this.restartsIDsManager.getAndClaimNextId();
						Restart restart = new Restart(id, null, this.plugin, this.proxyServer, servers, 120, source, false, this.restartsRegistry, this.portainerConfig);
						this.restartsRegistry.register(restart);
						sendMessage(source, Component.text("Der Restart wurde hinzugefügt. ", NamedTextColor.GOLD),
								Component.text("ID: %d".formatted(id), NamedTextColor.RED));
						restart.start();
						return;
					}
					case 4 -> {
						String name = null;
						int delay = 120;
						boolean whenEmpty = false;

						if(args[3].matches("(\\d+[hms])+")) {
							delay = this.getDelayFromInput(args[3]);
						} else if (args[3].equals("empty")) {
							delay = 0;
							whenEmpty = true;
						} else {
							name = args[3];
						}

						int id = this.restartsIDsManager.getAndClaimNextId();
						Restart restart = new Restart(id, name, this.plugin, this.proxyServer, servers, delay, source, whenEmpty, this.restartsRegistry, this.portainerConfig);
						this.restartsRegistry.register(restart);
						sendMessage(source, Component.text("Der Restart wurde hinzugefügt. ", NamedTextColor.GOLD), Component.text("ID: %d".formatted(id) + (name != null ? (", Name: " + name) : ""), NamedTextColor.RED));
						restart.start();
						return;
					}
					case 5 -> {
						int delay;
						boolean whenEmpty = false;

						if(args[3].matches("(\\d+[hms])+")) {
							delay = this.getDelayFromInput(args[3]);
						} else if (args[3].equals("empty")) {
							delay = 0;
							whenEmpty = true;
						} else {
							this.sendHelp(source);
							return;
						}
						String name = args[4];

						int id = this.restartsIDsManager.getAndClaimNextId();
						Restart restart = new Restart(id, name, this.plugin, this.proxyServer, servers, delay, source, whenEmpty, this.restartsRegistry, this.portainerConfig);
						this.restartsRegistry.register(restart);
						sendMessage(source, Component.text("Der Restart wurde hinzugefügt. ", NamedTextColor.GOLD),
								Component.text("ID: %d, Name: %s".formatted(id, name)));
						restart.start();
						return;
					}
				}
			}
		}

		this.sendHelp(source);
	}

	private void sendHelp(CommandSource source) {
		this.proxyServer.getCommandManager().executeAsync(source, "bteg restart help");
	}

	private int getDelayFromInput(String input) {
		int delay = 0;
		Matcher matcherHours = Pattern.compile("\\d+h").matcher(input);
		Matcher matcherMinutes = Pattern.compile("\\d+m").matcher(input);
		Matcher matcherSeconds = Pattern.compile("\\d+s").matcher(input);
		while (matcherHours.find()) {
			delay += Integer.parseInt(matcherHours.group().substring(0, matcherHours.group().length() - 1)) * 3600;
		}
		while (matcherMinutes.find()) {
			delay += Integer.parseInt(matcherMinutes.group().substring(0, matcherMinutes.group().length() - 1)) * 60;
		}
		while (matcherSeconds.find()) {
			delay += Integer.parseInt(matcherSeconds.group().substring(0, matcherSeconds.group().length() - 1));
		}
		return delay;
	}

	@Override
	public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
		return CompletableFuture.supplyAsync(() -> {
			List<String> result = new ArrayList<>();

			if (!this.hasPermission(invocation)) {
				return result;
			}

			String[] args = invocation.arguments();

			String[] options1 = new String[] {"server", "restart", "restarts", "help"};

			switch (args.length) {
				case 0 -> Collections.addAll(result, options1);

				case 1 -> {
					for (String s : options1) {
						if(s.startsWith(args[0].toLowerCase())) result.add(s);
					}
				}

				case 2 -> {
					if (!args[0].equalsIgnoreCase("server")) {
						break;
					}

					result.addAll(Utils.getServersTabCompletion(args[1], this.proxyServer));
				}

				case 3 -> {
					if("restart".startsWith(args[2].toLowerCase()) && args[0].equalsIgnoreCase("server")) {
						result.add("restart");
					}
					if("cancel".startsWith(args[2].toLowerCase()) && args[0].equalsIgnoreCase("restart")) {
						result.add("cancel");
					}
				}

				case 4 -> {
					if(!args[0].equalsIgnoreCase("server")) {
						break;
					}
					String input = args[3];
					if("empty".startsWith(input.toLowerCase())) {
						result.add("empty");
						break;
					}
					int indexSeconds = input.lastIndexOf("s");
					int indexMinutes = input.lastIndexOf("m");
					int indexHours = input.lastIndexOf("h");
					int highestIndex = Math.max(indexSeconds, Math.max(indexMinutes, indexHours));
					if(highestIndex == input.length() - 1 && highestIndex > -1) {
						break;
					}
					result.add(input + "s");
					result.add(input + "m");
					result.add(input + "h");
				}
			}

			return result;
		});
	}

	@Override
	public boolean hasPermission(Invocation invocation) {
		return invocation.source().hasPermission("bteg.restart");
	}
	
}
