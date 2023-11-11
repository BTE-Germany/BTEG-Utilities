package de.leander.bteggamemode.commands;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import de.leander.bteggamemode.BTEGGamemode;
import de.leander.bteggamemode.util.WorldEditUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CoverCommand  implements CommandExecutor {
    Clipboard backup;
    BlockVector3 koordinaten;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { return true; }
        Player player = (Player) sender;
        if (!command.getName().equalsIgnoreCase("cover")&&!command.getName().equalsIgnoreCase("/cover")) {
            return true;
        }
        if (!player.hasPermission("bteg.builder")) {
            player.sendMessage(BTEGGamemode.prefix + "§cNo permission for //cover");
            return true;
        }
        if(args.length == 1){
            if(args[0].equalsIgnoreCase("undo")){
                WorldEditUtil.pasteBackup(player, backup, koordinaten);
                return true;
            }
        }

        Region region = null;
        try {
            region = WorldEdit.getInstance().getSessionManager().findByName(player.getName()).getSelection(WorldEdit.getInstance().getSessionManager().findByName(player.getName()).getSelectionWorld());
        } catch (IncompleteRegionException e) {
            e.printStackTrace();
            player.sendMessage(BTEGGamemode.prefix + "§cPlease select a WorldEdit selection!");
            return true;
        }
        backup = WorldEditUtil.saveBackup(region, player);
        koordinaten = BlockVector3.at(region.getMinimumPoint().getBlockX(),region.getMinimumPoint().getY(),region.getMinimumPoint().getZ());
        EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(region.getWorld(), -1);
        LocalSession localSession = WorldEdit.getInstance().getSessionManager().findByName(player.getName());
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
