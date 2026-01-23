package dev.btedach.dachutility.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import static dev.btedach.dachutility.DACHUtility.sendMessage;

public class Discord implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        TextComponent textComponent = Component.text("Klicke hier, um auf unseren Discord Server zu gelangen.")
                        .hoverEvent(HoverEvent.showText(Component.text("Klicke hier!")))
                                .clickEvent(ClickEvent.openUrl("https://discord.gg/btegermany"))
                                        .toBuilder().build();
        sendMessage(source, textComponent);
    }
}
