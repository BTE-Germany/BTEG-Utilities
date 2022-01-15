package de.leander.bteggamemode.commands;

import de.leander.bteggamemode.BTEGGamemode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class Speed implements CommandExecutor, Listener {

    private static Plugin plugin;

    public Speed(JavaPlugin pPlugin) {
        plugin = pPlugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) { return true; }
        Player player = (Player) sender;
        if (cmd.getName().equalsIgnoreCase("speed")) {
            if (player.hasPermission("bteg.speed")) {
                if (args.length == 0 || args.length > 1) {
                    player.sendMessage("§b§lBTEG §7» §7Usage: /speed <0-5>");
                } else if (args.length == 1) {
                    String speed = args[0];
                    if (speed.equals("n") || speed.equals("normal")) {
                        player.setWalkSpeed(0.2F);
                        player.setFlySpeed(0.1F);
                        player.sendMessage("§b§lBTEG §7» §7New speed: Normal");
                        return true;
                    } else if (speed.contains(",") || speed.contains(".") || speed.matches("1") || speed.matches("2") || speed.matches("3") || speed.matches("4") || speed.matches("5")) {


                        Float newSpeed = Float.parseFloat(speed.replace(",", "."));
                        if (newSpeed < 0 || newSpeed > 5) {
                            player.sendMessage("§b§lBTEG §7» §7Usage: /speed <0-5>");
                            return true;
                        }

                        player.setWalkSpeed(newSpeed / 5);
                        player.setFlySpeed(newSpeed / 10);
                        player.sendMessage("§b§lBTEG §7» §7New speed: " + newSpeed + "");

                    } else {
                        player.sendMessage("§b§lBTEG §7» §7Usage: /speed <0-5>");
                        return true;
                    }


                }
            }


        }
        return true;
    }



}
