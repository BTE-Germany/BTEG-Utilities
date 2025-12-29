package dev.btedach.dachutility.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import dev.btedach.dachutility.DACHUtility;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
    import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.awt.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

public class ChangeServerListener {
    public static DACHUtility instance = DACHUtility.getInstance();

    public static ArrayList<UUID> playerSessionCache = new ArrayList<UUID>();

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        String name = event.getPlayer().getUsername();
        TextChannel channel = Objects.requireNonNull(instance.jda.getGuildById(instance.getMainServerID())).getTextChannelById(instance.getAllChannelID());
        EmbedBuilder builder = new EmbedBuilder();
        if(event.getPreviousServer() == null) return;
        builder.setDescription("`"+name +"` hat den Server gewechselt. **" + event.getPreviousServer().getServerInfo().getName()+"** -> **"+event.getOriginalServer().getServerInfo().getName()+"**");
        builder.setThumbnail("https://mc-heads.net/avatar/"+uuid+"/20");

        if(event.getOriginalServer().getServerInfo().getName().startsWith("terra")){
            if(!playerSessionCache.contains(event.getPlayer().getUniqueId())){
                playerSessionCache.add(event.getPlayer().getUniqueId());

                TextComponent textComponent = Component.text("Unser Server nutzt Daten von Drittanbietern. Die entsprechenden Lizenzen findest du unter https://buildthe.earth/credits")
                        .color(NamedTextColor.GREEN).hoverEvent(HoverEvent.showText(Component.text("Klicke hier zum öffnen!")))
                        .clickEvent(ClickEvent.openUrl("https://buildthe.earth/credits"))
                        .toBuilder().build();

                event.getPlayer().sendMessage(textComponent);
            }
        }

        builder.setColor(Color.decode("#3770c9"));
        assert channel != null;
        channel.sendMessageEmbeds(builder.build()).queue();
    }
}
