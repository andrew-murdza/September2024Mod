package net.amurdza.examplemod.worldgen.feature.features;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.CaveVinePlacement;
import net.amurdza.examplemod.worldgen.feature.configs.CaveVineConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class CaveVineColumn extends Feature<CaveVineConfig> {
    public CaveVineColumn(Codec<CaveVineConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<CaveVineConfig> context) {
        CaveVineConfig config = context.config();
        RandomSource random = context.random();
        WorldGenLevel level = context.level();
        ChunkPos chunk = new ChunkPos(context.origin());
        boolean placed = false;
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                for (int y = level.getMaxBuildHeight() - 2; y > level.getMinBuildHeight(); y--) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState current = level.getBlockState(pos);
                    if (!current.isAir() && !current.is(Blocks.WATER)) continue;
                    BlockState support = level.getBlockState(pos.above());
                    if (!support.isSolid() && !support.is(BlockTags.LEAVES)) continue;
                    if (random.nextFloat() >= config.chance) continue;
                    float choice = random.nextFloat() * (1 + config.weepingVinesChance + config.sporeBlossomChance);
                    if (choice < config.weepingVinesChance) {
                        placed |= placeWeeping(level, pos, config.heightProvider.sample(random));
                    } else if (choice < config.weepingVinesChance + config.sporeBlossomChance) {
                        if (GameplayDimensions.is(level, GameplayDimensions.TROPICAL)
                                && GameplayDimensions.biomeIs(level, pos, "mangrove_swamp")) continue;
                        if (GameplayDimensions.is(level, GameplayDimensions.TROPICAL) && random.nextInt(4) != 0) continue;
                        BlockState blossom = Blocks.SPORE_BLOSSOM.defaultBlockState();
                        if (current.isAir() && blossom.canSurvive(level, pos)) placed |= level.setBlock(pos, blossom, 2);
                    } else {
                        boolean mangrove = GameplayDimensions.is(level, GameplayDimensions.TROPICAL)
                                && GameplayDimensions.biomeIs(level, pos, "mangrove_swamp");
                        placed |= CaveVinePlacement.place(level, pos, config.heightProvider.sample(random), !mangrove);
                    }
                }
            }
        }
        return placed;
    }

    private static boolean placeWeeping(WorldGenLevel level, BlockPos start, int length) {
        int count = 0;
        while (count < length && start.getY() - count >= level.getMinBuildHeight()
                && level.isEmptyBlock(start.below(count))) count++;
        for (int i = 0; i < count; i++) {
            BlockState state = i == count - 1
                    ? Blocks.WEEPING_VINES.defaultBlockState().setValue(BlockStateProperties.AGE_25, 25)
                    : Blocks.WEEPING_VINES_PLANT.defaultBlockState();
            level.setBlock(start.below(i), state, 2);
        }
        return count > 0;
    }
}
