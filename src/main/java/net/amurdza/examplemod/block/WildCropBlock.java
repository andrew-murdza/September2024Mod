package net.amurdza.examplemod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Iterator;

public class WildCropBlock extends FlowerBlock implements BonemealableBlock {
    protected static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);

    public WildCropBlock(MobEffect suspiciousStewEffect, int effectDuration, BlockBehaviour.Properties properties) {
        super(suspiciousStewEffect, effectDuration, properties);
    }

    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND);
    }

    public boolean canBeReplaced(BlockState state, BlockPlaceContext useContext) {
        return false;
    }

    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean isClient) {
        return true;
    }

    public boolean isBonemealSuccess(Level level, RandomSource rand, BlockPos pos, BlockState state) {
        return (double)rand.nextFloat() < 0.800000011920929;
    }

    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        int wildCropLimit = 10;
        Iterator var6 = BlockPos.betweenClosed(pos.offset(-4, -1, -4), pos.offset(4, 1, 4)).iterator();

        while(var6.hasNext()) {
            BlockPos nearbyPos = (BlockPos)var6.next();
            if (level.getBlockState(nearbyPos).is(this)) {
                --wildCropLimit;
                if (wildCropLimit <= 0) {
                    return;
                }
            }
        }

        BlockPos randomPos = pos.offset(random.nextInt(3) - 1, random.nextInt(2) - random.nextInt(2), random.nextInt(3) - 1);

        for(int k = 0; k < 4; ++k) {
            if (level.isEmptyBlock(randomPos) && state.canSurvive(level, randomPos)) {
                pos = randomPos;
            }

            randomPos = pos.offset(random.nextInt(3) - 1, random.nextInt(2) - random.nextInt(2), random.nextInt(3) - 1);
        }

        if (level.isEmptyBlock(randomPos) && state.canSurvive(level, randomPos)) {
            level.setBlock(randomPos, state, 2);
        }

    }
}
