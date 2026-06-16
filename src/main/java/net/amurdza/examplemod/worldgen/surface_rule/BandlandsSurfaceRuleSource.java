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
        int surfaceDepth
) implements SurfaceRules.RuleSource {

    public static final KeyDispatchDataCodec<BandlandsSurfaceRuleSource> CODEC =
            KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec(inst -> inst.group(
                    Codec.INT.optionalFieldOf("start_y", 69).forGetter(BandlandsSurfaceRuleSource::startY),
                    ExtraCodecs.POSITIVE_INT.optionalFieldOf("band_height", 2).forGetter(BandlandsSurfaceRuleSource::bandHeight),
                    ExtraCodecs.POSITIVE_INT.optionalFieldOf("surface_depth", 4).forGetter(BandlandsSurfaceRuleSource::surfaceDepth)
            ).apply(inst, BandlandsSurfaceRuleSource::new)));

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

    @Override
    public SurfaceRules.SurfaceRule apply(SurfaceRules.Context context) {
        return new Rule(context, this.startY, this.bandHeight, this.surfaceDepth);
    }

    @Override
    public KeyDispatchDataCodec<? extends SurfaceRules.RuleSource> codec() {
        return CODEC;
    }

    private static final class Rule implements SurfaceRules.SurfaceRule {
        private final SurfaceRules.Context context;
        private final int startY;
        private final int bandHeight;
        private final int surfaceDepth;

        private Rule(
                SurfaceRules.Context context,
                int startY,
                int bandHeight,
                int surfaceDepth
        ) {
            this.context = context;
            this.startY = startY;
            this.bandHeight = bandHeight;
            this.surfaceDepth = surfaceDepth;
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
            if (stoneDepthAbove < 0 || stoneDepthAbove >= this.surfaceDepth) {
                return null;
            }

            /*
             * This is the key part.
             *
             * The color is based on the top block of this x,z column,
             * not on the current block's own Y.
             */
            int topY = y + stoneDepthAbove;

            return getBandBlock(topY, this.startY, this.bandHeight).defaultBlockState();
        }

        private static Block getBandBlock(int topY, int startY, int bandHeight) {
            if (topY < startY) {
                return BANDS[0];
            }

            int bandIndex = (topY - startY) / bandHeight;

            if (bandIndex >= BANDS.length) {
                return Blocks.TERRACOTTA;
            }

            return BANDS[bandIndex];
        }
    }
}