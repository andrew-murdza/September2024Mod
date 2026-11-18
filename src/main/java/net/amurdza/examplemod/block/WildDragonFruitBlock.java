package net.amurdza.examplemod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class WildDragonFruitBlock extends BushBlock {
    public WildDragonFruitBlock(Properties properties) { super(properties); }

    @Override
    protected boolean mayPlaceOn(BlockState ground, BlockGetter level, BlockPos pos) {
        return ground.is(Blocks.SAND) || ground.is(Blocks.RED_SAND);
    }
}
