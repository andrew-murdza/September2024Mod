package net.amurdza.examplemod.mixins.trees;

import net.amurdza.examplemod.event_handlers.BoneMeal;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AzaleaBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AzaleaBlock.class)
public class AzaleaRainforestTree {
    @Inject(method = "performBonemeal", at = @At("HEAD"), cancellable = true)
    private void aoemod$growDecorativeAzaleas(
            ServerLevel level,
            RandomSource random,
            BlockPos pos,
            BlockState state,
            CallbackInfo ci
    ) {
        BoneMeal.growFlowers(level, pos);
        ci.cancel();
    }
}
