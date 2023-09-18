package de.leander.bteggamemode.commands;

import de.leander.bteggamemode.BTEGGamemode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class RegionFileCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!(sender instanceof Player)) { return true; }
        Player player = (Player) sender;
        if(!player.hasPermission("bteg.regionfile")){
            player.sendMessage(BTEGGamemode.prefix + "§cNo permission for /regionfile");
            return true;
        }


        int x = player.getLocation().getBlockX();
        int z = player.getLocation().getBlockZ();
        boolean isVanilla = true;
        if(new File(player.getWorld().getWorldFolder().getAbsolutePath() + "region3d").exists() || (args.length == 1 && args[0].equalsIgnoreCase("cc"))) {
            isVanilla = false;
        }
        int chunkX = (int) x >> 4;
        int chunkZ = (int) z >> 4;
        int regionX = chunkX >> 5;
        int regionZ = chunkZ >> 5;

        if(isVanilla){
            String regionFile = "r."+regionX+"."+regionZ+".mca";

            Component message = Component.text(BTEGGamemode.prefix + "Region file: §9" +regionFile);
            Component hover = message.hoverEvent(Component.text("§7Click to copy §9" + regionFile + " §7to clipboard"));
            Component click = hover.clickEvent(ClickEvent.copyToClipboard(regionFile));

            player.sendMessage(click);
        }else{
            String region3dFile = (regionX*2)+"."+"0"+"."+(regionZ*2)+".3dr";
            String region2dFile = regionX+"."+regionZ+".2dr";

            Component message1 = Component.text(BTEGGamemode.prefix + "Region3d file: §9" +region3dFile);
            Component hover1 = message1.hoverEvent(Component.text("§7Click to copy §9" + region3dFile + " §7to clipboard"));
            Component click1 = hover1.clickEvent(ClickEvent.copyToClipboard(region3dFile));

            Component message2 = Component.text(BTEGGamemode.prefix + "Region2d file: §9" +region2dFile);
            Component hover2 = message2.hoverEvent(Component.text("§7Click to copy §9" + region2dFile + " §7to clipboard"));
            Component click2 = hover2.clickEvent(ClickEvent.copyToClipboard(region2dFile));

            player.sendMessage(click1);
            player.sendMessage(click2);

        }

        return true;
    }
}
