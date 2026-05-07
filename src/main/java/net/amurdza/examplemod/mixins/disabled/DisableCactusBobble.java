package net.amurdza.examplemod.mixins.disabled;

import net.minecraft.world.level.levelgen.feature.BlockColumnFeature;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = BlockColumnFeature.class,priority = 1001)
public abstract class DisableCactusBobble {
//    @Redirect(method = "place", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/WorldGenLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
//    private boolean place(WorldGenLevel level, BlockPos pos, BlockState state, int i) {
//        return level.setBlock(pos, state, i);
//    }
}