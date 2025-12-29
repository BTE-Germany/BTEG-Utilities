package dev.btedach.dachutility.utils.messagebridge;

import com.velocitypowered.api.proxy.Player;
import dev.btedach.dachutility.DACHUtility;

public class ChatPrefixUtil {
    public static String getServerShort(Player player){
        if(player.getCurrentServer().isEmpty()){
            return "";
        }
        return switch (player.getCurrentServer().get().getServer().getServerInfo().getName()) {
            case "Lobby-1" -> "\u058F";
            case "Plot-1" -> "\u0586";
            case "Terra-1" -> "\u0580";
            case "Terra-2" -> "\u0583";
            case "Terra-3" -> "\u0584";
            default -> player.getCurrentServer().get().getServer().getServerInfo().getName();
        };
    }

    public static String getRankPrefix(Player player) {
        return switch (DACHUtility.getInstance().getLuckPermsHook().getHightestRole(player.getUniqueId())){
            case "admin" -> "\ue352";
            case "manager" -> "\ue353";
            case "dev" -> "\ue354";
            case "mod" -> "\ue355";
            case "sup" -> "\ue356";
            case "staff" -> "\ue357";
            case "architect" -> "\ue358";
            case "influencer" -> "\ue359";
            case "donator" -> "\ue360";
            case "builder" -> "\ue361";
            case "trial" -> "\ue362";
            default -> "\ue363";
        };
    }
}
