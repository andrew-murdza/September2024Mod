package net.amurdza.examplemod.mixins.othermods.farmersdelight;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vectorwing.farmersdelight.common.block.RichSoilFarmlandBlock;

@Mixin(RichSoilFarmlandBlock.class)
public class RichFarmlandNoBonemeal {
    @Inject(method = "randomTick", at = @At(value = "INVOKE",
            target = "Lvectorwing/farmersdelight/common/block/RichSoilBlock;tryBoostingPlantsAboveAndBelow(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V",
            remap = false), cancellable = true, require = 1)
    private void aoemod$stopAutomaticBonemeal(BlockState state, ServerLevel level, BlockPos pos,
                                              RandomSource random, CallbackInfo ci) {
        // The dependency updates moisture before reaching its fertilization branch.
        ci.cancel();
    }
}
