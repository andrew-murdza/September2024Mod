package net.amurdza.examplemod.mixins.block_behavior.randomTicks;

import net.amurdza.examplemod.block.BlockHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.FungusBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(FungusBlock.class)
public abstract class NetherFungusGrowth extends BushBlock {

    public NetherFungusGrowth(Properties pProperties) {
        super(pProperties);
    }

    /**
     * Makes crimson/warped fungus receive random ticks.
     *
     * Vanilla FungusBlock does not randomly tick by default, so adding randomTick
     * alone is not enough.
     */
    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    /**
     * Gives nether fungus mushroom-like random growth behavior.
     */
    @Override
    public void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        BlockHelper.spreadMushroom(pState, pLevel, pPos, pRandom,
                (level,pos,state,random) -> ((FungusBlock)state.getBlock()).performBonemeal(level,random,pos,state));
    }
}