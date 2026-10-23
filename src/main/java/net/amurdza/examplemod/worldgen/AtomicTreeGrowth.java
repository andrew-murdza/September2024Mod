package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.BiomePlacementRestrictions;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.GrowthPlacementContext;
import net.amurdza.examplemod.util.TreeGrowthPlacementRules;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.function.Function;

/** Shared atomic shape validation for vanilla trees, giant fungi and the template-based tropical trees. */
public final class AtomicTreeGrowth {
    private AtomicTreeGrowth() { }

    public static <C extends FeatureConfiguration> boolean place(FeaturePlaceContext<C> context,
                                                                 Function<FeaturePlaceContext<C>, Boolean> generate) {
        if (!GameplayDimensions.isCustom(context.level()) || context.level() instanceof GrowthPlacementBuffer) {
            return generate.apply(context);
        }
        for (int attempt = 0; attempt < 4; attempt++) {
            GrowthPlacementBuffer buffer = new GrowthPlacementBuffer(context.level());
            FeaturePlaceContext<C> staged = new FeaturePlaceContext<>(context.topFeature(), buffer,
                    context.chunkGenerator(), context.random(), context.origin(), context.config());
            if (!generate.apply(staged)) continue;
            boolean[] accepted = {false};
            GrowthPlacementContext.runTree(buffer, context.origin(), () -> {
                if (buffer.isValid((pos, state) -> TreeGrowthPlacementRules.isAllowed(buffer, context.origin(), pos, state)
                        && BiomePlacementRestrictions.isAllowed(buffer, pos, state)
                        && NetherRampDecorationGuard.allows(context.level(), pos, state))) {
                    accepted[0] = buffer.commit();
                }
            });
            if (accepted[0]) return true;
        }
        return false;
    }
}
