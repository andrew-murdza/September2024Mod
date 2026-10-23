package net.amurdza.examplemod.worldgen.surface_rule;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.mixins.accessor.SurfaceRulesContextAccessor;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.SurfaceRules;

/** Material profiles apply to the actual density surface, including underground gallery floors. */
public record DimensionSurfaceRule(String dimension) implements SurfaceRules.RuleSource {
    public static final KeyDispatchDataCodec<DimensionSurfaceRule> CODEC = KeyDispatchDataCodec.of(
            Codec.STRING.fieldOf("dimension").xmap(DimensionSurfaceRule::new, DimensionSurfaceRule::dimension));
    private static final Block[] TERRACOTTA = {Blocks.TERRACOTTA, Blocks.ORANGE_TERRACOTTA,
            Blocks.TERRACOTTA, Blocks.YELLOW_TERRACOTTA, Blocks.BROWN_TERRACOTTA,
            Blocks.TERRACOTTA, Blocks.RED_TERRACOTTA, Blocks.WHITE_TERRACOTTA};

    @Override public KeyDispatchDataCodec<? extends SurfaceRules.RuleSource> codec() { return CODEC; }

    @Override
    public SurfaceRules.SurfaceRule apply(SurfaceRules.Context context) {
        return (x, y, z) -> {
            int depth = ((SurfaceRulesContextAccessor) (Object) context).aoemod$getStoneDepthAbove();
            int surfaceY = y + depth - 1;
            double variation = context.randomState.router().vegetation().compute(new DensityFunction.SinglePointContext(x, 0, z));
            int min = switch (dimension) { case "snowy" -> -96; case "plains" -> -64; case "desert" -> -32; default -> 0; };
            if (y == min && !dimension.equals("desert")) return state(Blocks.BEDROCK);
            if (dimension.equals("nether")) {
                double materialNoise = context.randomState.router().vegetation()
                        .compute(new DensityFunction.SinglePointContext(x, y, z));
                return nether(y, depth, materialNoise);
            }
            int sea = dimension.equals("snowy") ? 96 : 64;
            double ground = context.randomState.router().initialDensityWithoutJaggedness()
                    .compute(new DensityFunction.SinglePointContext(x, 0, z));
            boolean surface = surfaceY >= ground - 2;
            boolean submerged = surface && surfaceY < sea;
            switch (dimension) {
                case "tropical":
                    return state(depth <= (submerged ? 3 : 4) ? Blocks.MOSS_BLOCK : Blocks.MUD);
                case "savanna":
                    if (surface && !submerged) {
                        if (depth == 1) return state(Blocks.GRASS_BLOCK);
                        if (depth <= 3) return state(Blocks.ROOTED_DIRT);
                        if (depth <= 5) return state(Blocks.DIRT);
                    }
                    return state(Blocks.MOSS_BLOCK);
                case "plains":
                    if (surface) {
                        if (submerged && depth <= 3) return state(variation > 0.25 && surfaceY > 60 ? Blocks.SAND : Blocks.CLAY);
                        if (!submerged && depth == 1) return state(Blocks.GRASS_BLOCK);
                        if (!submerged && depth <= 4) return state(Blocks.DIRT);
                    }
                    return state(Blocks.STONE);
                case "snowy":
                    if (submerged && depth <= 4) {
                        // Ice is restricted to shallow shore material; deep fishing water stays open.
                        if (surfaceY >= 92 && variation > 0.25) return state(variation > 0.50 ? Blocks.BLUE_ICE : Blocks.PACKED_ICE);
                        return state(depth <= 2 ? Blocks.MUD : Blocks.CLAY);
                    }
                    if (surfaceY >= -32 && surfaceY < 0 && depth <= 3) return state(depth == 1 ? Blocks.MYCELIUM : Blocks.DIRT);
                    if (surface && !submerged && depth <= 3) return state(depth == 1 ? Blocks.GRASS_BLOCK : Blocks.DIRT);
                    double transition = (y + 48) / 16.0;
                    return state(y <= -49 || y < -32 && variation * 0.5 + 0.5 > transition ? Blocks.DEEPSLATE : Blocks.STONE);
                case "desert":
                    if (y < 0) {
                        if (depth == 1 && surfaceY >= -32 && surfaceY < 0) return state(Blocks.SCULK);
                        return state(Blocks.DEEPSLATE);
                    }
                    if (surface) {
                        double elevation = surfaceY + variation * 12;
                        if (submerged || elevation <= 80) {
                            if (depth <= 2) return state(Blocks.SAND);
                            if (depth == 3) return state(Blocks.SANDSTONE);
                        } else if (elevation < 112) {
                            if (depth <= 2) return state(Blocks.RED_SAND);
                            if (depth <= 4) return state(Blocks.RED_SANDSTONE);
                        } else if (depth <= 16) {
                            return state(TERRACOTTA[Math.floorMod(Math.floorDiv(y, 3), TERRACOTTA.length)]);
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

    private static BlockState nether(int y, int depth, double variation) {
        int above = Math.min(127, y + 1);
        if (above >= 96) return state(variation > 0.05 ? Blocks.SOUL_SAND : Blocks.SOUL_SOIL);
        if (above >= 64) return state(depth == 1 ? Blocks.WARPED_NYLIUM : Blocks.NETHERRACK);
        if (above >= 32) return state(depth == 1 ? Blocks.CRIMSON_NYLIUM : Blocks.NETHERRACK);
        if (y < 12 && depth == 1) return state(Blocks.MAGMA_BLOCK);
        return state(variation > 0 ? Blocks.BASALT : Blocks.BLACKSTONE);
    }

    private static BlockState state(Block block) { return block.defaultBlockState(); }
}
