package net.amurdza.examplemod.mixins.growth_rate;

import net.amurdza.examplemod.config.BlockBehaviorConfig;
import net.amurdza.examplemod.util.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GrassBlock.class)
public abstract class GrassBlockSpreadRate extends MyceliumGrassSpreadRate {
    public GrassBlockSpreadRate(Properties pProperties) {
        super(pProperties);
    }


    @Override
    public int randomTick1(int constant, BlockState state, ServerLevel world, BlockPos pos, RandomSource random){
        Integer value = Helper.getBiomeValue(world, pos, BlockBehaviorConfig.BIOME_TO_GRASS_SPREAD_NUM_TRIES);
        return value != null ? value : 0;
    }
}
