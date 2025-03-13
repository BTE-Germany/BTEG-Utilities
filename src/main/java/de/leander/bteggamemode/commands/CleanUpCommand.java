package de.leander.bteggamemode.commands;

import de.leander.bteggamemode.BTEGGamemode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CleanUpCommand implements CommandExecutor {

    final String logs = "oak_log,spruce_log,birch_log,jungle_log,acacia_log,dark_oak_log,mangrove_log,cherry_log,stripped_oak_log,stripped_spruce_log,stripped_birch_log,stripped_jungle_log,stripped_acacia_log,stripped_dark_oak_log,stripped_mangrove_log,stripped_cherry_log";
    final String wood = "oak_wood,spruce_wood,birch_wood,jungle_wood,acacia_wood,dark_oak_wood,mangrove_wood,cherry_wood,stripped_oak_wood,stripped_spruce_wood,stripped_birch_wood,stripped_jungle_wood,stripped_acacia_wood,stripped_dark_oak_wood,stripped_mangrove_wood,stripped_cherry_wood";
    final String leaves = "oak_leaves,spruce_leaves,birch_leaves,jungle_leaves,acacia_leaves,dark_oak_leaves,mangrove_leaves,cherry_leaves,azalea_leaves,flowering_azalea_leaves";
    final String flowers = "dandelion,poppy,blue_orchid,allium,azure_bluet,red_tulip,orange_tulip,white_tulip,pink_tulip,oxeye_daisy,cornflower,lily_of_the_valley,torchflower,wither_rose,pink_petals,spore_blossom,sunflower,lilac,rose_bush,peony,pitcher_plant";
    final String other = "mangrove_roots,muddy_mangrove_roots,mushroom_stem,brown_mushroom_block,red_mushroom_block,grass,tall_grass,fern,bamboo,sugar_cane,vine,large_fern,brown_mushroom,red_mushroom,azalea,flowering_azalea,melon,pumpkin,oak_fence,birch_fence,spruce_fence,oak_fence_gate,birch_fence_gate,spruce_fence_gate";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player) || !(command.getName().equalsIgnoreCase("cleanup") || command.getName().equalsIgnoreCase("/cleanup"))) {
            return true;
        }
        if (!player.hasPermission("bteg.builder")) {
            player.sendMessage(BTEGGamemode.PREFIX + "§cNo permission for //cleanup");
            return true;
        }

        player.chat("//re " + String.join(",", logs, wood, leaves, flowers, other) + " 0");

        return true;
    }



}
