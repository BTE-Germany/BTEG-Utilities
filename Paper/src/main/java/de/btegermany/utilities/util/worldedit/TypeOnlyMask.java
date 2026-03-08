package de.btegermany.utilities.util.worldedit;

import com.sk89q.worldedit.world.block.BlockType;

public class TypeOnlyMask {

    private final boolean inverse;
    private final BlockType[] blockTypes;

    public TypeOnlyMask(boolean inverse, BlockType... blockTypes) {
        this.inverse = inverse;
        this.blockTypes = blockTypes;
    }

    public TypeOnlyMask(BlockType... blockTypes) {
        this(false, blockTypes);
    }

    /**
     * @return whether the blocktype matches the mask
     */
    public boolean test(BlockType blockType) {
        if (inverse) {
            for (BlockType typeToTest : this.blockTypes) {
                if (typeToTest.equals(blockType)) {
                    return false;
                }
            }
            return true;
        }

        for (BlockType typeToTest : this.blockTypes) {
            if (typeToTest.equals(blockType)) {
                return true;
            }
        }
        return false;
    }

}
