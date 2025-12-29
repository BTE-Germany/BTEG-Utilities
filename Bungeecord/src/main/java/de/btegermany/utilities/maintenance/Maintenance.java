package de.btegermany.utilities.maintenance;

import net.md_5.bungee.api.config.ServerInfo;

import java.time.ZonedDateTime;
import java.util.Set;

public record Maintenance (String name, Set<ServerInfo> servers, ZonedDateTime time, boolean proxy) {}