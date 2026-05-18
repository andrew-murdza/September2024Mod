package net.amurdza.examplemod.mixins.othermods.iceandfire;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.block.BlockPixieHouse;
import com.github.alexthe666.iceandfire.block.IafBlockRegistry;
import com.github.alexthe666.iceandfire.entity.EntityPixie;
import com.github.alexthe666.iceandfire.entity.IafEntityRegistry;
import com.github.alexthe666.iceandfire.world.IafWorldRegistry;
import com.github.alexthe666.iceandfire.world.gen.WorldGenPixieVillage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import vectorwing.farmersdelight.common.registry.ModBlocks;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = WorldGenPixieVillage.class, remap = false)
public abstract class PixieGroundTerrain {
    @Unique
    private static final int MAX_DETOUR_ATTEMPTS = 6;
    @Unique
    private static final int HOUSE_SPACING = 4;
    @Unique
    private static final int ROAD_CLEAR_HEIGHT = 3;
    @Unique
    private static final int HOUSE_CLEAR_HEIGHT = 1;

    /**
     * @author Andrew Murdza / ChatGPT
     * @reason Replace Ice and Fire's straight-line pixie village roads with roads that avoid trees,
     * avoid houses in roads, clear vegetation, and reduce long empty road segments.
     */
    @Overwrite
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource rand = context.random();
        BlockPos origin = context.origin();

        if (rand.nextInt(IafConfig.spawnPixiesChance) != 0 || !IafWorldRegistry.isFarEnoughFromSpawn(level, origin)) {
            return false;
        }

        int maxRoads = IafConfig.pixieVillageSize + rand.nextInt(5);
        BlockPos buildPosition = september2024Mod$surface(level, origin);

        for (int placedRoads = 0; placedRoads < maxRoads; placedRoads++) {
            int roadLength = 10 + rand.nextInt(15);
            Direction direction = Direction.from2DDataValue(rand.nextInt(4));

            List<BlockPos> road = september2024Mod$buildRoad(level, rand, buildPosition, direction, roadLength);
            if (road.isEmpty()) {
                continue;
            }

            september2024Mod$placeHousesAlongRoad(level, rand, road, direction);

            buildPosition = road.get(rand.nextInt(road.size()));
        }

