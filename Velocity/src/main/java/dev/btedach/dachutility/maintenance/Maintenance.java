package dev.btedach.dachutility.maintenance;

import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.time.ZonedDateTime;
import java.util.Set;

public record Maintenance(String name, Set<RegisteredServer> servers, ZonedDateTime time, boolean proxy) {}
