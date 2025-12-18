package net.amurdza.examplemod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.violetmoon.quark.content.world.block.GlowShroomRingBlock;
import org.violetmoon.quark.content.world.module.GlimmeringWealdModule;
import org.violetmoon.zeta.util.MiscUtil;

public class GiantGlowshroomFeature extends Feature<NoneFeatureConfiguration> {
    public GiantGlowshroomFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource rand = context.random();
        BlockPos pos = context.origin();

        return placeOnMycelium(level, rand, pos);
    }

    public static boolean placeOnMycelium(LevelAccessor worldIn, RandomSource rand, BlockPos pos) {
        BlockPos placePos = pos;

        BlockState stem = GlimmeringWealdModule.glow_shroom_stem.defaultBlockState();
        BlockState ring = GlimmeringWealdModule.glow_shroom_ring.defaultBlockState();

        // Match their cap state build (their block had DOWN property); adjust if your cap differs:
        BlockState cap = GlimmeringWealdModule.glow_shroom_block.defaultBlockState();
        if (cap.hasProperty(net.minecraft.world.level.block.HugeMushroomBlock.DOWN)) {
            cap = cap.setValue(net.minecraft.world.level.block.HugeMushroomBlock.DOWN, false);
        }

        int stemHeight1 = 2;
        int stemHeight2 = rand.nextInt(4);
        boolean hasBigCap = rand.nextDouble() < 0.6;
        int totalHeight = stemHeight1 + stemHeight2 + (hasBigCap ? 2 : 1);

        int horizCheck = 2;

        // clearance check
        for (int dx = -horizCheck; dx <= horizCheck; dx++) {
            for (int dz = -horizCheck; dz <= horizCheck; dz++) {
                for (int dy = 1; dy < totalHeight; dy++) {
                    if (!worldIn.getBlockState(placePos.offset(dx, dy, dz)).isAir()) {
                        return false;
                    }
                }
            }
        }

        // stem part 1
        for (int i = 0; i < stemHeight1; i++) {
            worldIn.setBlock(placePos, stem, 2);
            placePos = placePos.above();
        }

        // optional horizontal offset
        if (stemHeight2 > 0) {
            Direction dir = MiscUtil.HORIZONTALS[rand.nextInt(MiscUtil.HORIZONTALS.length)];
            placePos = placePos.relative(dir);
        }

        // stem part 2
        for (int i = 0; i < stemHeight2; i++) {
            worldIn.setBlock(placePos, stem, 2);
            placePos = placePos.above();
        }

        int ringHeight = Math.min(2, stemHeight2);

        // ring blocks
        for (int i = 0; i < ringHeight; i++) {
            for (Direction ringDir : MiscUtil.HORIZONTALS) {
                BlockPos ringPos = placePos.relative(ringDir).relative(Direction.DOWN, i + 1);
                BlockState ringState = ring;
                if (ringState.hasProperty(GlowShroomRingBlock.FACING)) {
                    ringState = ringState.setValue(GlowShroomRingBlock.FACING, ringDir);
                }
                worldIn.setBlock(ringPos, ringState, 2);
            }
        }

        // cap 3x3
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                worldIn.setBlock(placePos.offset(dx, 0, dz), cap, 2);
            }
        }

        // optional big cap
        if (hasBigCap) {
            worldIn.setBlock(placePos.above(), cap, 2);
        }

        return true;
    }
}
