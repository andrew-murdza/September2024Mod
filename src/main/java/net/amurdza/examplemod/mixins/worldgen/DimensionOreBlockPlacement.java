package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.util.BiomePlacementRestrictions;
import net.amurdza.examplemod.worldgen.OrePlacementContext;
import net.amurdza.examplemod.worldgen.NetherRampDecorationGuard;
import net.amurdza.examplemod.worldgen.PowderCanePatches;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

@Mixin(OreFeature.class)
public class DimensionOreBlockPlacement {
    @Inject(method = "shouldSkipAirCheck", at = @At("HEAD"), cancellable = true)
    private static void aoemod$oneExposureDecisionPerVein(RandomSource random, float chance,
            CallbackInfoReturnable<Boolean> result) {
        Boolean skip = OrePlacementContext.skipAirCheck();
        if (skip != null) result.setReturnValue(skip);
    }

    @Inject(method = "canPlaceOre", at = @At("RETURN"), cancellable = true)
    private static void aoemod$checkOreHabitat(BlockState existing, Function<BlockPos, BlockState> neighbors,
                                              RandomSource random, OreConfiguration config,
                                              OreConfiguration.TargetBlockState target, BlockPos.MutableBlockPos pos,
                                              CallbackInfoReturnable<Boolean> result) {
        if (existing.is(Blocks.MYCELIUM)) {
            result.setReturnValue(false);
            return;
        }
        var level = OrePlacementContext.level();
        if (level != null && Boolean.TRUE.equals(result.getReturnValue())
                && (!BiomePlacementRestrictions.isAllowed(level, pos, target.state)
                || !NetherRampDecorationGuard.allows(level, pos, target.state)
                || !PowderCanePatches.allowsOre(level, pos, existing, target.state))) result.setReturnValue(false);
    }
}
