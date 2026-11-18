package net.amurdza.examplemod.worldgen.feature.features;

import net.amurdza.examplemod.worldgen.AquaticPlantPlacement;
import net.amurdza.examplemod.worldgen.DocumentedFloraFeature;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Absolute root probabilities preserve the former shallow fallback when doubling short seagrass. */
public final class PlainsSeafloorFeature extends Feature<NoneFeatureConfiguration> {
    public PlainsSeafloorFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var level = context.level();
        var pos = context.origin();
        if (!level.getBlockState(pos).is(Blocks.WATER) || !level.getFluidState(pos).isSource()
                || !level.getFluidState(pos).is(FluidTags.WATER)
                || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                || !AquaticPlantPlacement.hasSkyExposure(level, pos)) return false;
        int depth = AquaticPlantPlacement.waterColumnHeight(level, pos, 3);
        int roll = context.random().nextInt(100);
        if (roll < (depth < 3 ? 80 : 72)) {
            return DocumentedFloraFeature.placePlant(level, pos, "minecraft:seagrass", context.random());
        }
        if (depth >= 3 && roll < 74) {
            return DocumentedFloraFeature.placePlant(level, pos, "minecraft:tall_seagrass", context.random());
        }
        return false;
    }
}
