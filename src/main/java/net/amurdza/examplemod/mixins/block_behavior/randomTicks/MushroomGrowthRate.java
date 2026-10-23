package net.amurdza.examplemod.mixins.block_behavior.randomTicks;

import net.amurdza.examplemod.block.BlockHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MushroomBlock.class)
public abstract class MushroomGrowthRate {
    /**
     * Spreads only small mushrooms; giant mushrooms require bone meal.
     * @author amurdza
     * @reason simplest way to do what I want
     */
    @Overwrite
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockHelper.spreadMushroom(state, level, pos, random);
    }

    @Inject(method = "performBonemeal", at = @At("HEAD"), cancellable = true)
    private void aoe$restrictGiantMushroomHabitat(
            ServerLevel level,
            RandomSource random,
            BlockPos pos,
            BlockState state,
            CallbackInfo ci
    ) {
        if (!BlockHelper.canGrowGiantMushroom(level, pos)) {
            ci.cancel();
        }
    }
}
