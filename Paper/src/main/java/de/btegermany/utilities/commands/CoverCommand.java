package de.btegermany.utilities.commands;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.regions.Region;
import de.btegermany.utilities.BTEGUtilities;
import de.btegermany.utilities.util.CommandWithBackup;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CoverCommand extends CommandWithBackup implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player) || !(command.getName().equalsIgnoreCase("cover") || command.getName().equalsIgnoreCase("/cover"))) {
            return true;
        }
        if (!player.hasPermission("bteg.builder")) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cNo permission for //cover");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("undo")){
                this.pasteBackup();
                return true;
        }


        Region region;
        LocalSession localSession;
        try {
            localSession = WorldEdit.getInstance().getSessionManager().findByName(player.getName());
            if(localSession == null) {
                return true;
            }
            region = localSession.getSelection(localSession.getSelectionWorld());
        } catch (IncompleteRegionException e) {
            e.printStackTrace();
            player.sendMessage(BTEGUtilities.PREFIX + "§cPlease select a WorldEdit selection!");
            return true;
        }
        this.saveBackup(player, region);
        EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder().world(region.getWorld()).maxBlocks(-1).build();
        localSession.remember(editSession);

        player.chat("//gmask 0");
        player.chat("//re <95:7 159:9");
        player.chat("//re >95:7 159:9");
        player.chat("//re <95:8 35:7");
        player.chat("//re >95:8 35:7");
        player.chat("//gmask");
        player.chat("//side light_gray_stained_glass gray_wool n n air");
        player.chat("//side light_gray_stained_glass gray_wool e n air");
        player.chat("//side light_gray_stained_glass gray_wool s n air");
        player.chat("//side light_gray_stained_glass gray_wool w n air");
        player.chat("//side gray_stained_glass cyan_terracotta n n air");
        player.chat("//side gray_stained_glass cyan_terracotta e n air");
        player.chat("//side gray_stained_glass cyan_terracotta s n air");
        player.chat("//side gray_stained_glass cyan_terracotta w n air");


        return true;
    }
}
