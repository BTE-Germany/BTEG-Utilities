package de.leander.bteggamemode.commands;

import de.leander.bteggamemode.BTEGGamemode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CleanUpCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { return true; }
        Player player = (Player) sender;
        if (command.getName().equalsIgnoreCase("cleanup")||command.getName().equalsIgnoreCase("/cleanup")) {
            if (player.hasPermission("bteg.builder")) {
                player.chat("//re 4,8,9,10,11,12,13,17,18,31,37,38,39,40,82,86,106,175 0");
            }else{
                player.sendMessage(BTEGGamemode.prefix + "§cNo permission for //cleanup");
            }
        }
        return true;
    }



}
