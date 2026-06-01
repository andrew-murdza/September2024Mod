package net.amurdza.examplemod.worldgen.feature;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.block.BlockPixieHouse;
import com.github.alexthe666.iceandfire.block.IafBlockRegistry;
import com.github.alexthe666.iceandfire.entity.EntityPixie;
import com.github.alexthe666.iceandfire.entity.IafEntityRegistry;
import com.github.alexthe666.iceandfire.world.IafWorldRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import vectorwing.farmersdelight.common.registry.ModBlocks;

public class PixieVillageFeature extends Feature<NoneFeatureConfiguration> {
    private static final int MIN_OFFSET = -4;
    private static final int MAX_OFFSET = 3;

    private static final int INNER_MIN = -2;
    private static final int INNER_MAX = 1;

    private static final int ROAD_MIN = -3;
    private static final int ROAD_MAX = 2;

    private static final int ROAD_CLEAR_HEIGHT = 4;
    private static final int HOUSE_CLEAR_HEIGHT = 3;

    public PixieVillageFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource rand = context.random();
        BlockPos origin = context.origin();

        if (rand.nextInt(IafConfig.spawnPixiesChance) != 0 || !IafWorldRegistry.isFarEnoughFromSpawn(level, origin)) {
            return false;
        }

        boolean placedAnything = false;

        for (int dx = MIN_OFFSET; dx <= MAX_OFFSET; dx++) {
            for (int dz = MIN_OFFSET; dz <= MAX_OFFSET; dz++) {
                if (isRoadRing(dx, dz)) {
                    BlockPos groundPos = surface(level, origin.offset(dx, 0, dz));

                    if (isMossGround(level, groundPos)) {
                        placeRoad(level, groundPos);
                        placedAnything = true;
                    }
                }
            }
        }

        for (int dx = MIN_OFFSET; dx <= MAX_OFFSET; dx++) {
            for (int dz = MIN_OFFSET; dz <= MAX_OFFSET; dz++) {
                Direction inwardFacing = getHouseFacing(dx, dz);

                if (inwardFacing != null) {
                    BlockPos groundPos = surface(level, origin.offset(dx, 0, dz));

                    if (isMossGround(level, groundPos)) {
                        placeHouse(level, rand, groundPos, inwardFacing);
                        placedAnything = true;
                    }
                }
            }
        }

        return placedAnything;
    }

    private static boolean isRoadRing(int dx, int dz) {
        if (isMiddleFourByFour(dx, dz)) {
            return false;
        }

        return dx >= ROAD_MIN
                && dx <= ROAD_MAX
                && dz >= ROAD_MIN
                && dz <= ROAD_MAX
                && (dx == ROAD_MIN || dx == ROAD_MAX || dz == ROAD_MIN || dz == ROAD_MAX);
    }

    private static boolean isMiddleFourByFour(int dx, int dz) {
        return dx >= INNER_MIN
                && dx <= INNER_MAX
                && dz >= INNER_MIN
                && dz <= INNER_MAX;
    }

    private static Direction getHouseFacing(int dx, int dz) {
        if (dz == MIN_OFFSET && dx >= INNER_MIN && dx <= INNER_MAX) {
            return Direction.SOUTH;
        }

        if (dz == MAX_OFFSET && dx >= INNER_MIN && dx <= INNER_MAX) {
            return Direction.NORTH;
        }

        if (dx == MIN_OFFSET && dz >= INNER_MIN && dz <= INNER_MAX) {
            return Direction.EAST;
        }

        if (dx == MAX_OFFSET && dz >= INNER_MIN && dz <= INNER_MAX) {
            return Direction.WEST;
        }

        return null;
    }

    private static void placeRoad(WorldGenLevel level, BlockPos groundPos) {
        level.setBlock(groundPos, ModBlocks.RICH_SOIL.get().defaultBlockState(), 2);
        clearAbove(level, groundPos, ROAD_CLEAR_HEIGHT);
    }

    private static void placeHouse(WorldGenLevel level, RandomSource rand, BlockPos groundPos, Direction facing) {
        BlockPos housePos = groundPos.above();

        clearAbove(level, housePos, HOUSE_CLEAR_HEIGHT);
        level.setBlock(housePos, randomHouseState(rand, facing), 2);
        spawnPixie(level, housePos);
    }

    private static void clearAbove(WorldGenLevel level, BlockPos basePos, int height) {
        for (int y = 1; y <= height; y++) {
            level.setBlock(basePos.above(y), Blocks.AIR.defaultBlockState(), 2);
        }
    }

    private static void spawnPixie(WorldGenLevel level, BlockPos housePos) {
        EntityPixie pixie = IafEntityRegistry.PIXIE.get().create(level.getLevel());

        if (pixie == null) {
            return;
        }

        pixie.finalizeSpawn(
                level,
                level.getCurrentDifficultyAt(housePos),
                MobSpawnType.SPAWNER,
                null,
                null
        );

        pixie.setPos(
                housePos.getX() + 0.5D,
                housePos.getY() + 1.0D,
                housePos.getZ() + 0.5D
        );

        pixie.setPersistenceRequired();
        level.addFreshEntity(pixie);
    }

    private static boolean isMossGround(WorldGenLevel level, BlockPos groundPos) {
        return level.getBlockState(groundPos).is(Blocks.MOSS_BLOCK);
    }

    private static BlockState randomHouseState(RandomSource rand, Direction facing) {
        BlockState houseState;

        switch (rand.nextInt(6)) {
            case 0 -> houseState = IafBlockRegistry.PIXIE_HOUSE_MUSHROOM_RED.get().defaultBlockState();
            case 1 -> houseState = IafBlockRegistry.PIXIE_HOUSE_MUSHROOM_BROWN.get().defaultBlockState();
            case 2 -> houseState = IafBlockRegistry.PIXIE_HOUSE_OAK.get().defaultBlockState();
            case 3 -> houseState = IafBlockRegistry.PIXIE_HOUSE_BIRCH.get().defaultBlockState();
            case 4 -> houseState = IafBlockRegistry.PIXIE_HOUSE_SPRUCE.get().defaultBlockState();
            default -> houseState = IafBlockRegistry.PIXIE_HOUSE_DARK_OAK.get().defaultBlockState();
        }

        return houseState.setValue(BlockPixieHouse.FACING, facing);
    }

    private static BlockPos surface(WorldGenLevel level, BlockPos pos) {
        return level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, pos).below();
    }
}