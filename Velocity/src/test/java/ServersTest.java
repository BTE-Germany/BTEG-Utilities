import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ServersTest {

    @Test
    public void test() {
        Map<String, RegisteredServer> networkServers = new HashMap<>();
        networkServers.put("lobby-1", null);
        networkServers.put("plot-1", null);
        networkServers.put("terra-1", null);
        networkServers.put("terra-2", null);
        networkServers.put("terra-3", null);

        Map<String, RegisteredServer> servers = this.fromInput(networkServers, "all");
        Assertions.assertTrue(servers.containsKey("Proxy-1"));
        Assertions.assertTrue(servers.containsKey("lobby-1"));
        Assertions.assertTrue(servers.containsKey("plot-1"));
        Assertions.assertTrue(servers.containsKey("terra-1"));
        Assertions.assertTrue(servers.containsKey("terra-2"));
        Assertions.assertTrue(servers.containsKey("terra-3"));
        servers = fromInput(networkServers, "1-3", "Plot");
        Assertions.assertFalse(servers.containsKey("Proxy-1"));
        Assertions.assertFalse(servers.containsKey("lobby-1"));
        Assertions.assertTrue(servers.containsKey("plot-1"));
        Assertions.assertTrue(servers.containsKey("terra-1"));
        Assertions.assertTrue(servers.containsKey("terra-2"));
        Assertions.assertTrue(servers.containsKey("terra-3"));
        servers = fromInput(networkServers, "2", "3", "Proxy");
        Assertions.assertTrue(servers.containsKey("Proxy-1"));
        Assertions.assertFalse(servers.containsKey("lobby-1"));
        Assertions.assertFalse(servers.containsKey("plot-1"));
        Assertions.assertFalse(servers.containsKey("terra-1"));
        Assertions.assertTrue(servers.containsKey("terra-2"));
        Assertions.assertTrue(servers.containsKey("terra-3"));
        servers = fromInput(networkServers, "2-3", "Lobby");
        Assertions.assertFalse(servers.containsKey("Proxy-1"));
        Assertions.assertTrue(servers.containsKey("lobby-1"));
        Assertions.assertFalse(servers.containsKey("plot-1"));
        Assertions.assertFalse(servers.containsKey("terra-1"));
        Assertions.assertTrue(servers.containsKey("terra-2"));
        Assertions.assertTrue(servers.containsKey("terra-3"));

        servers = fromInput(networkServers, "1-3", "plot");
        Assertions.assertFalse(servers.containsKey("Proxy-1"));
        Assertions.assertFalse(servers.containsKey("lobby-1"));
        Assertions.assertTrue(servers.containsKey("plot-1"));
        Assertions.assertTrue(servers.containsKey("terra-1"));
        Assertions.assertTrue(servers.containsKey("terra-2"));
        Assertions.assertTrue(servers.containsKey("terra-3"));
        servers = fromInput(networkServers, "2", "3", "proxy");
        Assertions.assertTrue(servers.containsKey("Proxy-1"));
        Assertions.assertFalse(servers.containsKey("lobby-1"));
        Assertions.assertFalse(servers.containsKey("plot-1"));
        Assertions.assertFalse(servers.containsKey("terra-1"));
        Assertions.assertTrue(servers.containsKey("terra-2"));
        Assertions.assertTrue(servers.containsKey("terra-3"));
        servers = fromInput(networkServers, "2-3", "lobby");
        Assertions.assertFalse(servers.containsKey("Proxy-1"));
        Assertions.assertTrue(servers.containsKey("lobby-1"));
        Assertions.assertFalse(servers.containsKey("plot-1"));
        Assertions.assertFalse(servers.containsKey("terra-1"));
        Assertions.assertTrue(servers.containsKey("terra-2"));
        Assertions.assertTrue(servers.containsKey("terra-3"));
    }

    private Map<String, RegisteredServer> fromInput(Map<String, RegisteredServer> servers, String... serversArgs) {
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
                Optional<Map.Entry<String, RegisteredServer>> terraServerOptional = servers.entrySet().stream().filter(entry -> entry.getKey().equalsIgnoreCase(f)).findFirst();
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
