package net.amurdza.examplemod.mixins.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.HugeFungusConfiguration;
import net.minecraft.world.level.levelgen.feature.HugeFungusFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

;

@Mixin(HugeFungusFeature.class)
public abstract class HugeFungiDontGenerateIntoWalls extends Feature<HugeFungusConfiguration> {
    public HugeFungiDontGenerateIntoWalls(Codec<HugeFungusConfiguration> codec) {
        super(codec);
    }

    @Shadow
    private static boolean isReplaceable(WorldGenLevel level, BlockPos pos, HugeFungusConfiguration config, boolean checkConfig) {
        throw new AssertionError();
    }

    @Shadow
    private void placeStem(WorldGenLevel level, RandomSource random, HugeFungusConfiguration config, BlockPos pos, int height, boolean huge) {
        throw new AssertionError();
    }

    @Shadow
    private void placeHat(WorldGenLevel level, RandomSource random, HugeFungusConfiguration config, BlockPos pos, int height, boolean huge) {
        throw new AssertionError();
    }

    /**
     * @author Andrew
     * @reason Prevent huge fungus from partially generating through non-replaceable blocks.
     */
    @Overwrite
    public boolean place(FeaturePlaceContext<HugeFungusConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        ChunkGenerator chunkGenerator = context.chunkGenerator();
        HugeFungusConfiguration config = context.config();

        Block baseBlock = config.validBaseState.getBlock();
        BlockState belowState = level.getBlockState(origin.below());

        if (!belowState.is(baseBlock)) {
            return false;
        }

        int height = Mth.nextInt(random, 8, 13);//4

        if (!config.planted) {
            int genDepth = chunkGenerator.getGenDepth();
            if (origin.getY() + height + 1 >= genDepth) {
                return false;
            }
        }

        boolean huge = !config.planted && random.nextFloat() < 0.06F;

        if (!aoemod$canPlaceStem(level, config, origin, height, huge)) {
            return false;
        }

        if (!aoemod$canPlaceHat(level, random, config, origin, height, huge)) {
            return false;
        }

        level.setBlock(origin, Blocks.AIR.defaultBlockState(), 4);
        this.placeStem(level, random, config, origin, height, huge);
        this.placeHat(level, random, config, origin, height, huge);
        return true;
    }

    @Unique
    private static boolean aoemod$canPlaceStem(
            WorldGenLevel level,
            HugeFungusConfiguration config,
            BlockPos pos,
            int height,
            boolean huge
    ) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int radius = huge ? 1 : 0;

        for (int x = -radius; x <= radius; ++x) {
            for (int z = -radius; z <= radius; ++z) {
                for (int y = 0; y < height; ++y) {
                    mutable.setWithOffset(pos, x, y, z);

                    if (!isReplaceable(level, mutable, config, true)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    @Unique
    private static boolean aoemod$canPlaceHat(
            WorldGenLevel level,
            RandomSource random,
            HugeFungusConfiguration config,
            BlockPos pos,
            int height,
            boolean huge
    ) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        int hatHeight = Math.min(random.nextInt(1 + height / 3) + 5, height)-3;//-0
        int hatStart = height - hatHeight;

        for (int y = hatStart; y <= height; ++y) {
            int radius = y < height - random.nextInt(3) ? 2 : 1;

            if (hatHeight > 8 && y < hatStart + 4) {
                radius = 3;
            }

            if (huge) {
                ++radius;
            }

            for (int x = -radius; x <= radius; ++x) {
                for (int z = -radius; z <= radius; ++z) {
                    mutable.setWithOffset(pos, x, y, z);

                    if (!isReplaceable(level, mutable, config, false)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }
}