package de.leander.bteggamemode.util;

import de.leander.bteggamemode.BTEGGamemode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;

public class MessageUtil {

    public static void sendHoverClickMessage(Player player, String content, String hoverText, ClickEvent clickEvent) {
        Component message = Component.text(BTEGGamemode.PREFIX + content);
        Component hover = message.hoverEvent(Component.text(hoverText));
        Component click = hover.clickEvent(clickEvent);
        player.sendMessage(click);
    }


}
