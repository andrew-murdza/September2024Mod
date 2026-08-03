package net.amurdza.examplemod.worldgen.surface_rule;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.amurdza.examplemod.mixins.accessor.SurfaceRulesContextAccessor;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.jetbrains.annotations.Nullable;

public record BandlandsSurfaceRuleSource(
        int startY,
        int bandHeight,
        int surfaceDepth,
        int biomeWidth,
        int xSpacing,
        int riverWidth,
        int flatAfterRiver,
        int flatAfterBiomeEdge
) implements SurfaceRules.RuleSource {

    public static final KeyDispatchDataCodec<BandlandsSurfaceRuleSource> CODEC =
            KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec(inst -> inst.group(
                    Codec.INT.optionalFieldOf("start_y", 69).forGetter(BandlandsSurfaceRuleSource::startY),
                    ExtraCodecs.POSITIVE_INT.optionalFieldOf("band_height", 6).forGetter(BandlandsSurfaceRuleSource::bandHeight),
                    ExtraCodecs.POSITIVE_INT.optionalFieldOf("surface_depth", 5).forGetter(BandlandsSurfaceRuleSource::surfaceDepth),
                    ExtraCodecs.POSITIVE_INT.optionalFieldOf("biome_width", 64).forGetter(BandlandsSurfaceRuleSource::biomeWidth),
                    ExtraCodecs.POSITIVE_INT.optionalFieldOf("x_spacing", 64).forGetter(BandlandsSurfaceRuleSource::xSpacing),
                    ExtraCodecs.POSITIVE_INT.optionalFieldOf("river_width", 7).forGetter(BandlandsSurfaceRuleSource::riverWidth),
                    ExtraCodecs.POSITIVE_INT.optionalFieldOf("flat_after_river", 40).forGetter(BandlandsSurfaceRuleSource::flatAfterRiver),
                    ExtraCodecs.POSITIVE_INT.optionalFieldOf("flat_after_biome_edge", 96).forGetter(BandlandsSurfaceRuleSource::flatAfterBiomeEdge)
            ).apply(inst, BandlandsSurfaceRuleSource::new)));

    private static final int BIOME_BANDS_PER_PERIOD = 10;
    private static final int BADLANDS_FIRST_BAND = 4;
    private static final int BADLANDS_BAND_COUNT = 2;
    private static final int SAND_RECTANGLE_MIN_X = -718;
    private static final int SAND_RECTANGLE_MAX_X = -195;
    private static final int SAND_RECTANGLE_MIN_Z = 4346;
    private static final int SAND_RECTANGLE_MAX_Z = 4869;
    private static final int SAND_RECTANGLE_SAND_DEPTH = 2;
    private static final int SAND_RECTANGLE_SANDSTONE_DEPTH = 2;

    private static final Block[] BANDS = new Block[] {
            Blocks.BROWN_TERRACOTTA,
            Blocks.RED_TERRACOTTA,
            Blocks.ORANGE_TERRACOTTA,
            Blocks.TERRACOTTA,
            Blocks.YELLOW_TERRACOTTA,
            Blocks.WHITE_TERRACOTTA,
            Blocks.LIGHT_GRAY_TERRACOTTA,
            Blocks.TERRACOTTA,
            Blocks.GRAY_TERRACOTTA,
            Blocks.BLACK_TERRACOTTA,
            Blocks.PURPLE_TERRACOTTA,
            Blocks.MAGENTA_TERRACOTTA,
            Blocks.PINK_TERRACOTTA,
            Blocks.TERRACOTTA,
            Blocks.LIME_TERRACOTTA,
            Blocks.GREEN_TERRACOTTA,
            Blocks.CYAN_TERRACOTTA,
            Blocks.LIGHT_BLUE_TERRACOTTA,
            Blocks.BLUE_TERRACOTTA
    };

    public static Block terraceBandBlock(
            int squareRingDistance,
            int bandHeight,
            int maximumSquareRingDistance
    ) {
        int initialHiddenDistance = bandHeight - 1;
        int bandDistance = Math.max(0, squareRingDistance - initialHiddenDistance);
        int cycleLength = (BANDS.length + 1) * bandHeight;
        int maximumBandDistance = Math.max(0, maximumSquareRingDistance - initialHiddenDistance);
        int completeCycleDistance = maximumBandDistance / cycleLength * cycleLength;

        if (bandDistance >= completeCycleDistance) {
            return Blocks.TERRACOTTA;
        }

        int distanceInCycle = Math.floorMod(bandDistance, cycleLength);
        int bandIndex = distanceInCycle / bandHeight;
        return bandIndex == BANDS.length ? Blocks.TERRACOTTA : BANDS[bandIndex];
    }

    public static Block terraceBandBlockAt(
            int x,
            int z,
            int bandHeight,
            int biomeWidthBlocks,
            int xSpacingBlocks,
            int riverWidthBlocks,
            int flatAfterRiver,
            int flatAfterBiomeEdge,
            int maximumSquareRingDistance
    ) {
        int squareRingDistance = terraceSquareRingDistanceAt(
                x,
                z,
                biomeWidthBlocks,
                xSpacingBlocks,
                riverWidthBlocks,
                flatAfterRiver,
                flatAfterBiomeEdge
        );
        return terraceBandBlock(squareRingDistance, bandHeight, maximumSquareRingDistance);
    }

    public static int terraceSquareRingDistanceAt(
            int x,
            int z,
            int biomeWidthBlocks,
            int xSpacingBlocks,
            int riverWidthBlocks,
            int flatAfterRiver,
            int flatAfterBiomeEdge
    ) {
        int localX = Math.floorMod(x, xSpacingBlocks);
        int distanceFromRiver;
        if (localX < riverWidthBlocks) {
            distanceFromRiver = 0;
        } else {
            int distanceFromRightEdge = localX - (riverWidthBlocks - 1);
            int distanceFromLeftEdgeOfNextRiver = xSpacingBlocks - localX;
            distanceFromRiver = Math.min(distanceFromRightEdge, distanceFromLeftEdgeOfNextRiver);
        }

        int periodBlocks = biomeWidthBlocks * BIOME_BANDS_PER_PERIOD;
        int localZ = Math.floorMod(z, periodBlocks);
        int badlandsStart = BADLANDS_FIRST_BAND * biomeWidthBlocks;
        int localBandZ = Math.floorMod(localZ - badlandsStart, biomeWidthBlocks);
        int distanceFromNorthEdge = localBandZ + 1;
        int distanceFromSouthEdge = biomeWidthBlocks - localBandZ;
        int distanceInsideBadlands = Math.min(distanceFromNorthEdge, distanceFromSouthEdge);

        int xRingDistance = Math.max(0, distanceFromRiver - flatAfterRiver);
        int zRingDistance = Math.max(0, distanceInsideBadlands - flatAfterBiomeEdge);
        return Math.min(xRingDistance, zRingDistance);
    }

    @Override
    public SurfaceRules.SurfaceRule apply(SurfaceRules.Context context) {
        return new Rule(
                context,
                this.bandHeight,
                this.surfaceDepth,
                this.biomeWidth,
                this.xSpacing,
                this.riverWidth,
                this.flatAfterRiver,
                this.flatAfterBiomeEdge
        );
    }

    @Override
    public KeyDispatchDataCodec<? extends SurfaceRules.RuleSource> codec() {
        return CODEC;
    }

    private static final class Rule implements SurfaceRules.SurfaceRule {
        private final SurfaceRules.Context context;
        private final int bandHeight;
        private final int surfaceDepth;
        private final int biomeWidthBlocks;
        private final int xSpacingBlocks;
        private final int riverWidthBlocks;
        private final int flatAfterRiver;
        private final int flatAfterBiomeEdge;
        private final int maximumSquareRingDistance;

        private Rule(
                SurfaceRules.Context context,
                int bandHeight,
                int surfaceDepth,
                int biomeWidth,
                int xSpacing,
                int riverWidth,
                int flatAfterRiver,
                int flatAfterBiomeEdge
        ) {
            this.context = context;
            this.bandHeight = bandHeight;
            this.surfaceDepth = surfaceDepth;
            this.biomeWidthBlocks = biomeWidth * 16;
            this.xSpacingBlocks = xSpacing * 16;
            this.riverWidthBlocks = riverWidth * 16;
            this.flatAfterRiver = flatAfterRiver;
            this.flatAfterBiomeEdge = flatAfterBiomeEdge;

            int maximumXDistance = Math.max(
                    0,
                    (this.xSpacingBlocks - this.riverWidthBlocks) / 2 - this.flatAfterRiver
            );
            int maximumZDistance = Math.max(
                    0,
                    this.biomeWidthBlocks / 2 - this.flatAfterBiomeEdge
            );
            this.maximumSquareRingDistance = Math.min(maximumXDistance, maximumZDistance);
        }

        @Override
        @Nullable
        public BlockState tryApply(int x, int y, int z) {
            int stoneDepthAbove = ((SurfaceRulesContextAccessor)(Object) this.context).aoemod$getStoneDepthAbove();

            /*
             * Only replace the top N blocks of the floor surface.
             *
             * stoneDepthAbove = 0 means top surface block.
             * stoneDepthAbove = 1 means one block below surface.
             * etc.
             */
            if (stoneDepthAbove < 0) {
                return null;
            }

            if (isInsideSandRectangle(x, z)) {
                if (stoneDepthAbove < SAND_RECTANGLE_SAND_DEPTH) {
                    return Blocks.SAND.defaultBlockState();
                }

                if (stoneDepthAbove
                        < SAND_RECTANGLE_SAND_DEPTH + SAND_RECTANGLE_SANDSTONE_DEPTH) {
                    return Blocks.SANDSTONE.defaultBlockState();
                }

                return Blocks.STONE.defaultBlockState();
            }

            if (stoneDepthAbove < this.surfaceDepth) {
                return getBandBlock(x, z).defaultBlockState();
            }

            if (stoneDepthAbove < this.surfaceDepth + 2) {
                return Blocks.RED_SANDSTONE.defaultBlockState();
            }

            return null;
        }

        private boolean isInsideSandRectangle(int x, int z) {
            return x >= SAND_RECTANGLE_MIN_X
                    && x <= SAND_RECTANGLE_MAX_X
                    && z >= SAND_RECTANGLE_MIN_Z
                    && z <= SAND_RECTANGLE_MAX_Z;
        }

        private Block getBandBlock(int x, int z) {
            return terraceBandBlockAt(
                    x,
                    z,
                    this.bandHeight,
                    this.biomeWidthBlocks,
                    this.xSpacingBlocks,
                    this.riverWidthBlocks,
                    this.flatAfterRiver,
                    this.flatAfterBiomeEdge,
                    this.maximumSquareRingDistance
            );
        }
    }
}
