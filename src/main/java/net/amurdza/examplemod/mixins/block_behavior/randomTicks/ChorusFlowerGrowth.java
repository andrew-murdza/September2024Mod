package net.amurdza.examplemod.mixins.block_behavior.randomTicks;

import net.amurdza.examplemod.config.BlockConfig;
import net.amurdza.examplemod.util.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.ChorusPlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.*;

import javax.annotation.Nullable;

@Mixin(ChorusFlowerBlock.class)
public abstract class ChorusFlowerGrowth extends Block {
    @Shadow @Final public static IntegerProperty AGE;
    @Shadow @Final private ChorusPlantBlock plant;

    public ChorusFlowerGrowth(Properties properties) {
        super(properties);
    }

    @Shadow
    private static boolean allNeighborsEmpty(LevelReader level, BlockPos pos, @Nullable Direction excludingSide) {
        throw new AssertionError();
    }

    @Unique
    private static boolean aoe$isChorusBase(BlockState state) {
        return state.is(Blocks.END_STONE)
                || state.is(Blocks.SCULK)
                || state.is(Blocks.SCULK_CATALYST);
    }

    @Unique
    private static BlockState deadFlower() {
        return Blocks.CHORUS_FLOWER.defaultBlockState().setValue(AGE, 5);
    }

    @Unique
    private static BlockState flower(int age) {
        return Blocks.CHORUS_FLOWER.defaultBlockState().setValue(AGE, Math.min(age, 5));
    }

    @Unique
    private static BlockState plantState(LevelAccessor level, BlockPos pos) {
        return ((ChorusPlantBlock) Blocks.CHORUS_PLANT).getStateForPlacement(level, pos);
    }

    @Unique
    private static void placeDeadFlower(LevelAccessor level, BlockPos pos) {
        level.setBlock(pos, deadFlower(), 2);
        refreshPlantIfPresent(level, pos.below());
        level.levelEvent(1034, pos, 0);
    }

    @Unique
    private static void placeGrownFlower(LevelAccessor level, BlockPos pos, int age) {
        level.setBlock(pos, flower(age), 2);
        refreshPlantIfPresent(level, pos.below());
        level.levelEvent(1033, pos, 0);
    }

    @Unique
    private static boolean hasOnlyOneVerticalAir(LevelReader level, BlockPos pos) {
        return level.isEmptyBlock(pos.above()) && !level.isEmptyBlock(pos.above(2));
    }

    @Unique
    private static boolean hasVerticalAir(LevelReader level, BlockPos pos, int count) {
        for (int i = 1; i <= count; i++) {
            if (!level.isEmptyBlock(pos.above(i))) return false;
        }
        return true;
    }

