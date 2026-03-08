package de.btegermany.utilities.util.worldedit;


import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import de.btegermany.utilities.util.Direction;

public record ReplaceSideArgs(BlockState preBlockState, BlockState postBlockState, Direction direction, boolean ignoreSameBlock, TypeOnlyMask mask) {

    public ReplaceSideArgs(BlockState preBlockState, BlockState postBlockState, Direction direction) {
        this(preBlockState, postBlockState, direction, false, null);
    }


    public ReplaceSideArgs(BlockState preBlockState, BlockType postBlockType, Direction direction, boolean ignoreSameBlock, TypeOnlyMask mask) {
        this(preBlockState, postBlockType.getDefaultState(), direction, ignoreSameBlock, mask);
    }

    public ReplaceSideArgs(BlockState preBlockState, BlockType postBlockType, Direction direction) {
        this(preBlockState, postBlockType, direction, false, null);
    }


    public ReplaceSideArgs(BlockType preBlockType, BlockState postBlockState, Direction direction, boolean ignoreSameBlock, TypeOnlyMask mask) {
        this(preBlockType.getDefaultState(), postBlockState, direction, ignoreSameBlock, mask);
    }

    public ReplaceSideArgs(BlockType preBlockType, BlockState postBlockState, Direction direction) {
        this(preBlockType, postBlockState, direction, false, null);
    }


    public ReplaceSideArgs(BlockType preBlockType, BlockType postBlockType, Direction direction, boolean ignoreSameBlock, TypeOnlyMask mask) {
        this(preBlockType.getDefaultState(), postBlockType.getDefaultState(), direction, ignoreSameBlock, mask);
    }

    public ReplaceSideArgs(BlockType preBlockType, BlockType postBlockType, Direction direction) {
        this(preBlockType, postBlockType, direction, false, null);
    }


    public BlockType preBlockType() {
        return preBlockState.getBlockType();
    }

    public BlockType postBlockType() {
        return postBlockState.getBlockType();
    }

}
