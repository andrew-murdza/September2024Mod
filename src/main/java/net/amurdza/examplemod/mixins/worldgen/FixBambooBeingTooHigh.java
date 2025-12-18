
package net.amurdza.examplemod.mixins.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.BambooFeature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.ProbabilityFeatureConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BambooFeature.class)
public class FixBambooBeingTooHigh {
    @Redirect(
            method = "place",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos$MutableBlockPos;move(Lnet/minecraft/core/Direction;I)Lnet/minecraft/core/BlockPos$MutableBlockPos;",ordinal = 0)
    )
    private BlockPos.MutableBlockPos safeMove(BlockPos.MutableBlockPos instance, Direction dir, int n,
                                              FeaturePlaceContext<ProbabilityFeatureConfiguration> ctx) {
        BlockPos next = instance.relative(dir, n); // DOES NOT mutate instance
        if (ctx.level().isEmptyBlock(next)) {
            return instance.move(dir, n);            // mutate only if allowed
        }
        return instance;
    }

}