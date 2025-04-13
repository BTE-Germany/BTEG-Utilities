package de.leander.bteg_utilities.commands;

import de.leander.bteg_utilities.BTEGUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CleanUpCommand implements CommandExecutor {

    static final String LOGS = "oak_log,spruce_log,birch_log,jungle_log,acacia_log,dark_oak_log,mangrove_log,cherry_log,stripped_oak_log,stripped_spruce_log,stripped_birch_log,stripped_jungle_log,stripped_acacia_log,stripped_dark_oak_log,stripped_mangrove_log,stripped_cherry_log";
    static final String WOOD = "oak_wood,spruce_wood,birch_wood,jungle_wood,acacia_wood,dark_oak_wood,mangrove_wood,cherry_wood,stripped_oak_wood,stripped_spruce_wood,stripped_birch_wood,stripped_jungle_wood,stripped_acacia_wood,stripped_dark_oak_wood,stripped_mangrove_wood,stripped_cherry_wood";
    static final String LEAVES = "oak_leaves,spruce_leaves,birch_leaves,jungle_leaves,acacia_leaves,dark_oak_leaves,mangrove_leaves,cherry_leaves,azalea_leaves,flowering_azalea_leaves";
    static final String FLOWERS = "dandelion,poppy,blue_orchid,allium,azure_bluet,red_tulip,orange_tulip,white_tulip,pink_tulip,oxeye_daisy,cornflower,lily_of_the_valley,torchflower,wither_rose,pink_petals,spore_blossom,sunflower,lilac,rose_bush,peony,pitcher_plant";
    static final String OTHER = "mangrove_roots,muddy_mangrove_roots,mushroom_stem,brown_mushroom_block,red_mushroom_block,short_grass,tall_grass,fern,bamboo,sugar_cane,vine,large_fern,brown_mushroom,red_mushroom,azalea,flowering_azalea,melon,pumpkin,oak_fence,birch_fence,spruce_fence,oak_fence_gate,birch_fence_gate,spruce_fence_gate";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player) || !(command.getName().equalsIgnoreCase("cleanup") || command.getName().equalsIgnoreCase("/cleanup"))) {
            return true;
        }
        if (!player.hasPermission("bteg.builder")) {
            player.sendMessage(Component.text(BTEGUtilities.PREFIX)
                    .append(Component.text("No permission for //cleanup", NamedTextColor.RED)));
            return true;
        }

        player.chat("//re " + String.join(",", LOGS, WOOD, LEAVES, FLOWERS, OTHER) + " 0");

        return true;
    }



}
