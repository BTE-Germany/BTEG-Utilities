package de.leander.bteg_utilities.commands;

import de.leander.bteg_utilities.BTEGUtilities;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class NormsCommand  implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!(sender instanceof Player player)) {
            return true;
        }
        if(!player.hasPermission("bteg.norms")){
            player.sendMessage(BTEGUtilities.PREFIX + "§cNo permission for /norms");
            return true;
        }
        World world = Bukkit.getWorld("normen-hub");
        if(player.getWorld().equals(world)){
            player.sendMessage(BTEGUtilities.PREFIX + "§cYou are already in the Normen-Hub");
            return true;
        }
        player.teleport(new Location(world, 0.5, 5, 0.5));

        return true;
    }
}
