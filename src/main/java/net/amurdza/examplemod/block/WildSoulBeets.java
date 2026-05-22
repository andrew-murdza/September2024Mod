package net.amurdza.examplemod.block;

import net.mcreator.nourishednether.init.NourishedNetherModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class WildSoulBeets extends WildCropBlock {

    public WildSoulBeets(MobEffect suspiciousStewEffect, int effectDuration, Properties properties) {
        super(suspiciousStewEffect, effectDuration, properties);
    }
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(ModBlocks.NETHER_FARMLAND.get()) || state.is(Blocks.SOUL_SOIL)
                || state.is(NourishedNetherModBlocks.SOUL_SLUDGE.get());
    }
}
