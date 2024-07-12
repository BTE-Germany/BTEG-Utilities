package de.leander.bteggamemode.commands;

import de.leander.bteggamemode.BTEGGamemode;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class NormsCommand  implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!(sender instanceof Player)) { return true; }
        Player player = (Player) sender;
        if(!player.hasPermission("bteg.norms")){
            player.sendMessage(BTEGGamemode.prefix + "§cNo permission for /norms");
            return true;
        }
        World world = Bukkit.getWorld("normen-hub");
        if(player.getWorld().equals(world)){
            player.sendMessage(BTEGGamemode.prefix + "§cYou are already in the Normen-Hub");
            return true;
        }
        player.teleport(new Location(world, 0.5, 5, 0.5));

        return true;
    }
}
