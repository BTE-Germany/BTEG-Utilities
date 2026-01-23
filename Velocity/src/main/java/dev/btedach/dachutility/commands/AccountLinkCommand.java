package dev.btedach.dachutility.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import dev.btedach.dachutility.DACHUtility;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import static dev.btedach.dachutility.DACHUtility.sendMessage;

public class AccountLinkCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            sendMessage(invocation.source(), Component.text("Dieser Befehl kann nur von einem Spieler ausgeführt werden.", NamedTextColor.RED));
            return;
        }

        String url = DACHUtility.getInstance().generateUrl(player);

        sendMessage(player, Component.text("Verbinde deinen Account, indem du auf den folgenden Link klickst:", NamedTextColor.GRAY));

        Component component = Component.text(url, NamedTextColor.GREEN)
                        .decorate(TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl(url));

        sendMessage(player, component);
    }
}
