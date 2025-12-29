package de.btegermany.utilities;

import java.util.HashMap;
import java.util.Map;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;

public class Servers {

	public static Map<String, ServerInfo> fromInput(String... serversArgs) {
		
		Map<String, ServerInfo> serversRes = new HashMap<>();
		for(String serverName : serversArgs) {
			serverName = Character.toUpperCase(serverName.charAt(0)) + serverName.toLowerCase().substring(1);
			Map<String, ServerInfo> servers = ProxyServer.getInstance().getServers();
			
			if(serverName.equalsIgnoreCase("all")) {
				serversRes.putAll(servers);
				serversRes.put("Proxy-1", null);
				break;
			}
			
			String[] filters = new String[] {serverName + "-1", "Terra-" + serverName};
			
			for(String f : filters) {
				if(servers.containsKey(f)) {
					serversRes.put(f, servers.get(f));
				} else if(f.equals("Proxy-1")) {
					serversRes.put(f, null);
				}
			}
			
			if(serverName.length() == 3 && serverName.charAt(1) == '-') {
				String[] range = serverName.split("-");
				int start = Integer.parseInt(range[0]);
				int end = Integer.parseInt(range[1]);
				if(start > end) {
					int temp = start;
					start = end;
					end = temp;
				}
				for(int i = start; i <= end; i++) {
					if(servers.containsKey("Terra-" + i)) {
						serversRes.put("Terra-" + i, servers.get("Terra-" + i));
					}
				}
				
			}
		}
		return serversRes;
	}
	
}
