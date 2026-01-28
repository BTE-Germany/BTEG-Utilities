package dev.btedach.dachutility.restart;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import dev.btedach.dachutility.DACHUtility;
import dev.btedach.dachutility.data.PortainerConfig;
import dev.btedach.dachutility.registry.RestartsRegistry;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

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

import static dev.btedach.dachutility.DACHUtility.sendMessage;

public class Restart {

	@Getter
    private final int id;
	@Getter
    private final String name;
	private final DACHUtility plugin;
	private final ProxyServer proxyServer;
	private final Map<String, RegisteredServer> servers;
	private final Map<String, RegisteredServer> restartedServers = new TreeMap<>();
	private final int initialDelay;
	private final CommandSource source;
	private final boolean whenEmpty;
	private final RestartsRegistry restartsRegistry;
	private final PortainerConfig portainerConfig;
	private int counter;
	private ScheduledTask scheduledTask;
	
	public Restart(int id, String name, DACHUtility plugin, ProxyServer proxyServer, Map<String, RegisteredServer> servers, int delay, CommandSource source, boolean whenEmpty, RestartsRegistry restartsRegistry, PortainerConfig portainerConfig) {
		this.id = id;
		this.name = name;
		this.plugin = plugin;
		this.proxyServer = proxyServer;
		this.whenEmpty = whenEmpty;
		this.initialDelay = delay;
		this.counter = delay;
		this.source = source;
		this.restartsRegistry = restartsRegistry;
		this.portainerConfig = portainerConfig;
		this.servers = new TreeMap<>(servers);
	}

	public void start() {
		this.cancel();

		if (source != null) {
			sendMessage(source, Component.text("/bteg restart " + this.id + " cancel ", NamedTextColor.BLUE),
					Component.text("oder ", NamedTextColor.GOLD),
					Component.text("hier ", NamedTextColor.BLUE)
							.clickEvent(ClickEvent.runCommand("/bteg restart %d cancel".formatted(this.id))),
					Component.text("klicken, um den Restart folgender Server abzubrechen:", NamedTextColor.GOLD));
			servers.forEach((k, v) -> sendMessage(source, Component.text(k, NamedTextColor.BLUE)));
		}

		this.scheduledTask = this.proxyServer.getScheduler()
				.buildTask(this.plugin, () -> {
					if (counter == 0) {
						if (this.whenEmpty) {
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

					String hours = h + " hour" + (h != 1 ? "s" : "");
					String minutes = min + " minute" + (min != 1 ? "s" : "");
					String seconds = s + " second" + (s != 1 ? "s" : "");

					if (counter == initialDelay && source != null) {
						sendMessage(source, Component.text("Restarting in ", NamedTextColor.GOLD),
								Component.text("%d:%d:%dh".formatted(h, min, s), NamedTextColor.BLUE));
					}

					// hours
					if (min == 0 && s == 0) {
						sendMessageToAll(MessageType.CHAT, Component.text("Restarting server in ", NamedTextColor.RED)
								.append(Component.text("%s!".formatted(hours), NamedTextColor.DARK_BLUE)));
					}

					// last hour
					if (h == 0) {

						// chat	minutes
						if ((s == 0 && (min == 30 || min == 15 || min == 10 || min == 5 || min == 1)) || (s == 30 && min == 1)) {
							sendMessageToAll(MessageType.CHAT, Component.text("Restarting server in ", NamedTextColor.RED)
									.append(Component.text("%s%s!".formatted(minutes, (s != 0 ? " and %s".formatted(seconds) : "")), NamedTextColor.DARK_BLUE)));
						}
						// chat seconds last minute
						if (min == 0 && (s == 30 || s <= 15)) {
							sendMessageToAll(MessageType.CHAT, Component.text("Restarting server in ", NamedTextColor.RED)
									.append(Component.text("%s!".formatted(seconds), NamedTextColor.DARK_BLUE)));
						}

						// action bar
						if (min < 15) {
							sendMessageToAll(MessageType.ACTION_BAR, Component.text("Restarting server in ", NamedTextColor.RED, TextDecoration.BOLD)
									.append(Component.text((min >= 1 ? (s >= 1 ? "%d:%s%d min".formatted(min, (s < 10 ? "0" : ""), s) : minutes) : seconds), NamedTextColor.DARK_BLUE, TextDecoration.BOLD)));
						}
					}

					counter--;
				})
				.repeat(1, TimeUnit.SECONDS)
				.schedule();
	}
	
	public void cancel() {
		if (this.scheduledTask != null) {
			this.scheduledTask.cancel();
		}
		counter = initialDelay;
		if (source != null) {
			sendMessage(source, Component.text("Der Countdown wurde abgebrochen.", NamedTextColor.GOLD));
		}
	}
	
	private void sendMessageToAll(MessageType type, Component message) {
		this.proxyServer.getAllServers().forEach(server -> {
			if (servers.keySet().stream().anyMatch(key -> key.equalsIgnoreCase(server.getServerInfo().getName())) || servers.containsKey("Proxy-1")) {
				for (Player player : server.getPlayersConnected()) {
					switch (type) {
						case CHAT -> player.sendMessage(DACHUtility.getMessage(message));
						case ACTION_BAR -> player.sendActionBar(message);
					}
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

	private CompletableFuture<List<Map.Entry<String, RegisteredServer>>> getServersToRestart() {
		return CompletableFuture.supplyAsync(() -> {
			List<Map.Entry<String, RegisteredServer>> list = new ArrayList<>(this.servers.entrySet().stream()
					.filter(entry -> {
						// suppliers because e.g. alreadyRestarted should not run for Proxy
						Supplier<Boolean> isProxy = () -> entry.getKey().equalsIgnoreCase("Proxy-1");
						Supplier<Boolean> alreadyRestarted = () -> this.restartedServers.containsKey(entry.getKey());
						Supplier<Boolean> noPlayers = () -> entry.getValue().getPlayersConnected().isEmpty();
						// !whenEmpty: only check !isProxy. whenEmpty: also check after ||
						return !isProxy.get() && (!this.whenEmpty || (!alreadyRestarted.get() && noPlayers.get()));
					}).toList());
			// !whenEmpty: only check if proxy. whenEmpty: check if all servers are empty
			if (this.servers.containsKey("Proxy-1") && (!this.whenEmpty || this.proxyServer.getAllServers().stream().allMatch(server -> server.getPlayersConnected().isEmpty()))) {
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
			this.plugin.getLogger().warn("unable to restart, {} is null", (environmentId == null ? "environmentId" : "") + " " + (accessToken == null ? "accessToken" : ""));
			return;
		}

        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            URI uri = new URI("https://portainer.bteger.dev/api/endpoints/%s/docker/containers/%s/restart".formatted(environmentId, server));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .header("X-API-Key", accessToken)
                    .header("Accept", "*/*")
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 204) {
                sendMessage(source, Component.text("Restarting server '%s' now".formatted(server), NamedTextColor.GOLD));
                return;
            }

            sendMessage(source, Component.text("Failed to restart server '%s'".formatted(server), NamedTextColor.RED));
            this.plugin.getLogger().warn(("Failed to restart server '%s'. Status Code %d".formatted(server, response.statusCode())));

        } catch (IOException | InterruptedException | URISyntaxException e) {
            this.plugin.getLogger().warn("Failed to restart server '%s'".formatted(server));
            e.printStackTrace();
        }
	}

	private enum MessageType {
		CHAT,
		ACTION_BAR
	}
}