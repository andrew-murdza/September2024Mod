package net.amurdza.examplemod.block;

import net.amurdza.examplemod.util.ModTags;
import net.amurdza.examplemod.worldgen.PaleOakTreeGrower;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;

public class PaleOakSaplingBlock extends SaplingBlock {
    public PaleOakSaplingBlock(Properties properties) {
        super(new PaleOakTreeGrower(), properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(ModTags.Blocks.soulSediments);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1)) {
            return;
        }

        if (ForgeHooks.onCropsGrowPre(level,pos,state,random.nextInt(7)==0)) {
            this.advanceTree(level, pos, state, random);
            net.minecraftforge.common.ForgeHooks.onCropsGrowPost(level, pos, state);
        }
    }
}
