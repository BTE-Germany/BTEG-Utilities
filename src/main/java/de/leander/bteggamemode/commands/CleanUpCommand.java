package de.leander.bteggamemode.commands;

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
            if (player.hasPermission("bteg.cleanup")) {
                player.chat("//re 1,2,3,4,8,9,10,11,12,13,15,16,17,18,31,37,38,39,40,82,86,106,175 0");
            }
        }
        return true;
    }
}
