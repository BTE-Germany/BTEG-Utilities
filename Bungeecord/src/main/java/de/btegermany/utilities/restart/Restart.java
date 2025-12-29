package de.btegermany.utilities.restart;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import de.btegermany.utilities.BTEGUtilitiesBungee;
import de.btegermany.utilities.data.PortainerConfig;
import de.btegermany.utilities.registry.RestartsRegistry;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;

public class Restart {

	private final int id;
	private final String name;
	private final BTEGUtilitiesBungee plugin;
	private final Map<String, ServerInfo> servers;
	private final Map<String, ServerInfo> restartedServers = new TreeMap<>();
	private final int initialDelay;
	private final ProxiedPlayer player;
	private final boolean whenEmpty;
	private final RestartsRegistry restartsRegistry;
	private final PortainerConfig portainerConfig;
	private int counter;
	private ScheduledTask scheduledTask;
	
	
	
	public Restart(int id, String name, BTEGUtilitiesBungee plugin, Map<String, ServerInfo> servers, int delay, ProxiedPlayer player, boolean whenEmpty, RestartsRegistry restartsRegistry, PortainerConfig portainerConfig) {
		this.id = id;
		this.name = name;
		this.plugin = plugin;
		this.whenEmpty = whenEmpty;
		this.initialDelay = delay;
		this.counter = delay;
		this.player = player;
		this.restartsRegistry = restartsRegistry;
		this.portainerConfig = portainerConfig;
		this.servers = new TreeMap<>(servers);
	}

	public void start() {
		this.cancel();

		if(player != null) {
			TextComponent comp = new TextComponent("§9hier ");
			comp.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/bteg restart " + this.id + " cancel"));
			player.sendMessage(new ComponentBuilder("ᾠ §9/bteg §9restart §9" + this.id + " §9cancel §6oder ").append(comp).append("§6klicken, §6um §6den §6Restart §6folgender §6Server §6abzubrechen:").create());
			servers.forEach((k, v) -> player.sendMessage(new ComponentBuilder(" §9" + k).create()));
		}

		this.scheduledTask = ProxyServer.getInstance().getScheduler().schedule(this.plugin, () -> {
			if(counter == 0) {
				if(this.whenEmpty) {
					this.checkRestart();
				} else {
					this.restartNow();
				}
				this.scheduledTask.cancel();
				return;
			}

			int h = counter / 60 / 60;
			int min = (counter / 60) % 60;
			int s = counter % 60;

			String hours = "§1" + h + " §1hour" + (h != 1 ? "s" : "");
			String minutes = "§1" + min + " §1minute" + (min != 1 ? "s" : "");
			String seconds = "§1" + s + " §1second" + (s != 1 ? "s" : "");

			if(counter == initialDelay && player != null) {
				player.sendMessage(new ComponentBuilder("ᾠ §6Restarting §6in §9" + h + ":" + min + ":" + s + " §9h").create());
			}

			// hours
			if(min == 0 && s == 0) {
				sendMessage(ChatMessageType.CHAT, "ᾠ §cRestarting server §cin §1" + hours + "!");
			}

			// last hour
			if(h == 0) {

				// chat	minutes
				if((s == 0 && (min == 30 || min == 15 || min == 10 || min == 5 || min == 1)) || (s == 30 && min == 1)) {
					sendMessage(ChatMessageType.CHAT, "ᾠ §cRestarting server §cin " + minutes + (s != 0 ? " §1and §1" + seconds : "") + "!");
				}
				// chat seconds last minute
				if(min == 0 && (s == 30 || s <= 15)) {
					sendMessage(ChatMessageType.CHAT, "ᾠ §cRestarting server §cin " + seconds + "!");
				}

				// action bar
				if(min < 15) {
					sendMessage(ChatMessageType.ACTION_BAR, "§c§lRestarting server §c§lin §1" + (min >= 1 ? (s >= 1 ? min + ":" + (s < 10 ? "0" : "") + s + " §1min" : minutes) : seconds));
				}
			}

			counter--;
		}, 0, 1, TimeUnit.SECONDS);
	}
	
