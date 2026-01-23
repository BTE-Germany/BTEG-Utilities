package dev.btedach.dachutility.utils;

import com.velocitypowered.api.proxy.ProxyServer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Utils {

    public static List<String> getServersTabCompletion(String input, ProxyServer proxyServer) {
        List<String> result = new ArrayList<>();

        String lastCharacter = !input.isEmpty() ? input.substring(input.length() - 1) : "";
        switch (lastCharacter) {
            case "", "," -> {
                if (lastCharacter.isEmpty()) {
                    result.add(input + "all");
                }
                result.addAll(Utils.getServersTabCompletionAbbr(false, input, proxyServer));
            }
            case "-" -> result.addAll(Utils.getServersTabCompletionAbbr(true, input, proxyServer));
            default -> {
                int indexComma = input.lastIndexOf(",");
                String inputRelevant = input.substring(indexComma == -1 ? 0 : (indexComma + 1 == input.length() ? indexComma : indexComma + 1));
                String inputStart = input.substring(0, input.length() - inputRelevant.length());
                if (inputRelevant.matches("\\d+")) {
                    result.addAll(Utils.getServersTabCompletionAbbr(true, input.concat("-"), proxyServer));
                    break;
                }
                if ("all".startsWith(inputRelevant.toLowerCase())) {
                    result.add(inputStart + "all");
                }
                if ("plot".startsWith(inputRelevant.toLowerCase())) {
                    result.add(inputStart + "plot");
                }
                if ("lobby".startsWith(inputRelevant.toLowerCase())) {
                    result.add(inputStart + "lobby");
                }
                if ("proxy".startsWith(inputRelevant.toLowerCase())) {
                    result.add(inputStart + "proxy");
                }
            }
        }

        return result;
    }

    private static Set<String> getServersTabCompletionAbbr(boolean onlyTerraServers, @Nullable String input, ProxyServer proxyServer) {
        boolean appendToInput = input != null;
        Set<String> serversShort = new HashSet<>();

        String beginning = "terra-";
        proxyServer.getAllServers().stream()
                .map(server -> server.getServerInfo().getName())
                .filter(name -> name.toLowerCase().startsWith(beginning))
                .forEach(name -> serversShort.add((appendToInput ? input : "") + name.substring(beginning.length())));

        if (onlyTerraServers) {
            return serversShort;
        }

        serversShort.add((appendToInput ? input : "") + "plot");
        serversShort.add((appendToInput ? input : "") + "lobby");
        serversShort.add((appendToInput ? input : "") + "proxy");
        return serversShort;
    }

}