    @Unique
    private static boolean hasThreeStemBlocksBelow(LevelReader level, BlockPos pos) {
        for (int i = 1; i <= 3; i++) {
            if (!level.getBlockState(pos.below(i)).is(Blocks.CHORUS_PLANT)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Replaces vanilla randomTick so side/up failure always caps with a mature flower.
     *
     * @author Andrew Murdza / ChatGPT
     * @reason Custom chorus growth rules for cramped cave generation.
     */
    @Overwrite
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        BlockPos above = pos.above();

        if (age >= 5) return;

        if (!level.isEmptyBlock(above) || above.getY() >= level.getMaxBuildHeight()) {
            placeDeadFlower(level, pos);
            return;
        }

        if (!ForgeHooks.onCropsGrowPre(level, above, state, true)) return;

        if (hasOnlyOneVerticalAir(level, pos)) {
            level.setBlock(pos, this.plant.getStateForPlacement(level, pos), 2);
            placeDeadFlower(level, above);
            ForgeHooks.onCropsGrowPost(level, pos, state);
            return;
        }

        boolean canGrowUp = false;
        boolean rootedOnChorusBase = false;
        BlockState below = level.getBlockState(pos.below());

        if (aoe$isChorusBase(below)) {
            canGrowUp = true;
        } else if (below.is(this.plant)) {
            int stemBelow = 1;

            for (int k = 0; k < 4; ++k) {
                BlockState lower = level.getBlockState(pos.below(stemBelow + 1));
                if (!lower.is(this.plant)) {
                    if (aoe$isChorusBase(lower)) rootedOnChorusBase = true;
                    break;
                }
                ++stemBelow;
            }

            if (stemBelow < 2 || stemBelow <= random.nextInt(rootedOnChorusBase ? 5 : 4)) {
                canGrowUp = true;
            }
        } else if (below.isAir()) {
            canGrowUp = true;
        }

        if (canGrowUp && allNeighborsEmpty(level, above, null) && level.isEmptyBlock(pos.above(2))) {
            level.setBlock(pos, this.plant.getStateForPlacement(level, pos), 2);
            int newAge = Helper.withChance(level, BlockConfig.PLACE_CHORUS_FLOWER_CHANCE)
                    ? age - 1
                    : age;
            placeGrownFlower(level, above, Math.max(0, newAge));
            ForgeHooks.onCropsGrowPost(level, pos, state);
            return;
        }

        if (age < 4) {
            int tries = random.nextInt(4);
            if (rootedOnChorusBase) ++tries;

            boolean placedSideBranch = false;

            for (int i = 0; i < tries; ++i) {
                Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                BlockPos side = pos.relative(direction);

                if (
                        hasThreeStemBlocksBelow(level, pos) &&
                                level.isEmptyBlock(side) &&
                                level.isEmptyBlock(side.below()) &&
                                allNeighborsEmpty(level, side, direction.getOpposite())
                ) {
                    placeGrownFlower(level, side, age + 1);
                    placedSideBranch = true;
                }
            }

            if (placedSideBranch) {
                level.setBlock(pos, this.plant.getStateForPlacement(level, pos), 2);
            } else {
                placeDeadFlower(level, pos);
            }
        } else {
            placeDeadFlower(level, pos);
        }

        ForgeHooks.onCropsGrowPost(level, pos, state);
    }

    /**
     * Feature generation only: refuse to start unless there are at least 4 vertical air blocks.
     *
     * @author Andrew Murdza / ChatGPT
     * @reason Prevent feature-grown chorus trees from spawning into cramped ceilings.
     */
    @Overwrite
    public static void generatePlant(LevelAccessor level, BlockPos pos, RandomSource random, int maxHorizontalDistance) {
        if (!hasVerticalAir(level, pos, 4)) {
            return;
        }

        level.setBlock(pos, plantState(level, pos), 2);
        aoe$growTreeRecursive(level, pos, random, pos, maxHorizontalDistance, 0);
    }

    @Unique
    private static void aoe$growTreeRecursive(LevelAccessor level, BlockPos branchPos, RandomSource random, BlockPos originalBranchPos, int maxHorizontalDistance, int iterations) {
        int height = random.nextInt(4) + 1;
        if (iterations == 0) ++height;

        if (height < 3) height = 3;

        for (int j = 0; j < height; ++j) {
            BlockPos up = branchPos.above(j + 1);

            if (!level.isEmptyBlock(up)) {
                placeDeadFlower(level, up.below());
                return;
            }

            if (!allNeighborsEmpty(level, up, null)) {
                placeDeadFlower(level, up);
                return;
            }

            if (!level.isEmptyBlock(up.above())) {
                placeDeadFlower(level, up);
                return;
            }

            level.setBlock(up, plantState(level, up), 2);
            level.setBlock(up.below(), plantState(level, up.below()), 2);
        }

        boolean branched = false;
        BlockPos top = branchPos.above(height);

        if (iterations < 4) {
            int tries = random.nextInt(4);
            if (iterations == 0) ++tries;

            for (int k = 0; k < tries; ++k) {
                Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                BlockPos side = top.relative(direction);

                if (
                        Math.abs(side.getX() - originalBranchPos.getX()) < maxHorizontalDistance &&
                                Math.abs(side.getZ() - originalBranchPos.getZ()) < maxHorizontalDistance &&
                                hasThreeStemBlocksBelow(level, top) &&
                                level.isEmptyBlock(side) &&
                                level.isEmptyBlock(side.below()) &&
                                allNeighborsEmpty(level, side, direction.getOpposite())
                ) {
                    branched = true;
                    level.setBlock(side, plantState(level, side), 2);
                    level.setBlock(side.relative(direction.getOpposite()), plantState(level, side.relative(direction.getOpposite())), 2);
                    aoe$growTreeRecursive(level, side, random, originalBranchPos, maxHorizontalDistance, iterations + 1);
                }
            }
        }

        if (!branched) {
            BlockPos flowerPos = top.above();

            if (level.isEmptyBlock(flowerPos)) {
                placeDeadFlower(level, flowerPos);
            } else {
                placeDeadFlower(level, top);
            }
        }
    }
    @Unique
    private static void refreshPlantIfPresent(LevelAccessor level, BlockPos pos) {
        if (level.getBlockState(pos).is(Blocks.CHORUS_PLANT)) {
            level.setBlock(pos, plantState(level, pos), 2);
        }
    }
}