        return true;
    }

    @Unique
    private static List<BlockPos> september2024Mod$buildRoad(WorldGenLevel level, RandomSource rand, BlockPos start, Direction direction, int roadLength) {
        List<BlockPos> road = new ArrayList<>();
        BlockPos current = september2024Mod$surface(level, start);

        for (int i = 0; i < roadLength; i++) {
            current = september2024Mod$surface(level, current);

            if (september2024Mod$isTreeBlocking(level, current)) {
                BlockPos detour = september2024Mod$findDetour(level, current, direction, rand);
                if (detour == null) {
                    break;
                }
                current = detour;
            }

            september2024Mod$placeRoadBlock(level, current);
            september2024Mod$clearNonSolidAbove(level, current, ROAD_CLEAR_HEIGHT);
            road.add(current);

            current = current.relative(direction);
        }

        return road;
    }

    @Unique
    private static BlockPos september2024Mod$findDetour(WorldGenLevel level, BlockPos blocked, Direction forward, RandomSource rand) {
        Direction left = forward.getCounterClockWise();
        Direction right = forward.getClockWise();

        Direction firstSide = rand.nextBoolean() ? left : right;
        Direction secondSide = firstSide == left ? right : left;

        BlockPos attempt = september2024Mod$tryDetourSide(level, blocked, forward, firstSide);
        if (attempt != null) {
            return attempt;
        }

        return september2024Mod$tryDetourSide(level, blocked, forward, secondSide);
    }

    @Unique
    private static BlockPos september2024Mod$tryDetourSide(WorldGenLevel level, BlockPos blocked, Direction forward, Direction side) {
        for (int distance = 1; distance <= MAX_DETOUR_ATTEMPTS; distance++) {
            BlockPos sidePos = september2024Mod$surface(level, blocked.relative(side, distance));
            BlockPos forwardPos = september2024Mod$surface(level, sidePos.relative(forward));

            if (!september2024Mod$isTreeBlocking(level, sidePos) && !september2024Mod$isTreeBlocking(level, forwardPos)) {
                september2024Mod$placeRoadBlock(level, sidePos);
                september2024Mod$clearNonSolidAbove(level, sidePos, ROAD_CLEAR_HEIGHT);

                september2024Mod$placeRoadBlock(level, forwardPos);
                september2024Mod$clearNonSolidAbove(level, forwardPos, ROAD_CLEAR_HEIGHT);

                return forwardPos.relative(forward);
            }
        }

        return null;
    }

    @Unique
    private static void september2024Mod$placeHousesAlongRoad(WorldGenLevel level, RandomSource rand, List<BlockPos> road, Direction roadDirection) {
        int lastHouseIndex = -999;

        for (int i = 1; i < road.size() - 1; i++) {
            boolean forcedHouse = i - lastHouseIndex >= HOUSE_SPACING;
            boolean randomHouse = rand.nextInt(8) == 0;

            if (!forcedHouse && !randomHouse) {
                continue;
            }

            Direction firstSide = rand.nextBoolean() ? roadDirection.getClockWise() : roadDirection.getCounterClockWise();
            Direction secondSide = firstSide == roadDirection.getClockWise()
                    ? roadDirection.getCounterClockWise()
                    : roadDirection.getClockWise();

            if (september2024Mod$tryPlaceHouse(level, rand, road.get(i), firstSide) || september2024Mod$tryPlaceHouse(level, rand, road.get(i), secondSide)) {
                lastHouseIndex = i;
            }
        }
    }

    @Unique
    private static boolean september2024Mod$tryPlaceHouse(WorldGenLevel level, RandomSource rand, BlockPos roadPos, Direction houseDir) {
        BlockPos supportPos = september2024Mod$surface(level, roadPos.relative(houseDir));
        BlockPos housePos = supportPos.above();

        if (!september2024Mod$canPlaceHouse(level, supportPos, housePos)) {
            return false;
        }

        BlockState houseState = september2024Mod$randomHouseState(rand, houseDir);
        level.setBlock(supportPos, Blocks.COARSE_DIRT.defaultBlockState(), 2);
        level.setBlock(supportPos.below(), Blocks.COARSE_DIRT.defaultBlockState(), 2);
        september2024Mod$clearNonSolidAbove(level, housePos, HOUSE_CLEAR_HEIGHT);
        level.setBlock(housePos, houseState, 2);

        EntityPixie pixie = IafEntityRegistry.PIXIE.get().create(level.getLevel());
        if (pixie != null) {
            pixie.finalizeSpawn(level, level.getCurrentDifficultyAt(housePos), MobSpawnType.SPAWNER,
                    null, null);
            pixie.setPos(housePos.getX() + 0.5D, housePos.getY() + 1.0D, housePos.getZ() + 0.5D);
            pixie.setPersistenceRequired();
            level.addFreshEntity(pixie);
        }

        return true;
    }

    @Unique
    private static boolean september2024Mod$canPlaceHouse(WorldGenLevel level, BlockPos supportPos, BlockPos housePos) {
        BlockState support = level.getBlockState(supportPos);
        BlockState houseSpace = level.getBlockState(housePos);

        if (september2024Mod$isRoadBlock(support) || september2024Mod$isPixieHouse(houseSpace)) {
            return false;
        }

        if (!support.getFluidState().isEmpty()) {
            return false;
        }

        if (houseSpace.canOcclude()) {
            return false;
        }

        return !september2024Mod$isTreeBlocking(level, supportPos);
    }

    @Unique
    private static void september2024Mod$placeRoadBlock(WorldGenLevel level, BlockPos pos) {
        level.setBlock(pos, ModBlocks.RICH_SOIL.get().defaultBlockState(), 2);
    }

    @Unique
    private static void september2024Mod$clearNonSolidAbove(WorldGenLevel level, BlockPos basePos, int height) {
        for (int y = 1; y <= height; y++) {
            BlockPos pos = basePos.above(y);
            BlockState state = level.getBlockState(pos);

            if (!state.canOcclude()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    @Unique
    private static boolean september2024Mod$isTreeBlocking(WorldGenLevel level, BlockPos roadPos) {
        for (int y = 1; y <= ROAD_CLEAR_HEIGHT; y++) {
            BlockState state = level.getBlockState(roadPos.above(y));

            if (state.is(Blocks.OAK_LOG)
                    || state.is(Blocks.BIRCH_LOG)
                    || state.is(Blocks.SPRUCE_LOG)
                    || state.is(Blocks.JUNGLE_LOG)
                    || state.is(Blocks.DARK_OAK_LOG)
                    || state.is(Blocks.ACACIA_LOG)
                    || state.is(Blocks.MANGROVE_LOG)
                    || state.is(Blocks.CHERRY_LOG)
                    || state.getBlock() instanceof LeavesBlock) {
                return true;
            }
        }

        return false;
    }

    @Unique
    private static boolean september2024Mod$isRoadBlock(BlockState state) {
        return state.is(Blocks.DIRT_PATH)
                || state.is(Blocks.SPRUCE_PLANKS)
                || state.is(ModBlocks.RICH_SOIL.get());
    }

    @Unique
    private static boolean september2024Mod$isPixieHouse(BlockState state) {
        return state.is(IafBlockRegistry.PIXIE_HOUSE_MUSHROOM_RED.get())
                || state.is(IafBlockRegistry.PIXIE_HOUSE_MUSHROOM_BROWN.get())
                || state.is(IafBlockRegistry.PIXIE_HOUSE_OAK.get())
                || state.is(IafBlockRegistry.PIXIE_HOUSE_BIRCH.get())
                || state.is(IafBlockRegistry.PIXIE_HOUSE_SPRUCE.get())
                || state.is(IafBlockRegistry.PIXIE_HOUSE_DARK_OAK.get());
    }

    @Unique
    private static BlockState september2024Mod$randomHouseState(RandomSource rand, Direction houseDir) {
        BlockState houseState;

        switch (rand.nextInt(6)) {
            case 0 -> houseState = IafBlockRegistry.PIXIE_HOUSE_MUSHROOM_RED.get().defaultBlockState();
            case 1 -> houseState = IafBlockRegistry.PIXIE_HOUSE_MUSHROOM_BROWN.get().defaultBlockState();
            case 2 -> houseState = IafBlockRegistry.PIXIE_HOUSE_OAK.get().defaultBlockState();
            case 3 -> houseState = IafBlockRegistry.PIXIE_HOUSE_BIRCH.get().defaultBlockState();
            case 4 -> houseState = IafBlockRegistry.PIXIE_HOUSE_SPRUCE.get().defaultBlockState();
            default -> houseState = IafBlockRegistry.PIXIE_HOUSE_DARK_OAK.get().defaultBlockState();
        }

        return houseState.setValue(BlockPixieHouse.FACING, houseDir.getOpposite());
    }

    @Unique
    private static BlockPos september2024Mod$surface(WorldGenLevel level, BlockPos pos) {
        return level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, pos).below();
    }
}