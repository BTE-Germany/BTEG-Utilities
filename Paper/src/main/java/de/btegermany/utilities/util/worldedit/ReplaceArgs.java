package de.btegermany.utilities.util.worldedit;

import com.sk89q.worldedit.world.block.BlockState;

public record ReplaceArgs(TypeOnlyMask maskToReplace, BlockState blockToReplaceWith) {
}
