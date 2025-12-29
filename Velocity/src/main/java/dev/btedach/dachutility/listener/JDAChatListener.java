package dev.btedach.dachutility.listener;

import dev.btedach.dachutility.DACHUtility;
import dev.btedach.dachutility.utils.messagebridge.ChatManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Objects;

public class JDAChatListener extends ListenerAdapter {
    private final DACHUtility instance;
    public JDAChatListener(DACHUtility instance) {
        this.instance = instance;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event){
        if(!event.isFromGuild()){
            return;
        }
        if(event.getGuild().getIdLong() == instance.getMainServerID() && event.getChannel().getIdLong() == instance.getAllChannelID()){
            if(Objects.requireNonNull(event.getMember()).getIdLong() == 792917188368400394L) return;
            sendMessage(event);
        }/*else if(event.getGuild().getIdLong() == instance.getMainServerID() && event.getChannel().getIdLong() == instance.getBuilderChatID()){
            if(Objects.requireNonNull(event.getMember()).getIdLong() == 792917188368400394L) return;
            TextComponent textComponent = new TextComponent(instance.getBuilderPrefix());
            sendMessage(event, textComponent, true);
        }*/
    }

    private void sendMessage(MessageReceivedEvent event) {
        TextComponent cDiscord = Component.text("Ὼ ").toBuilder().build();
        //Role role = Objects.requireNonNull(event.getMember()).getRoles().get(0);
        TextComponent cRole = Component.text(roleToName(Objects.requireNonNull(event.getMember()), 0)+" ").toBuilder().build();
        TextComponent cName = Component.text(event.getMember().getEffectiveName().split(" \\[")[0]).color(NamedTextColor.GRAY).toBuilder().build();
        TextComponent cDot = Component.text(": ").color(NamedTextColor.DARK_GRAY).toBuilder().build();
        TextComponent cMessage = Component.text(event.getMessage().getContentRaw()).color(NamedTextColor.WHITE).toBuilder().build();
        TextComponent textComponent = Component.text().append(cDiscord).append(cRole).append(cName).append(cDot).append(cMessage).build();
        /*if(event.getMessage().getAttachments().size() > 0){
            textComponent8.addExtra(" [ANHANG]");
            textComponent8.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder("Click to go the the Message").create()));
            textComponent8.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, event.getMessage().getJumpUrl()));
            textComponent8.setColor(ChatColor.RED);
            textComponent.addExtra(textComponent8);
        }*/
        if(false){
            DACHUtility.getInstance().getServer().getAllPlayers().stream()
                    .filter(player1 -> player1.hasPermission("chat.builder") && ChatManager.isPlayerInBuilderChat(player1))
                    .forEach(player1 -> player1.sendMessage(textComponent));
        }else{
            DACHUtility.getInstance().getServer().getAllPlayers().forEach(player1 -> player1.sendMessage(textComponent));
        }
    }

    private String roleToName(Member member, int i) {
        if(member.getRoles().get(i)==null) return "\ue363";
        return switch (Objects.requireNonNull(member).getRoles().get(i).getName().toLowerCase()) {
            case "admin" -> "\ue352";
            case "manager" -> "\ue353";
            case "developer" -> "\ue354";
            case "moderator" -> "\ue355";
            case "support", "supporter" -> "\ue356";
            case "server team","social-media team","plot-team" -> "\ue357";
            case "official bte 1:1 staff", "builder of the month", "architect lead", "patreon", "twitch subscriber", "server booster" -> roleToName(member, i+1);
            case "architect" -> "\ue358";
            case "influencer" -> "\ue359";
            case "donator" -> "\ue360";
            case "builder" -> "\ue361";
            case "trial" -> "\ue362";
            default -> "\ue363";
        };
    }

    public static void addComponent(TextComponent textComponent, TextComponent ... textComponent1) {
        for(TextComponent component : textComponent1){
            textComponent.append(component);
        }
    }
}
