package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.surface_rule.DimensionSurfaceRule;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.BooleanSupplier;

/** OreFeature's direct chunk-section writes still need the level for per-block habitat checks. */
public final class OrePlacementContext {
    private static final ThreadLocal<WorldGenLevel> LEVEL = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> SKIP_AIR_CHECK = new ThreadLocal<>();

    private OrePlacementContext() { }

    public static WorldGenLevel level() { return LEVEL.get(); }

    /** Null outside an actual ore-vein invocation; blob features retain their own rules. */
    public static Boolean skipAirCheck() { return SKIP_AIR_CHECK.get(); }

    public static boolean runVein(FeaturePlaceContext<OreConfiguration> context, BooleanSupplier placement) {
        Boolean previous = SKIP_AIR_CHECK.get();
        var config = context.config();
        if (!config.targetStates.isEmpty() && config.targetStates.stream().allMatch(target -> isOre(target.state))) {
            float chance = config.discardChanceOnAirExposure;
            if (GameplayDimensions.is(context.level(), GameplayDimensions.SNOWY)) {
                var origin = context.origin();
                int y = origin.getY();
                double variation = y >= -49 && y < -32 ? context.level().getLevel().getChunkSource().randomState()
                        .router().vegetation().compute(new DensityFunction.SinglePointContext(
                                origin.getX(), 0, origin.getZ())) : 0;
                boolean deep = DimensionSurfaceRule.snowyDeepslate(y, variation);
                if (GameplayDimensions.biomeIs(context.level(), origin, "snowy_forest")) chance = .75F;
                chance = 1 - (1 - chance) / (deep ? 9 : 3);
            }
            if (GameplayDimensions.is(context.level(), GameplayDimensions.DESERT)
                    && context.origin().getY() < 0) chance = deepDarkDiscardChance(chance);
            // Exactly one sample for the invocation, including 0/100% settings.
            // This keeps vein geometry independent of the exposure probability.
            SKIP_AIR_CHECK.set(context.random().nextFloat() >= chance);
        } else SKIP_AIR_CHECK.remove();
        try { return run(context.level(), placement); }
        finally {
            if (previous == null) SKIP_AIR_CHECK.remove();
            else SKIP_AIR_CHECK.set(previous);
        }
    }

    /** Reduce the exposed-vein share, including ores whose normal exposure chance is zero. */
    public static float deepDarkDiscardChance(float originalDiscardChance) {
        return 1 - (1 - originalDiscardChance) / 6;
    }

    private static boolean isOre(BlockState state) {
        var id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return state.is(Tags.Blocks.ORES) || state.is(Blocks.ANCIENT_DEBRIS)
                || id != null && id.getPath().endsWith("_ore");
    }

    public static boolean run(WorldGenLevel level, BooleanSupplier placement) {
        WorldGenLevel previous = LEVEL.get();
        LEVEL.set(level);
        try { return placement.getAsBoolean(); }
        finally {
            if (previous == null) LEVEL.remove();
            else LEVEL.set(previous);
        }
    }
}
