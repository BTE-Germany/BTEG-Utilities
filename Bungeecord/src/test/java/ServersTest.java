import net.md_5.bungee.api.config.ServerInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

public class ServersTest {

    @Test
    public void test() {
        Map<String, ServerInfo> servers = fromInput(new String[]{"all"});
        Assertions.assertTrue(servers.containsKey("Proxy-1"));
        Assertions.assertTrue(servers.containsKey("Lobby-1"));
        Assertions.assertTrue(servers.containsKey("Plot-1"));
        Assertions.assertTrue(servers.containsKey("Terra-1"));
        Assertions.assertTrue(servers.containsKey("Terra-2"));
        Assertions.assertTrue(servers.containsKey("Terra-3"));
        Assertions.assertTrue(servers.containsKey("Terra-4"));
        Assertions.assertTrue(servers.containsKey("Terra-5"));
        servers = fromInput(new String[]{"2-4", "Plot", "Lobby"});
        Assertions.assertFalse(servers.containsKey("Proxy-1"));
        Assertions.assertTrue(servers.containsKey("Lobby-1"));
        Assertions.assertTrue(servers.containsKey("Plot-1"));
        Assertions.assertFalse(servers.containsKey("Terra-1"));
        Assertions.assertTrue(servers.containsKey("Terra-2"));
        Assertions.assertTrue(servers.containsKey("Terra-3"));
        Assertions.assertTrue(servers.containsKey("Terra-4"));
        Assertions.assertFalse(servers.containsKey("Terra-5"));
        servers = fromInput(new String[]{"2", "4-5", "Proxy"});
        Assertions.assertTrue(servers.containsKey("Proxy-1"));
        Assertions.assertFalse(servers.containsKey("Lobby-1"));
        Assertions.assertFalse(servers.containsKey("Plot-1"));
        Assertions.assertFalse(servers.containsKey("Terra-1"));
        Assertions.assertTrue(servers.containsKey("Terra-2"));
        Assertions.assertFalse(servers.containsKey("Terra-3"));
        Assertions.assertTrue(servers.containsKey("Terra-4"));
        Assertions.assertTrue(servers.containsKey("Terra-5"));
        servers = fromInput(new String[]{"2-4", "Lobby"});
        Assertions.assertFalse(servers.containsKey("Proxy-1"));
        Assertions.assertTrue(servers.containsKey("Lobby-1"));
        Assertions.assertFalse(servers.containsKey("Plot-1"));
        Assertions.assertFalse(servers.containsKey("Terra-1"));
        Assertions.assertTrue(servers.containsKey("Terra-2"));
        Assertions.assertTrue(servers.containsKey("Terra-3"));
        Assertions.assertTrue(servers.containsKey("Terra-4"));
        Assertions.assertFalse(servers.containsKey("Terra-5"));
    }

    public static Map<String, ServerInfo> fromInput(String[] serversArgs) {

        Map<String, ServerInfo> serversRes = new HashMap<>();
        for(String s : serversArgs) {
            s = Character.toUpperCase(s.charAt(0)) + s.toLowerCase().substring(1);
            Map<String, ServerInfo> servers = new HashMap<>();
            servers.put("Lobby-1", null);
            servers.put("Plot-1", null);
            servers.put("Terra-1", null);
            servers.put("Terra-2", null);
            servers.put("Terra-3", null);
            servers.put("Terra-4", null);
            servers.put("Terra-5", null);

            if(s.equalsIgnoreCase("all")) {
                serversRes.putAll(servers);
                serversRes.put("Proxy-1", null);
                break;
            }

            String[] filters = new String[] {s + "-1", "Terra-" + s};

            for(String f : filters) {
                if(servers.containsKey(f)) {
                    serversRes.put(f, servers.get(f));
                } else if(f.equals("Proxy-1")) {
                    serversRes.put(f, null);
                }
            }

            if(s.length() == 3 && s.charAt(1) == '-') {
                String[] range = s.split("-");
                for(int i = Integer.parseInt(range[0]); i <= Integer.parseInt(range[1]); i++) {
                    if(servers.containsKey("Terra-" + i)) {
                        serversRes.put("Terra-" + i, servers.get("Terra-" + i));
                    }
                }

            }
        }
        return serversRes;
    }

}
