package de.btegermany.utilities.util;

import de.btegermany.utilities.BTEGUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;

public class MessageUtil {

    public static void sendHoverClickMessage(Player player, String content, String hoverText, ClickEvent clickEvent) {
        Component message = Component.text(BTEGUtilities.PREFIX + content);
        Component hover = message.hoverEvent(Component.text(hoverText));
        Component click = hover.clickEvent(clickEvent);
        player.sendMessage(click);
    }


}
