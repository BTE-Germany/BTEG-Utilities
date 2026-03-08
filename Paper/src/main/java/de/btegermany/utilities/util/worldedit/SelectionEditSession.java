package de.btegermany.utilities.util.worldedit;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockState;
import org.bukkit.entity.Player;

import java.util.Map;

public record SelectionEditSession(Player player, Region region, EditSession editSession, LocalSession localSession, Map<BlockVector3, BlockState> changedBlocks) {}