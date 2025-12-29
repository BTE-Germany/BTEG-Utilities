package de.btegermany.utilities.commands;

import de.btegermany.utilities.BTEGUtilities;
import de.btegermany.utilities.util.MessageUtil;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class RegionFileCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        if (!player.hasPermission("bteg.regionfile")){
            player.sendMessage(BTEGUtilities.PREFIX + "§cNo permission for /regionfile");
            return true;
        }

        int x = player.getLocation().getBlockX();
        int z = player.getLocation().getBlockZ();
        boolean isVanilla = !new File(player.getWorld().getWorldFolder().getAbsolutePath() + "region3d").exists() && (args.length != 1 || !args[0].equalsIgnoreCase("cc"));

        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        int region3dX = chunkX >> 4;
        int regionX = chunkX >> 5;
        int region3dZ = chunkZ >> 4;
        int regionZ = chunkZ >> 5;

        if (isVanilla){
            String regionFile = "r." + regionX + "." + regionZ + ".mca";

            MessageUtil.sendHoverClickMessage(player, "Region file: §9" + regionFile, "§7Click to copy §9" + regionFile + " §7to clipboard", ClickEvent.copyToClipboard(regionFile));
        } else {
            String region3dFile = region3dX + "." + "0" + "." + region3dZ + ".3dr";
            String region2dFile = regionX + "." + regionZ + ".2dr";

            MessageUtil.sendHoverClickMessage(player,"Region3d file: §9" + region3dFile,"§7Click to copy §9" + region3dFile + " §7to clipboard", ClickEvent.copyToClipboard(region3dFile));
            MessageUtil.sendHoverClickMessage(player, "Region2d file: §9" + region2dFile,"§7Click to copy §9" + region2dFile + " §7to clipboard", ClickEvent.copyToClipboard(region2dFile));
        }

        return true;
    }
}
