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
                player.chat("//re 18,85,oak_wood,azalea_leaves,birch_leaves,birch_fence,spruce_fence,spruce_leaves,oak_log,spruce_fence_gate,stripped_spruce_log,grass,birch_log,lilac,brown_mushroom,dandelion,peony,rose_bush,poppy,red_mushroom 0");
            }else{
                player.sendMessage(BTEGGamemode.prefix + "§cNo permission for //cleanup");
            }
        }
        return true;
    }



}
