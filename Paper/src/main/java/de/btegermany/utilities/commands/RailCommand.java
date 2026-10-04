package de.btegermany.utilities.commands;


import java.util.ArrayList;
import java.util.Arrays;
import static java.util.Collections.emptyList;
import java.util.List;
import java.util.Locale;

import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;

import de.btegermany.utilities.BTEGUtilities;
import de.btegermany.utilities.util.Direction;
import de.btegermany.utilities.util.TabUtil;
import de.btegermany.utilities.util.worldedit.Converter;
import de.btegermany.utilities.util.worldedit.ReplaceArgs;
import de.btegermany.utilities.util.worldedit.ReplaceSideArgs;
import de.btegermany.utilities.util.worldedit.SelectionEditSession;
import de.btegermany.utilities.util.worldedit.TypeOnlyMask;
import de.btegermany.utilities.util.worldedit.WorldEditUtil;


public class RailCommand implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player) || !(command.getName().equalsIgnoreCase("rail") || command.getName().equalsIgnoreCase("/rail"))) {
            return true;
        }
        if (!player.hasPermission("bteg.builder")) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cNo permission for //rail");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(BTEGUtilities.PREFIX + "Usage:");
            player.sendMessage(BTEGUtilities.PREFIX + "//rail <Block-ID> <Wall-ID-railway-sleepers> <overhead-line[n,glass,tram_single,tram_double]> <rails-in-ground[y,n]>");
            return true;
        }

        String railwaySleepersMaterial = args.length >= 2 ? args[1] : "andesite_wall";
        BlockType middleBlockType;
        try {
            middleBlockType = Converter.getBlockType(args[0], player);
        } catch (RuntimeException exception) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cInvalid block type: " + args[0]);
            return true;
        }

        boolean validRailwaySleepersMaterial = TabUtil.getWallBlocks(railwaySleepersMaterial).stream()
                .anyMatch(wallType -> wallType.equalsIgnoreCase(railwaySleepersMaterial));
        if (!validRailwaySleepersMaterial) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cInvalid railway sleepers block type: " + railwaySleepersMaterial);
            return true;
        }

        String overheadLine = args.length >= 3 ? args[2].toLowerCase(Locale.ROOT) : "none";
        if (!(overheadLine.equals("n") || overheadLine.equals("none") || overheadLine.equals("y")
                || overheadLine.equals("glass") || overheadLine.equals("tram_single") || overheadLine.equals("tram_double"))) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cInvalid overhead-line option: " + args[2]
                    + "§c. Use n, glass, tram_single, or tram_double.");
            return true;
        }
        if (args.length >= 4 && !(args[3].equalsIgnoreCase("y") || args[3].equalsIgnoreCase("n"))) {
            player.sendMessage(BTEGUtilities.PREFIX + "§cInvalid rails-in-ground option: " + args[3] + "§c. Use y or n.");
            return true;
        }

        boolean inGround = args.length >= 4 && args[3].equalsIgnoreCase("y");

        WorldEditUtil.findSelection(player, session -> {
            Direction playerDirection = Direction.ofPlayer(player);
            BlockType anvilType = Converter.getBlockType("anvil");
            BaseBlock anvils = new BaseBlock(anvilType.getDefaultState().with(anvilType.getProperty("facing"),
                    (playerDirection.isHorizontal() ? com.sk89q.worldedit.util.Direction.EAST : com.sk89q.worldedit.util.Direction.NORTH)));

            if (!overheadLine.equals("n") && !overheadLine.equals("none")) {
                session.region().expand(BlockVector3.at(0, 8, 0));
                this.createOverheadLines(session, playerDirection, middleBlockType, overheadLine);
            }

            this.createRails(session, playerDirection, anvils, middleBlockType, railwaySleepersMaterial, inGround);

            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        });

        return true;
    }

    private void createRails(SelectionEditSession session, Direction playerDirection, BaseBlock anvils, BlockType middleBlockType, String railwaySleepersMaterial, boolean inGround) {
        BlockType[] placeholderBlocks = new BlockType[] {
                Converter.getBlockType("light_gray_glazed_terracotta"),
                Converter.getBlockType("red_glazed_terracotta"),
                middleBlockType,
                Converter.getBlockType("yellow_glazed_terracotta"),
                Converter.getBlockType("cyan_glazed_terracotta")
        };
        String duplicatePlaceholderName = "brown_glazed_terracotta";
        BlockType duplicatePlaceholder = Converter.getBlockType(duplicatePlaceholderName);

        // create placeholder blocks
        List<ReplaceSideArgs> replaceSideSteps = new ArrayList<>();

        if (playerDirection.isHorizontal()) {
            replaceSideSteps.addAll(List.of(
                new ReplaceSideArgs(placeholderBlocks[2], placeholderBlocks[1], Direction.NORTH),
                new ReplaceSideArgs(placeholderBlocks[2], placeholderBlocks[3], Direction.SOUTH),

                new ReplaceSideArgs(placeholderBlocks[2], duplicatePlaceholder, Direction.NORTH, true, new TypeOnlyMask(placeholderBlocks[2])),
                new ReplaceSideArgs(duplicatePlaceholder, duplicatePlaceholder, Direction.SOUTH, true, new TypeOnlyMask(placeholderBlocks[2]))
            ));

            if (!inGround) {
                replaceSideSteps.addAll(List.of(
                    new ReplaceSideArgs(placeholderBlocks[1], placeholderBlocks[0], Direction.NORTH, true,
                            new TypeOnlyMask(true, placeholderBlocks[2], duplicatePlaceholder, placeholderBlocks[3])),
                    new ReplaceSideArgs(placeholderBlocks[3], placeholderBlocks[4], Direction.SOUTH, true,
                            new TypeOnlyMask(true, placeholderBlocks[2], duplicatePlaceholder, placeholderBlocks[1], placeholderBlocks[0])),
                    // full width sleepers for 1 block distance between anvils instead of one-sided
                    new ReplaceSideArgs(placeholderBlocks[3], placeholderBlocks[2], Direction.SOUTH, true, new TypeOnlyMask(placeholderBlocks[0]))
                ));
            }
        } else {
            replaceSideSteps.addAll(List.of(
                new ReplaceSideArgs(placeholderBlocks[2], placeholderBlocks[1], Direction.WEST),
                new ReplaceSideArgs(placeholderBlocks[2], placeholderBlocks[3], Direction.EAST),

                new ReplaceSideArgs(placeholderBlocks[2], duplicatePlaceholder, Direction.WEST, true, new TypeOnlyMask(placeholderBlocks[2])),
                new ReplaceSideArgs(duplicatePlaceholder, duplicatePlaceholder, Direction.EAST, true, new TypeOnlyMask(placeholderBlocks[2]))
            ));

            if (!inGround) {
                replaceSideSteps.addAll(List.of(
                    new ReplaceSideArgs(placeholderBlocks[1], placeholderBlocks[0], Direction.WEST, true,
                            new TypeOnlyMask(true, placeholderBlocks[2], duplicatePlaceholder, placeholderBlocks[3])),
                    new ReplaceSideArgs(placeholderBlocks[3], placeholderBlocks[4], Direction.EAST, true,
                            new TypeOnlyMask(true, placeholderBlocks[2], duplicatePlaceholder, placeholderBlocks[1], placeholderBlocks[0])),
                    // full width sleepers for 1 block distance between anvils instead of one-sided
                    new ReplaceSideArgs(placeholderBlocks[3], placeholderBlocks[2], Direction.EAST, true, new TypeOnlyMask(placeholderBlocks[0]))
                ));
            }
        }

        for (ReplaceSideArgs args : replaceSideSteps) {
            SideCommand.replaceSide(session, args);
        }


        // replace placeholder blocks (anvils, railway sleepers)
        List<ReplaceArgs> replaceSteps = new ArrayList<>();

        replaceSteps.add(new ReplaceArgs(new TypeOnlyMask(placeholderBlocks[1], placeholderBlocks[3], duplicatePlaceholder), anvils.toBlockState()));

        if (!inGround) {
            BlockType railwaySleepersType = Converter.getBlockType(railwaySleepersMaterial);
            if (playerDirection.isHorizontal()) {
                replaceSteps.addAll(List.of(
                    new ReplaceArgs(new TypeOnlyMask(placeholderBlocks[0]), railwaySleepersType.getDefaultState()
                            .with(railwaySleepersType.getProperty("north"), "none")
                            .with(railwaySleepersType.getProperty("south"), "low")
                            .with(railwaySleepersType.getProperty("west"), "none")
                            .with(railwaySleepersType.getProperty("east"), "none")
                            .with(railwaySleepersType.getProperty("up"), false)),
                    new ReplaceArgs(new TypeOnlyMask(placeholderBlocks[4]), railwaySleepersType.getDefaultState()
                            .with(railwaySleepersType.getProperty("north"), "low")
                            .with(railwaySleepersType.getProperty("south"), "none")
                            .with(railwaySleepersType.getProperty("west"), "none")
                            .with(railwaySleepersType.getProperty("east"), "none")
                            .with(railwaySleepersType.getProperty("up"), false)),
                    new ReplaceArgs(new TypeOnlyMask(placeholderBlocks[2]), railwaySleepersType.getDefaultState()
                            .with(railwaySleepersType.getProperty("north"), "low")
                            .with(railwaySleepersType.getProperty("south"), "low")
                            .with(railwaySleepersType.getProperty("west"), "none")
                            .with(railwaySleepersType.getProperty("east"), "none")
                            .with(railwaySleepersType.getProperty("up"), false))
                ));
            } else {
                replaceSteps.addAll(List.of(
                        new ReplaceArgs(new TypeOnlyMask(placeholderBlocks[0]), railwaySleepersType.getDefaultState()
                                .with(railwaySleepersType.getProperty("north"), "none")
                                .with(railwaySleepersType.getProperty("south"), "none")
                                .with(railwaySleepersType.getProperty("west"), "none")
                                .with(railwaySleepersType.getProperty("east"), "low")
                                .with(railwaySleepersType.getProperty("up"), false)),
                        new ReplaceArgs(new TypeOnlyMask(placeholderBlocks[2]), railwaySleepersType.getDefaultState()
                                .with(railwaySleepersType.getProperty("north"), "none")
                                .with(railwaySleepersType.getProperty("south"), "none")
                                .with(railwaySleepersType.getProperty("west"), "low")
                                .with(railwaySleepersType.getProperty("east"), "low")
                                .with(railwaySleepersType.getProperty("up"), false)),
                        new ReplaceArgs(new TypeOnlyMask(placeholderBlocks[4]), railwaySleepersType.getDefaultState()
                                .with(railwaySleepersType.getProperty("north"), "none")
                                .with(railwaySleepersType.getProperty("south"), "none")
                                .with(railwaySleepersType.getProperty("west"), "low")
                                .with(railwaySleepersType.getProperty("east"), "none")
                                .with(railwaySleepersType.getProperty("up"), false))
                ));
            }
        }

        //we need to use the custom implementation as we cant provide the changed blocks to the default WE replace method
        WorldEditUtil.replaceAll(session, replaceSteps);
    }

    private void createOverheadLines(SelectionEditSession session, Direction playerDirection, BlockType middleBlockType, String overheadLine) {
        BlockType airPlaceholder = Converter.getBlockType("lime_glazed_terracotta");
        List<ReplaceSideArgs> replaceSideSteps;
        if (overheadLine.equals("tram_single") || overheadLine.equals("tram_double")) {
            this.createTramLines(session, playerDirection, middleBlockType, overheadLine.equals("tram_double"));
            return;
        } else {
            BlockType overheadLineType = Converter.getBlockType("black_stained_glass_pane");
            BlockState overheadLineState;
            if (playerDirection.isHorizontal()) {
                overheadLineState = overheadLineType.getDefaultState()
                        .with(overheadLineType.getProperty("north"), false)
                        .with(overheadLineType.getProperty("south"), false)
                        .with(overheadLineType.getProperty("west"), true)
                        .with(overheadLineType.getProperty("east"), true);
            } else {
                overheadLineState = overheadLineType.getDefaultState()
                        .with(overheadLineType.getProperty("north"), true)
                        .with(overheadLineType.getProperty("south"), true)
                        .with(overheadLineType.getProperty("west"), false)
                        .with(overheadLineType.getProperty("east"), false);
            }
            replaceSideSteps = this.getOverheadLineSteps(middleBlockType, overheadLineState, airPlaceholder);
        }

        for (ReplaceSideArgs args : replaceSideSteps) {
            SideCommand.replaceSide(session, args);
        }

        // remove unneeded stacked blocks
        WorldEditUtil.replaceAll(session, List.of(new ReplaceArgs(new TypeOnlyMask(airPlaceholder), Converter.getBlockType("air").getDefaultState())));
    }

    private void createTramLines(SelectionEditSession session, Direction playerDirection, BlockType middleBlockType,
                                 boolean doubleLine) {
        BlockType buttonType = Converter.getBlockType("polished_blackstone_button");
        BlockState airState = Converter.getBlockType("air").getDefaultState();
        com.sk89q.worldedit.util.Direction buttonFacing = switch (playerDirection) {
            case NORTH -> com.sk89q.worldedit.util.Direction.EAST;
            case SOUTH -> com.sk89q.worldedit.util.Direction.EAST;
            case EAST -> com.sk89q.worldedit.util.Direction.NORTH;
            case WEST -> com.sk89q.worldedit.util.Direction.NORTH;
            default -> com.sk89q.worldedit.util.Direction.NORTH;
        };
        BlockState ceilingButton = buttonType.getDefaultState()
                .with(buttonType.getProperty("face"), "ceiling")
                .with(buttonType.getProperty("facing"), buttonFacing);
        BlockState floorButton = ceilingButton.with(buttonType.getProperty("face"), "floor");

        for (int x = session.region().getMinimumPoint().x(); x <= session.region().getMaximumPoint().x(); x++) {
            for (int y = session.region().getMinimumPoint().y(); y <= session.region().getMaximumPoint().y(); y++) {
                for (int z = session.region().getMinimumPoint().z(); z <= session.region().getMaximumPoint().z(); z++) {
                    BlockVector3 middlePosition = BlockVector3.at(x, y, z);
                    if (!session.region().contains(middlePosition)
                            || !session.editSession().getBlock(middlePosition).getBlockType().equals(middleBlockType)) {
                        continue;
                    }

                    for (int offset = 1; offset <= 4; offset++) {
                        this.setBlock(session, x, y + offset, z, airState);
                    }
                    this.setBlock(session, x, y + 5, z, ceilingButton);
                    if (doubleLine) {
                        this.setBlock(session, x, y + 6, z, airState);
                        this.setBlock(session, x, y + 7, z, floorButton);
                    }
                }
            }
        }
    }

    private void setBlock(SelectionEditSession session, int x, int y, int z, BlockState blockState) {
        BlockVector3 position = BlockVector3.at(x, y, z);
        if (session.region().contains(position)) {
            session.editSession().setBlock(x, y, z, blockState);
            session.changedBlocks().put(position, blockState);
        }
    }

    private List<ReplaceSideArgs> getOverheadLineSteps(BlockType middleBlockType, BlockState overheadLineState, BlockType airPlaceholder) {
        List<ReplaceSideArgs> replaceSideSteps = new ArrayList<>();

        // stack overhead line block up
        replaceSideSteps.add(new ReplaceSideArgs(middleBlockType, overheadLineState, Direction.UP));
        for (int i = 0; i < 5; i++) {
            replaceSideSteps.add(new ReplaceSideArgs(overheadLineState, overheadLineState, Direction.UP));
        }

        // mark unneeded stacked blocks
        replaceSideSteps.add(new ReplaceSideArgs(middleBlockType, airPlaceholder, Direction.UP));
        for (int i = 0; i < 4; i++) {
            replaceSideSteps.add(new ReplaceSideArgs(airPlaceholder, airPlaceholder, Direction.UP));
        }
        return replaceSideSteps;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("bteg.builder")) {
            return emptyList();
        }
        return switch (args.length) {
            case 1 -> TabUtil.getMaterialBlocks(args[0], true);
            case 2 -> TabUtil.getWallBlocks(args[1]);
            case 3 -> Arrays.asList("none", "glass", "tram_single", "tram_double");
            case 4 -> Arrays.asList("y", "n");
            default -> emptyList();
        };
    }


}
