package dev.btedach.dachutility.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import dev.btedach.dachutility.DACHUtility;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.awt.*;
import java.util.Objects;
import java.util.UUID;

public class DisconnectListener {
    public static DACHUtility instance = DACHUtility.getInstance();

    @Subscribe
    public void onPlayerChat(DisconnectEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        String name = event.getPlayer().getUsername();
        TextChannel channel = Objects.requireNonNull(instance.jda.getGuildById(instance.getMainServerID())).getTextChannelById(instance.getAllChannelID());

        DACHUtility.getInstance().getServer().getAllPlayers().forEach(player -> player.sendMessage(Component.text("╜ "+ NamedTextColor.GRAY+name)));

        EmbedBuilder builder = new EmbedBuilder();
        //builder.addField(name + " ("+uuid+")", name +" hat das Minecraft Netzwerk verlassen", true);
        builder.setDescription("`"+name +"` hat das Minecraft Netzwerk **verlassen**");
        builder.setThumbnail("https://mc-heads.net/avatar/"+uuid+"/20");
        //builder.setFooter("UUID: "+uuid+" | "+ Calendar.getInstance().getTime());
        builder.setColor(Color.decode("#FF0000"));
        assert channel != null;
        channel.sendMessageEmbeds(builder.build()).queue();

        ChangeServerListener.playerSessionCache.remove(UUID.fromString(uuid));

        if(instance.playerCount>0){
            instance.playerCount--;
            channel.getManager().setTopic("Server Online - Spieler: "+instance.playerCount).queue();
        }
    }
}