	public void cancel() {
		if(this.scheduledTask != null) {
			this.scheduledTask.cancel();
		}
		counter = initialDelay;
		if(player != null) player.sendMessage(new ComponentBuilder("ᾠ §6Der §6Countdown §6wurde §6abgebrochen.").create());
	}
	
	private void sendMessage(ChatMessageType type, String message) {
		ProxyServer.getInstance().getServers().forEach((k, v) -> {
			if(servers.keySet().stream().anyMatch(key -> key.equalsIgnoreCase(k)) || servers.containsKey("Proxy-1")) {
				for(ProxiedPlayer p : v.getPlayers()) {
					p.sendMessage(type, new ComponentBuilder(message).create());
				}
			}
		});
	}
	
	public void restartNow() {
		CompletableFuture.runAsync(() -> {
			this.getServersToRestart().join()
					.forEach(entry -> this.doRestartCall(entry.getKey()));
		}).thenRun(() -> {
			this.restartsRegistry.unregister(this);
		});
	}

	// in case whenEmpty is true
	public void checkRestart() {
		if (this.counter > 0) {
			return;
		}

		CompletableFuture.runAsync(() -> {
			this.getServersToRestart().join()
					.forEach(entry -> {
						this.doRestartCall(entry.getKey());
						this.restartedServers.put(entry.getKey(), entry.getValue());
					});
		}).thenRun(() -> {
			if (this.restartedServers.size() == this.servers.size()) {
				this.restartsRegistry.unregister(this);
			}
		});
	}

	private CompletableFuture<List<Map.Entry<String, ServerInfo>>> getServersToRestart() {
		return CompletableFuture.supplyAsync(() -> {
			List<Map.Entry<String, ServerInfo>> list = new ArrayList<>(this.servers.entrySet().stream()
					.filter(entry -> {
						// suppliers because e.g. alreadyRestarted should not run for Proxy
						Supplier<Boolean> isProxy = () -> entry.getKey().equalsIgnoreCase("Proxy-1");
						Supplier<Boolean> alreadyRestarted = () -> this.restartedServers.containsKey(entry.getKey());
						Supplier<Boolean> noPlayers = () -> entry.getValue().getPlayers().isEmpty();
						// !whenEmpty: only check !isProxy. whenEmpty: also check after ||
						return !isProxy.get() && (!this.whenEmpty || (!alreadyRestarted.get() && noPlayers.get()));
					}).toList());
			// !whenEmpty: only check if proxy. whenEmpty: check if all servers are empty
			if (this.servers.containsKey("Proxy-1") && (!this.whenEmpty || ProxyServer.getInstance().getServers().values().stream().allMatch(serverInfo -> serverInfo.getPlayers().isEmpty()))) {
				list.add(new AbstractMap.SimpleEntry<>("Proxy-1", this.servers.get("Proxy-1")));
			}
			return list;
		});
	}

	private void doRestartCall(String server) {
		server = server.toLowerCase();
		if (server.equalsIgnoreCase("Proxy-1")) {
			server = "proxy";
		}

		Integer environmentId = this.portainerConfig.environmentId();
		String accessToken = this.portainerConfig.accessToken();

		if (environmentId == null || accessToken == null) {
			return;
		}

		try {
			URI uri = new URI("https://portainer.bteger.dev/api/endpoints/%s/docker/containers/%s/restart".formatted(environmentId, server));
			HttpRequest request = HttpRequest.newBuilder()
					.uri(uri)
					.POST(HttpRequest.BodyPublishers.noBody())
					.header("X-API-Key", accessToken)
					.header("Accept", "*/*")
					.build();
			HttpResponse<String> response = HttpClient
					.newHttpClient()
					.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() == 200 || response.statusCode() == 204) {
				player.sendMessage(new TextComponent("ᾠ §6Restarting server '%s' now".formatted(server)));
				return;
			}

			player.sendMessage(new TextComponent("ᾠ §6Failed to restart server '%s'".formatted(server)));
			this.plugin.getLogger().warning("Failed to restart server '%s'. Status Code %o".formatted(server, response.statusCode()));

		} catch (IOException | InterruptedException | URISyntaxException e) {
			this.plugin.getLogger().warning("Failed to restart server '%s'".formatted(server));
			e.printStackTrace();
		}
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public boolean isWhenEmpty() {
		return whenEmpty;
	}
}