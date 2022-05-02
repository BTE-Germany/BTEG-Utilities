package de.leander.bteggamemode.events;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import static de.leander.bteggamemode.commands.RestartTimer.sendMessage;
import static de.leander.bteggamemode.commands.RestartTimer.shortInteger;

public class DailyRestart {


    private int timeleft = -1;
    private boolean ausgefuehrt = false;


    public DailyRestart(Plugin plugin){

        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {

                Calendar cal =Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));

                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                String time = sdf.format(cal.getTime());
                String[] times = time.split(":");

                int hour = Integer.parseInt(times[0]);
                int minute = Integer.parseInt(times[1]);

                if (hour == 5 && minute == 0 && !ausgefuehrt) {
                    timeleft = 305;
                    ausgefuehrt = true;
                }
                if(timeleft < 305 && timeleft > 0) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§c§lServerrestart in " + shortInteger(timeleft)));
                    }
                }

                if(timeleft == 300){ sendMessage(timeleft); }
                if(timeleft == 120){ sendMessage(timeleft); }
                if(timeleft == 60){ sendMessage(timeleft); }
                if(timeleft == 30){ sendMessage(timeleft); }
                for(int i = 15; i > 0; i--) {
                    if(timeleft == i){ sendMessage(timeleft); }
                }
                timeleft--;
                if(timeleft == 0){
                    timeleft = -1;
                    ausgefuehrt = false;
                    Bukkit.getServer().spigot().restart();
                }
            }
        };

        runnable.runTaskTimer(plugin, 40,20);


    }



}
