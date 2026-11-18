package net.amurdza.examplemod.worldgen.surface_rule;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.mixins.accessor.SurfaceRulesContextAccessor;
import net.amurdza.examplemod.worldgen.MountainLakeOffset;
import net.amurdza.examplemod.worldgen.DesertHeight;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.SurfaceRules;

/** Material profiles apply to the actual density surface, including underground gallery floors. */
public record DimensionSurfaceRule(String dimension) implements SurfaceRules.RuleSource {
    public static final KeyDispatchDataCodec<DimensionSurfaceRule> CODEC = KeyDispatchDataCodec.of(
            Codec.STRING.fieldOf("dimension").xmap(DimensionSurfaceRule::new, DimensionSurfaceRule::dimension));
    @Override public KeyDispatchDataCodec<? extends SurfaceRules.RuleSource> codec() { return CODEC; }

    @Override
    public SurfaceRules.SurfaceRule apply(SurfaceRules.Context context) {
        SurfaceRules.SurfaceRule terracottaBands = SurfaceRules.bandlands().apply(context);
        var desertTerrain = dimension.equals("desert")
                ? net.amurdza.examplemod.worldgen.NetherRampDecorationGuard.terrain(context.randomState).orElse(null) : null;
        return (x, y, z) -> {
            int depth = ((SurfaceRulesContextAccessor) (Object) context).aoemod$getStoneDepthAbove();
            int surfaceY = y + depth - 1;
            // Only Snowy uses this value. Plains' vegetation channel is an
            // exact water classifier that constructs a full noise sampler on a
            // cache miss; sampling it here used to waste that work for every
            // surface column despite never using the resulting value.
            double variation = dimension.equals("snowy") ? context.randomState.router().vegetation()
                    .compute(new DensityFunction.SinglePointContext(x, 0, z)) : 0;
            int min = switch (dimension) { case "snowy" -> -96; case "plains" -> -64; case "desert" -> -32; default -> 0; };
            if (y == min && !dimension.equals("desert")) return state(Blocks.BEDROCK);
            if (dimension.equals("nether")) {
                double materialNoise = context.randomState.router().vegetation()
                        .compute(new DensityFunction.SinglePointContext(x, y, z));
                return nether(y, depth, materialNoise);
            }
            int sea = dimension.equals("snowy") || dimension.equals("savanna") || dimension.equals("plains") ? 63 : 64;
            // Read the actual outermost solid block for vanilla terrain and for
            // the desert's exact top-Y material bands, keeping cave floors separate.
            boolean heightmapSurface = dimension.equals("plains") || dimension.equals("savanna") || dimension.equals("snowy") || dimension.equals("desert");
            double ground = heightmapSurface
                    ? ((SurfaceRulesContextAccessor) (Object) context).aoemod$getSurfaceChunk()
                            .getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x & 15, z & 15)
                    : context.randomState.router().initialDensityWithoutJaggedness()
                            .compute(new DensityFunction.SinglePointContext(x, 0, z));
            boolean surface = surfaceY >= ground - (heightmapSurface ? 0 : 2);
            switch (dimension) {
                case "tropical":
                    return state(Blocks.MOSS_BLOCK);
                case "savanna":
                    // Use actual water above the surface so dry banks stay grassy
                    // and every submerged bed returns to the native mud.
                    boolean underwater = ((SurfaceRulesContextAccessor) (Object) context).aoemod$getWaterHeight() > surfaceY;
                    if (surface && underwater) return state(Blocks.MUD);
                    if (surface && !underwater) {
                        if (depth == 1) return state(Blocks.GRASS_BLOCK);
                        if (depth <= 3) return state(Blocks.ROOTED_DIRT);
                        if (depth <= 5) return state(Blocks.DIRT);
                    }
                    return state(Blocks.MUD);
                case "plains":
                    if (surface) {
                        // Vanilla sand, gravel and clay disks break up this dirt bed.
                        // Actual fluid height distinguishes dry Y63 shores from water.
                        boolean plainsWater = ((SurfaceRulesContextAccessor) (Object) context).aoemod$getWaterHeight() > surfaceY;
                        if (plainsWater && depth <= 4) return state(Blocks.DIRT);
                        if (!plainsWater && depth == 1) return state(Blocks.GRASS_BLOCK);
                        if (!plainsWater && depth <= 4) return state(Blocks.DIRT);
                    }
                    return state(Blocks.STONE);
                case "snowy":
                    boolean snowyWater = ((SurfaceRulesContextAccessor) (Object) context).aoemod$getWaterHeight() > surfaceY;
                    if (snowyWater && depth <= (surface ? 4 : 2)) {
                        return state(depth <= 2 ? Blocks.MUD : Blocks.CLAY);
                    }
                    // Habitat belongs to the air above the floor, including the Y-33 boundary floor.
                    if (!snowyWater && surfaceY + 1 >= -32 && surfaceY + 1 < 0 && depth <= 3) return state(depth == 1 ? Blocks.MYCELIUM : Blocks.DIRT);
                    if (surface && !snowyWater && depth <= 3) return state(depth == 1 ? Blocks.GRASS_BLOCK : Blocks.DIRT);
                    return state(snowyDeepslate(y, variation) ? Blocks.DEEPSLATE : Blocks.STONE);
                case "desert":
                    if (y < 0) {
                        // Final sculk/end-stone floors are applied after carving;
                        // ceilings keep this ordinary deepslate material.
                        return state(Blocks.DEEPSLATE);
                    }
                    if (desertTerrain != null && desertTerrain.isDesertEntranceRock(x, y, z)) return state(Blocks.STONE);
                    if (surface) {
                        boolean sandy = surfaceY < DesertHeight.FIRST_TERRACOTTA_Y;
                        int below = ((SurfaceRulesContextAccessor) (Object) context).aoemod$getStoneDepthBelow();
                        // If the entire remaining roof is loose sand, remove it
                        // together instead of leaving gravity blocks over air.
                        if (sandy && depth <= 2 && depth + below - 1 <= 2) return state(Blocks.AIR);
                        if (surfaceY < DesertHeight.RED_SAND_START_HEIGHT) {
                            if (depth <= 2) return state(Blocks.SAND);
                            if (depth == 3) return state(Blocks.SANDSTONE);
                        } else if (sandy) {
                            if (depth <= 2) return state(Blocks.RED_SAND);
                            if (depth <= 4) return state(Blocks.RED_SANDSTONE);
                        } else if (depth <= 16) {
                            // Sample the native palette at a fixed X/Z so every block at
                            // the same Y has the same color, including exposed cave walls.
                            // Y66 corresponds to vanilla's first band above sea level (Y64).
                            return terracottaBands.tryApply(0, y - 2, 0);
                        }
                    }
                    double deepStone = context.randomState.router().vegetation()
                            .compute(new DensityFunction.SinglePointContext(x, y, z));
                    return state(y < 16 && deepStone * 0.5 + 0.5 > y / 16.0 ? Blocks.DEEPSLATE : Blocks.STONE);
                default:
                    return null;
            }
        };
    }

    public static boolean snowyDeepslate(int y, double variation) {
        return y <= -49 || y < -32 && variation * 0.5 + 0.5 > (y + 48) / 16.0;
    }

    private static BlockState nether(int y, int depth, double variation) {
        int above = Math.min(127, y + 1);
        if (above >= 96) return state(variation > 0.05 ? Blocks.SOUL_SAND : Blocks.SOUL_SOIL);
        if (above >= 64) return state(depth == 1 ? Blocks.WARPED_NYLIUM : Blocks.NETHERRACK);
        if (above >= 32) return state(depth == 1 ? Blocks.CRIMSON_NYLIUM : Blocks.NETHERRACK);
        return state(variation > 0 ? Blocks.BASALT : Blocks.BLACKSTONE);
    }

    private static BlockState state(Block block) { return block.defaultBlockState(); }
}
