package net.amurdza.examplemod.worldgen.feature.features;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacementContext;

import java.util.Optional;
import java.util.stream.Stream;

/** Select one trunk size before positioning it, retaining the outer biome filter context. */
public final class SnowyTreesFeature extends Feature<RandomFeatureConfiguration> {
    public SnowyTreesFeature() { super(RandomFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<RandomFeatureConfiguration> context) {
        var selected = context.config().defaultFeature;
        for (var candidate : context.config().features) {
            if (context.random().nextFloat() < candidate.chance) {
                selected = candidate.feature;
                break;
            }
        }
        var outer = context.level().registryAccess().registryOrThrow(Registries.PLACED_FEATURE)
                .get(AOEMod.makeID("snowy/trees"));
        if (outer == null) return false;
        var placement = new PlacementContext(context.level(), context.chunkGenerator(), Optional.of(outer));
        Stream<BlockPos> positions = Stream.of(context.origin());
        for (var modifier : selected.value().placement()) {
            positions = positions.flatMap(pos -> modifier.getPositions(placement, context.random(), pos));
        }
        var configured = selected.value().feature().value();
        boolean placed = false;
        for (BlockPos pos : positions.toList()) {
            placed |= configured.place(context.level(), context.chunkGenerator(), context.random(), pos);
        }
        return placed;
    }
}
