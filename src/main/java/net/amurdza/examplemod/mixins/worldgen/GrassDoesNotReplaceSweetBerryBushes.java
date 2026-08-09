package net.amurdza.examplemod.mixins.worldgen;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.SimpleBlockFeature;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SimpleBlockFeature.class)
public class GrassDoesNotReplaceSweetBerryBushes {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void aoemod$preserveSweetBerryBush(
            FeaturePlaceContext<SimpleBlockConfiguration> context,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (context.level().getBlockState(context.origin()).is(Blocks.SWEET_BERRY_BUSH)) {
            cir.setReturnValue(false);
        }
    }
}
