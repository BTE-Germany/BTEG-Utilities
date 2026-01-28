package dev.btedach.dachutility.utils.messagebridge;

import com.velocitypowered.api.proxy.Player;
import dev.btedach.dachutility.DACHUtility;
import dev.btedach.dachutility.utils.Constants;
import dev.btedach.dachutility.utils.WebHookBuilder;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static dev.btedach.dachutility.DACHUtility.sendMessage;

@Getter
public class ReportUtil {
    private final Player reporter;

    private final Player reported;

    private final String reason;

    public ReportUtil(Player reporter, Player reported, String reason){
        this.reporter = reporter;
        this.reported = reported;
        this.reason = reason;
    }

    public void report(){
        DACHUtility.getInstance().getProxyServer().getAllPlayers().stream()
                .filter(player -> player.hasPermission("group.sup"))
                .forEach(player -> sendMessage(player, generateStaffMessage().asComponent()));
        sendMessage(this.getReporter(), generateReportedMessage().asComponent());
        try {
            sendWebHook();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendWebHook() throws IOException {
        WebHookBuilder.EmbedObject embedObject = new WebHookBuilder.EmbedObject();
        embedObject.addField("Reporter", this.getReporter().getUsername(), false);
        embedObject.addField("Reported", this.getReported().getUsername() + " " + this.getReporter().getUniqueId().toString(), false);
        embedObject.addField("Reason", this.getReason(), false);
        WebHookBuilder.newBuilder("https://discord.com/api/webhooks/1146531133375397888/UfJO2ovyviJhhWDZh7VMlHoC2-U5UJikqB1SFQbbHE4BQCf2Ym8Um0NbSMLeSphi0OuA")
                .addEmbed(embedObject)
                .addEmbed(getEmbedObject())
                .buildAndExecute();
    }

    private WebHookBuilder.EmbedObject getEmbedObject(){
        WebHookBuilder.EmbedObject embedObject = new WebHookBuilder.EmbedObject();
        String[] sendedMessages = MessageCache.getLastMessages(this.reported);
        if(sendedMessages == null){
            embedObject.setDescription("Keine Nachrichten gesendet.");
        }else{
            for(int i = 0; i < sendedMessages.length; i++){
                if(sendedMessages[i] == null) continue;
                embedObject.addField("Message: "+i, sendedMessages[i], false);
            }
        }
        embedObject.setThumbnail("https://crafthead.net/armor/body/"+this.reported.getUniqueId());

        return embedObject;

    }

    public Player getReporter() {
        return reporter;
    }

    public Player getReported() {
        return reported;
    }

    public String getReason() {
        return reason;
    }

    private @NotNull ComponentLike generateReportedMessage(){
        TextComponent textComponent = Component.text("Vielen Dank für Deinen Report, er wird zeitnah bearbeitet.", NamedTextColor.GRAY);
        return Constants.reportPrefix.append(textComponent);
    }
    private @NotNull ComponentLike generateStaffMessage(){

        TextComponent textComponent1 = Component.text("Eine neuer Report ist eingegangen: \n", NamedTextColor.GRAY);
        TextComponent textComponent2 = Component.text("Von: ", NamedTextColor.GRAY);
        TextComponent textComponent3 = Component.text(this.getReporter().getUsername(), NamedTextColor.YELLOW);
        TextComponent textComponent4 = Component.text(" -> ", NamedTextColor.GREEN);
        TextComponent textComponent5 = Component.text(this.getReported().getUsername(), NamedTextColor.RED);
        TextComponent textComponent6 = Component.text("Grund: ", NamedTextColor.GRAY);
        TextComponent textComponent7 = Component.text(this.getReason(), NamedTextColor.YELLOW);

        return Constants.reportPrefix.append(textComponent1).append(textComponent2).append(textComponent3).append(textComponent4).append(textComponent5).append(textComponent6).append(textComponent7);
    }
}
