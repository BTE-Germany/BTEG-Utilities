package de.btegermany.utilities.commands;

import de.btegermany.utilities.BTEGUtilities;
import de.btegermany.utilities.util.CommandSound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BuildSoundCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }
        if (args.length != 0) {
            player.sendMessage(BTEGUtilities.PREFIX + "Usage: /buildsound");
            return true;
        }

        boolean enabled = CommandSound.toggle(player);
        player.sendMessage(BTEGUtilities.PREFIX + "Command sounds are now " + (enabled ? "§aon" : "§coff") + "§7.");
        return true;
    }
}
