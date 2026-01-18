package dev.btedach.dachutility.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import dev.btedach.dachutility.utils.Constants;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

public class Discord implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        TextComponent textComponent = Component.text(Constants.prefix +" Klicke hier, um auf unseren Discord Server zu gelangen.")
                        .hoverEvent(HoverEvent.showText(Component.text("Klicke hier!")))
                                .clickEvent(ClickEvent.openUrl("https://discord.gg/btegermany"))
                                        .toBuilder().build();
        source.sendMessage(textComponent);
    }
}
