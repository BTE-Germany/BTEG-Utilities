package dev.btedach.dachutility.utils;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.btedach.dachutility.DACHUtility;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Servers {

    public static Map<String, RegisteredServer> fromInput(String... serversArgs) {

        Map<String, RegisteredServer> servers = new HashMap<>();
        for (RegisteredServer server : DACHUtility.getInstance().getProxy().getAllServers()) {
            servers.put(server.getServerInfo().getName(), server);
        }
        Map<String, RegisteredServer> serversRes = new HashMap<>();

        for(String serverName : serversArgs) {
            serverName = Character.toUpperCase(serverName.charAt(0)) + serverName.toLowerCase().substring(1);

            if(serverName.equalsIgnoreCase("all")) {
                serversRes.putAll(servers);
                serversRes.put("Proxy-1", null);
                break;
            }

            String[] filters = new String[] {serverName + "-1", "Terra-" + serverName};

            for(String f : filters) {
                Optional <Map.Entry<String, RegisteredServer>> terraServerOptional = servers.entrySet().stream().filter(entry -> entry.getKey().equalsIgnoreCase(f)).findFirst();
                if(terraServerOptional.isPresent()) {
                    serversRes.put(terraServerOptional.get().getKey(), terraServerOptional.get().getValue());
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
                    int finalI = i;
                    Optional <Map.Entry<String, RegisteredServer>> serverOptional = servers.entrySet().stream().filter(entry -> entry.getKey().equalsIgnoreCase("Terra-" + finalI)).findFirst();
                    serverOptional.ifPresent(server -> serversRes.put(server.getKey(), server.getValue()));
                }

            }
        }
        return serversRes;
    }

}
