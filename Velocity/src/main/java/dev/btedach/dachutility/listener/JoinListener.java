package dev.btedach.dachutility.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import dev.btedach.dachutility.DACHUtility;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.awt.*;
import java.util.Objects;

public class JoinListener {

    public static DACHUtility instance = DACHUtility.getInstance();

    @Subscribe
    public void onLoginEvent(LoginEvent event) {
        DACHUtility.getInstance().getServer().getAllPlayers().forEach(player -> {player.sendMessage(Component.text("֍ "+ NamedTextColor.GRAY +event.getPlayer().getUsername()).toBuilder().build());});
        TextChannel channel = Objects.requireNonNull(instance.jda.getGuildById(instance.getMainServerID())).getTextChannelById(instance.getAllChannelID());
        EmbedBuilder builder = new EmbedBuilder();
        builder.setThumbnail("https://mc-heads.net/avatar/"+event.getPlayer().getUniqueId()+"/20");
        builder.setDescription("`"+event.getPlayer().getUsername() +"` hat das Minecraft Netzwerk **betreten**");
        builder.setColor(Color.decode("#a2ffa2"));
        assert channel != null;
        channel.sendMessageEmbeds(builder.build()).queue();
        instance.playerCount++;
        channel.getManager().setTopic("Server Online - Spieler: "+instance.playerCount).queue();
    }
}
