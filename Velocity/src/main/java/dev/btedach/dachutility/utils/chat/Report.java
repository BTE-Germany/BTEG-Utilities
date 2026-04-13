package dev.btedach.dachutility.utils.chat;

import com.velocitypowered.api.proxy.Player;
import dev.btedach.dachutility.DACHUtility;
import dev.btedach.dachutility.utils.WebHookBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static dev.btedach.dachutility.DACHUtility.sendMessage;


public record Report(Player reporter, Player reported, String reason, String webhookUrl, MessageCache messageCache) {

    public void submit() {
        Component staffMessage = this.generateStaffMessage();
        DACHUtility.getInstance().getProxyServer().getAllPlayers().stream()
                .filter(player -> player.hasPermission("group.sup"))
                .forEach(player -> sendMessage(player, staffMessage));

        sendMessage(this.reporter(), Component.text("Vielen Dank für Deinen Report, er wird zeitnah bearbeitet.", NamedTextColor.GRAY));

        try {
            sendWebHook();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendWebHook() throws IOException {
        WebHookBuilder.EmbedObject embedObject = new WebHookBuilder.EmbedObject();
        embedObject.addField("Reporter", this.reporter().getUsername(), false);
        embedObject.addField("Reported", this.reported().getUsername() + " " + this.reported().getUniqueId().toString(), false);
        embedObject.addField("Reason", this.reason(), false);

        WebHookBuilder.newBuilder(this.webhookUrl)
                .addEmbed(embedObject)
                .addEmbed(this.getLastMessagesEmbedObject())
                .buildAndExecute();
    }

    private WebHookBuilder.EmbedObject getLastMessagesEmbedObject() {
        WebHookBuilder.EmbedObject embedObject = new WebHookBuilder.EmbedObject();
        String[] lastMessages = this.messageCache.getLastMessages(this.reported);
        if (lastMessages == null) {
            embedObject.setDescription("Keine Nachrichten gesendet.");
        } else {
            for (int i = 0; i < lastMessages.length; i++) {
                if (lastMessages[i] == null) continue;
                embedObject.addField("Message: " + i, lastMessages[i], false);
            }
        }
        embedObject.setThumbnail("https://crafthead.net/armor/body/" + this.reported.getUniqueId());

        return embedObject;
    }

    private @NotNull Component generateStaffMessage() {
        TextComponent textComponent1 = Component.text("Eine neuer Report ist eingegangen:", NamedTextColor.GRAY);
        TextComponent textComponent2 = Component.text("\nVon: ", NamedTextColor.GRAY);
        TextComponent textComponent3 = Component.text(this.reporter().getUsername(), NamedTextColor.YELLOW);
        TextComponent textComponent4 = Component.text(" -> ", NamedTextColor.GREEN);
        TextComponent textComponent5 = Component.text(this.reported().getUsername(), NamedTextColor.RED);
        TextComponent textComponent6 = Component.text("\nGrund: ", NamedTextColor.GRAY);
        TextComponent textComponent7 = Component.text(this.reason(), NamedTextColor.YELLOW);

        return textComponent1
                .append(textComponent2)
                .append(textComponent3)
                .append(textComponent4)
                .append(textComponent5)
                .append(textComponent6)
                .append(textComponent7);
    }
}
