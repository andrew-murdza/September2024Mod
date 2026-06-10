package net.amurdza.examplemod.mixins.growth_rate;

import net.amurdza.examplemod.config.BlockConfig;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MyceliumBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Mixin;


@Mixin(MyceliumBlock.class)
public abstract class MyceliumBehavior extends MyceliumGrassSpreadRate {
    public MyceliumBehavior(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public int randomTick1(int constant, BlockState state, ServerLevel world, BlockPos pos, RandomSource random){
        return BlockConfig.MYCELIUM_SPREAD_NUM_TRIES;
    }
    private static boolean canBeGrass(BlockState pState, LevelReader pLevelReader, BlockPos pPos) {
        BlockPos blockpos = pPos.above();
        BlockState blockstate = pLevelReader.getBlockState(blockpos);
        if(!pLevelReader.getBiome(pPos).is(ModTags.Biomes.mushroomCaves)){
            return false;
        }
        if (blockstate.is(Blocks.SNOW) && blockstate.getValue(SnowLayerBlock.LAYERS) == 1) {
            return true;
        } else if (blockstate.getFluidState().getAmount() == 8) {
            return false;
        } else {
            int i = LightEngine.getLightBlockInto(pLevelReader, pState, pPos, blockstate, blockpos, Direction.UP, blockstate.getLightBlock(pLevelReader, blockpos));
            return i < pLevelReader.getMaxLightLevel();
        }
    }

    private static boolean canPropagate(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        BlockPos blockpos = pPos.above();
        return canBeGrass(pState, pLevel, pPos) && !pLevel.getFluidState(blockpos).is(FluidTags.WATER);
    }

    @Override
    public void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        if (!canBeGrass(pState, pLevel, pPos)) {
            if (!pLevel.isAreaLoaded(pPos, 1)) return; // Forge: prevent loading unloaded chunks when checking neighbor's light and spreading
            pLevel.setBlockAndUpdate(pPos, Blocks.DIRT.defaultBlockState());
        } else {
            if (!pLevel.isAreaLoaded(pPos, 3)) return; // Forge: prevent loading unloaded chunks when checking neighbor's light and spreading
            BlockState blockstate = this.defaultBlockState();
            for(int i = 0; i < 4; ++i) {
                BlockPos blockpos = pPos.offset(pRandom.nextInt(3) - 1, pRandom.nextInt(5) - 3, pRandom.nextInt(3) - 1);
                if (pLevel.getBlockState(blockpos).is(Blocks.DIRT) && canPropagate(blockstate, pLevel, blockpos)) {
                    pLevel.setBlockAndUpdate(blockpos, blockstate.setValue(SnowyDirtBlock.SNOWY, pLevel.getBlockState(blockpos.above()).is(Blocks.SNOW)));
                }
            }

        }
    }
}