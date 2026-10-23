package net.amurdza.examplemod.mixins.worldgen;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.amurdza.examplemod.util.BiomePlacementRestrictions;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.OrePlacementContext;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.ScatteredOreFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import org.spongepowered.asm.mixin.Mixin;

/** OreFeature writes chunk sections directly, bypassing WorldGenRegion.setBlock. */
@Mixin({OreFeature.class, ScatteredOreFeature.class})
public class DimensionOreFeature {
    @WrapMethod(method = "place")
    private boolean aoemod$oreMaterials(FeaturePlaceContext<OreConfiguration> context, Operation<Boolean> original) {
        if (!GameplayDimensions.isCustom(context.level())) return original.call(context);
        int dimension = BiomePlacementRestrictions.dimensionMask(context.level());
        if (context.config().targetStates.stream().noneMatch(target ->
                (BiomePlacementRestrictions.allowedMask(target.state.getBlock()) & dimension) != 0)) return false;
        return OrePlacementContext.run(context.level(), () -> original.call(context));
    }
}
