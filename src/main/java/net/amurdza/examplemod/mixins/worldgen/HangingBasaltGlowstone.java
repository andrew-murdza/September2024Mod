package net.amurdza.examplemod.mixins.worldgen;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.GrowthPlacementBuffer;
import net.amurdza.examplemod.worldgen.PowderCanePatches;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.GlowstoneFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.spongepowered.asm.mixin.Mixin;

/** Hanging clusters must stay suspended; ore-shaped floor patches are unchanged. */
@Mixin(GlowstoneFeature.class)
public class HangingBasaltGlowstone {
    @WrapMethod(method = "place")
    private boolean aoemod$keepClustersAboveTheFloor(FeaturePlaceContext<NoneFeatureConfiguration> context,
                                                    Operation<Boolean> original) {
        if (!GameplayDimensions.is(context.level(), GameplayDimensions.NETHER)
                || !GameplayDimensions.biomeIs(context.level(), context.origin(), "basalt_deltas")) {
            return original.call(context);
        }
        GrowthPlacementBuffer buffer = new GrowthPlacementBuffer(context.level());
        var staged = new FeaturePlaceContext<>(context.topFeature(), buffer, context.chunkGenerator(),
                context.random(), context.origin(), context.config());
        if (!original.call(staged)) return false;
        // Inspect the original terrain: a hanging cluster may connect to itself,
        // but it cannot touch a floor, ledge, or a pre-existing floor ore patch.
        return buffer.isValid((pos, state) -> PowderCanePatches.allowsGlowstone(context.level(), pos, state)
                && (!state.is(Blocks.GLOWSTONE)
                || context.level().getBlockState(pos.below()).isAir()
                && context.level().getBlockState(pos.below(2)).isAir())) && buffer.commit();
    }
}
