package net.amurdza.examplemod.block;

import net.amurdza.examplemod.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

public class PaleMossBlock extends Block implements BonemealableBlock {
    public PaleMossBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean clientSide) {
        return level.getBlockState(pos.above()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        for (int i = 0; i < 48; i++) {
            BlockPos groundPos = pos.offset(random.nextInt(7) - 3, random.nextInt(3) - 1, random.nextInt(7) - 3);
            BlockState ground = level.getBlockState(groundPos);
            if (ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.DIRT) || ground.is(Blocks.COARSE_DIRT)
                    || ground.is(Blocks.ROOTED_DIRT) || ground.is(Blocks.STONE)) {
                level.setBlock(groundPos, defaultBlockState(), 3);
            }

            BlockPos plantPos = groundPos.above();
            if (!level.getBlockState(groundPos).is(this) || !level.isEmptyBlock(plantPos)) {
                continue;
            }
            int roll = random.nextInt(20);
            if (roll < 6) {
                level.setBlock(plantPos, ModBlocks.PALE_MOSS_CARPET.get().defaultBlockState(), 3);
            } else if (roll == 6) {
                level.setBlock(plantPos, ModBlocks.CLOSED_EYEBLOSSOM.get().defaultBlockState(), 3);
            } else if (roll == 7 && random.nextInt(3) == 0) {
                level.setBlock(plantPos, ModBlocks.PALE_OAK_SAPLING.get().defaultBlockState(), 3);
            }
        }
    }
}
