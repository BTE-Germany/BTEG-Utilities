package de.leander.bteggamemode.commands;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CoverCommand  implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { return true; }
        Player player = (Player) sender;
        if (command.getName().equalsIgnoreCase("cover")||command.getName().equalsIgnoreCase("/cover")) {
            if (player.hasPermission("bteg.builder")) {

                    Region region = null;
                    try {
                        region = WorldEdit.getInstance().getSessionManager().findByName(player.getName()).getSelection(WorldEdit.getInstance().getSessionManager().findByName(player.getName()).getSelectionWorld());
                    } catch (IncompleteRegionException e) {
                        e.printStackTrace();
                        player.sendMessage("§7§l>> §cPlease select a WorldEdit selection!");
                        return true;
                    }
                    EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(region.getWorld(), -1);
                    LocalSession localSession = WorldEdit.getInstance().getSessionManager().findByName(player.getName());
                    localSession.remember(editSession);

                    player.chat("//gmask 0");
                    player.chat("//re <95:7 159:9");
                    player.chat("//re >95:7 159:9");
                    player.chat("//re <95:8 35:7");
                    player.chat("//re >95:8 35:7");
                    player.chat("//gmask");
                    player.chat("//side 95:8 35:7 n n 0");
                    player.chat("//side 95:8 35:7 e n 0");
                    player.chat("//side 95:8 35:7 s n 0");
                    player.chat("//side 95:8 35:7 w n 0");
                    player.chat("//side 95:7 159:9 n n 0");
                    player.chat("//side 95:7 159:9 e n 0");
                    player.chat("//side 95:7 159:9 s n 0");
                    player.chat("//side 95:7 159:9 w n 0");


                }

        }else{
            player.sendMessage("§b§lBTEG §7» §cNo permission for //cover");
        }
        return true;
    }
}
