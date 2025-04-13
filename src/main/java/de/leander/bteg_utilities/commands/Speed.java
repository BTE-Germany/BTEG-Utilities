package de.leander.bteg_utilities.commands;

import de.leander.bteg_utilities.BTEGUtilities;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Speed implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player) || !cmd.getName().equalsIgnoreCase("speed")) {
            return true;
        }
        if (!player.hasPermission("bteg.speed")) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cNo permission for /speed");
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(BTEGUtilities.PREFIX + "Usage: /speed <0-5>");
            return true;
        }

        String speed = args[0];
        if (speed.equals("n") || speed.equals("normal")) {
            player.setWalkSpeed(0.2F);
            player.setFlySpeed(0.1F);
            player.sendMessage(BTEGUtilities.PREFIX + "New speed: Normal");
            return true;
        } else if (speed.contains(",") || speed.contains(".") || speed.matches("1") || speed.matches("2") || speed.matches("3") || speed.matches("4") || speed.matches("5")) {

            float newSpeed = Float.parseFloat(speed.replace(",", "."));
            if (newSpeed < 0 || newSpeed > 5) {
                player.sendMessage(BTEGUtilities.PREFIX + "Usage: /speed <0-5>");
                return true;
            }

            player.setWalkSpeed(newSpeed / 5);
            player.setFlySpeed(newSpeed / 10);
            player.sendMessage(BTEGUtilities.PREFIX + "New speed: " + newSpeed);

        } else {
            player.sendMessage(BTEGUtilities.PREFIX + "Usage: /speed <0-5>");
            return true;
        }

        return true;
    }


    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender.hasPermission("bteg.speed") || args.length != 1) {
            return Collections.emptyList();
        }

        List<String> list = new ArrayList<>();
        list.add("1");
        list.add("2");
        list.add("3");
        list.add("4");
        list.add("5");
        return list;
    }
}
