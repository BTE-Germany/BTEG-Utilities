package de.leander.bteggamemode.commands;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.leander.bteggamemode.cloudnet.RestartService;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

public class RestartTimer implements CommandExecutor  {

    public static int timeleft = 120;
    private static Plugin plugin;
    BukkitRunnable runnable;


    public RestartTimer(JavaPlugin pPlugin) {
        plugin = pPlugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) { return true; }
        Player player = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("bteg")) {

            if (player.hasPermission("bteg.restart")) {

                if(args.length == 0) {
                    player.sendMessage("§b§lBTEG §7» §7Example commands:");
                    player.sendMessage("§b§lBTEG §7» §7/bteg restart <Seconds[5min=300]> - Restarts a server - Standard: §82 minutes");
                    player.sendMessage("§b§lBTEG §7» §7/terraform <Height>");
                    player.sendMessage("§b§lBTEG §7» §7//side <Block-ID> <Block-ID> <Direction[n,e,s,w]>");
                }
                else if(args.length == 1 || args.length == 2){

                        if(args[0].matches("cancel") || args[0].matches("stop")){
                            runnable.cancel();
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                p.sendMessage("§b§lBTEG §7» §4Restart of " + Bukkit.getServerName() + " canceled!");
                                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
                            }
                            return true;
                        }else if(args[0].matches("restart")) {
                            if(args.length == 2){
                                timeleft = Integer.parseInt(args[1]);
                            }
                            runnable = new BukkitRunnable() {
                                @Override
                                public void run() {
                                    for (Player p : Bukkit.getOnlinePlayers()) {
                                        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§c§lServerrestart in "+shortInteger(timeleft)));
                                    }
                                    if(timeleft == 1800){ sendMessage(timeleft); }
                                    if(timeleft == 900){ sendMessage(timeleft); }
                                    if(timeleft == 600){ sendMessage(timeleft); }
                                    if(timeleft == 300){ sendMessage(timeleft); }
                                    if(timeleft == 120){ sendMessage(timeleft); }
                                    if(timeleft == 60){ sendMessage(timeleft); }
                                    if(timeleft == 30){ sendMessage(timeleft); }
                                    for(int i = 15; i > 0; i--) {
                                        if(timeleft == i){ sendMessage(timeleft); }
                                    }
                                    timeleft--;
                                    if(timeleft == 0){
                                        Bukkit.getServer().spigot().restart();
                                    }
                                }
                            };
                            player.sendMessage("§b§lBTEG §7» §7Planned restart in §4"+shortInteger(timeleft) +"§7 hours.");
                            runnable.runTaskTimer(plugin, 40,20);

                        }
                        else if(args[0].matches("warp")) {
                            player.chat("/nwarp");
                        }
                        else {
                            player.sendMessage("§b§lBTEG §7» §4Wrong usage. /bteg restart");
                        }
                }
                else if(args.length > 1){
                    player.sendMessage("§b§lBTEG §7» §4Wrong usage. /bteg restart");
                }

            }
            else {
                player.sendMessage("§b§lBTEG §7» §4No permission for §8/bteg restart§7!");
        }
    }



        return true;
}

    void sendMessage(int pZeit){
        if (pZeit == 60){
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage("§b§lBTEG §7» §c" + Bukkit.getServerName() + " restarts in §l1 minute!");
            }
        }else if(pZeit == 1){
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage("§b§lBTEG §7» §c" + Bukkit.getServerName() + " restarts in §l1 second!");
                sendPlayerToServer(p, "Lobby-1");
            }
        }else if(1 < pZeit && pZeit < 60){
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage("§b§lBTEG §7» §c" + Bukkit.getServerName() + " restarts in §l"+pZeit+" seconds!");
            }
        } else {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage("§b§lBTEG §7» §c" + Bukkit.getServerName() + " restarts in §l"+pZeit/60+" minutes!");
            }
        }
    }

    public String shortInteger(int duration) {
        String string = "";
        int hours = 0;
        int minutes = 0;
        int seconds = 0;
        if (duration / 60 / 60 / 24 >= 1) {
            duration -= duration / 60 / 60 / 24 * 60 * 60 * 24;
        }
        if (duration / 60 / 60 >= 1) {
            hours = duration / 60 / 60;
            duration -= duration / 60 / 60 * 60 * 60;
        }
        if (duration / 60 >= 1) {
            minutes = duration / 60;
            duration -= duration / 60 * 60;
        }
        if (duration >= 1)
            seconds = duration;
        if (hours <= 9) {
            string = String.valueOf(string) + "0" + hours + ":";
        } else {
            string = String.valueOf(string) + hours + ":";
        }
        if (minutes <= 9) {
            string = String.valueOf(string) + "0" + minutes + ":";
        } else {
            string = String.valueOf(string) + minutes + ":";
        }
        if (seconds <= 9) {
            string = String.valueOf(string) + "0" + seconds;
        } else {
            string = String.valueOf(string) + seconds;
        }
        return string;
    }

    public static void sendPlayerToServer(Player player, String server) {
        player.sendMessage("§b§lBTEG §8» §aSending to §6" + server);
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            out.writeUTF("Connect");
            out.writeUTF(server);
            player.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());
            b.close();
            out.close();
        }
        catch (Exception e) {
            player.sendMessage(ChatColor.RED+"Error when trying to connect to "+server);
        }
    }

}
