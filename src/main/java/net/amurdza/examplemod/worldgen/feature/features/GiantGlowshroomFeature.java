package net.amurdza.examplemod.worldgen.feature.features;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.feature.HugeBrownMushroomFeature;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import org.violetmoon.quark.content.world.block.GlowShroomRingBlock;
import org.violetmoon.quark.content.world.module.GlimmeringWealdModule;

public class GiantGlowshroomFeature extends HugeBrownMushroomFeature {
    public GiantGlowshroomFeature(Codec<HugeMushroomFeatureConfiguration> codec) {
        super(codec);
    }
    @Override
    protected void placeTrunk(LevelAccessor pLevel, RandomSource pRandom, BlockPos pPos, HugeMushroomFeatureConfiguration pConfig, int pMaxHeight, BlockPos.MutableBlockPos pMutablePos) {
        for(int i = 0; i < pMaxHeight; ++i) {
            pMutablePos.set(pPos).move(Direction.UP, i);
            if (!pLevel.getBlockState(pMutablePos).isSolidRender(pLevel, pMutablePos)) {
                this.setBlock(pLevel, pMutablePos, pConfig.stemProvider.getState(pRandom, pPos));
                if(i>=pMaxHeight-2){
                    for(Direction direction: Direction.Plane.HORIZONTAL){
                        BlockPos pos1=pMutablePos.relative(direction);
                        if(!pLevel.getBlockState(pos1).isSolidRender(pLevel, pos1)){
                            this.setBlock(pLevel, pos1, GlimmeringWealdModule.glow_shroom_ring.defaultBlockState().setValue(GlowShroomRingBlock.FACING,direction));
                        }
                    }
                }
            }
        }
    }
}
