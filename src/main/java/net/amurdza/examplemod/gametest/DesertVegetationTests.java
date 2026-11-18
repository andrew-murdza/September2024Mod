package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DesertCaveVegetation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DesertVegetationTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void desertFloorDoublesTheSharedPlantBudgetAndRejectsLichen(GameTestHelper helper) {
        var desert = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var floor = desert.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(new ResourceLocation("aoemod:desert/floor/forest_floor"));
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel world = fixture(helper, blocks, pos -> pos.getY() <= 70
                ? Blocks.SAND.defaultBlockState() : Blocks.AIR.defaultBlockState());
        BlockPos origin = new BlockPos(0, 71, 0);
        RandomSource random = RandomSource.create(81723);
        int populated = 0;
        for (int attempt = 0; attempt < 100_000; attempt++) {
            blocks.clear();
            floor.place(world, desert.getChunkSource().getGenerator(), random, origin);
            if (!blocks.isEmpty()) populated++;
        }
        helper.assertTrue(populated >= 3800 && populated <= 4200,
                "Both dry grasses, and dead bushes share the doubled four-percent opportunity: " + populated);
        blocks.clear();
        helper.assertFalse(net.amurdza.examplemod.worldgen.DocumentedFloraFeature.placePlant(
                world, origin, "minecraft:glow_lichen", random), "The desert flora pass cannot add floor lichen");
        helper.assertTrue(blocks.isEmpty(), "Rejected lichen must leave the desert floor unchanged");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void originalFloorBudgetKeepsOnlyRequestedRedSandPlants(GameTestHelper helper) {
        var desert = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var registry = desert.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        var grass = registry.get(new ResourceLocation("aoemod:desert/floor/grass"));
        var floor = registry.get(new ResourceLocation("aoemod:desert/floor/forest_floor"));
        BlockPos origin = new BlockPos(0, 71, 0);
        for (Block ground : new Block[]{Blocks.SAND, Blocks.RED_SAND, Blocks.TERRACOTTA}) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = fixture(helper, blocks, pos -> pos.getY() <= 70
                    ? ground.defaultBlockState() : Blocks.AIR.defaultBlockState());
            Map<String, Integer> counts = new HashMap<>();
            RandomSource random = RandomSource.create(891203);
            for (int attempt = 0; attempt < 46000; attempt++) {
                blocks.clear();
                // Direct provider execution gives a meaningful sample of the rare
                // crop replacements; terracotta exercises the enclosing filter.
                (ground == Blocks.TERRACOTTA ? floor : grass).place(world,
                        desert.getChunkSource().getGenerator(), random, origin);
                for (var state : blocks.values()) counts.merge(ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString(), 1, Integer::sum);
            }
            if (ground == Blocks.SAND) {
                helper.assertTrue(counts.size() == 3, "Sand must support both grasses, and dead bushes: " + counts);
                assertNear(helper, counts, "minecraft:short_dry_grass", 36000, 0.03);
                assertNear(helper, counts, "minecraft:tall_dry_grass", 9000, 0.06);
                assertNear(helper, counts, "minecraft:dead_bush", 1000, 0.12);
            } else if (ground == Blocks.RED_SAND) {
                helper.assertTrue(counts.keySet().equals(java.util.Set.of("minecraft:dead_bush")),
                        "Red sand must reject dry grass and wild peanuts: " + counts);
                assertNear(helper, counts, "minecraft:dead_bush", 1000, 0.12);
            } else helper.assertTrue(counts.isEmpty(), "Terracotta cannot receive desert floor plants: " + counts);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void originalCactusGridUsesSandAndRedSandOnly(GameTestHelper helper) {
        var desert = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var grid = desert.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(new ResourceLocation("aoemod:desert/floor/grid_choice"));
        for (Block ground : new Block[]{Blocks.SAND, Blocks.RED_SAND, Blocks.TERRACOTTA}) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = fixture(helper, blocks, pos -> pos.getY() <= 70
                    ? ground.defaultBlockState() : Blocks.AIR.defaultBlockState());
            for (int cx = 0; cx < 12; cx++) for (int cz = 0; cz < 16; cz++) {
                grid.place(world, desert.getChunkSource().getGenerator(), RandomSource.create(cx * 8191L + cz),
                        new BlockPos(cx * 16, 71, cz * 16));
            }
            long cactus = blocks.values().stream().filter(state -> state.is(Blocks.CACTUS)).count();
            helper.assertTrue(cactus == (ground == Blocks.TERRACOTTA ? 0 : 48 * 3),
                    "The historical 48 three-block cacti per 192 chunks must remain on sand/red sand; found " + cactus);
            for (var entry : blocks.entrySet()) if (entry.getValue().is(Blocks.CACTUS)) {
                helper.assertTrue(entry.getKey().getY() >= 71 && entry.getKey().getY() <= 73,
                        "The original cactus feature must retain its three-block column");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void caveMushroomsUseFiveInSixSquareAndRejectSurfaceAndDeepslate(GameTestHelper helper) {
        var desert = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        Function<BlockPos, BlockState> cave = pos -> (pos.getY() <= 30 || pos.getY() >= 36 && pos.getY() <= 70)
                ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
        BlockPos anchor = new BlockPos(0, 31, 0);
        for (boolean red : new boolean[]{false, true}) {
            Map<BlockPos, BlockState> actual = new HashMap<>();
            int planted = DesertCaveVegetation.placePatch(fixture(helper, actual, cave), desert.getChunkSource().getGenerator(),
                    RandomSource.create(451), anchor, red);
            helper.assertTrue(planted == 5 && actual.size() == 5
                            && actual.values().stream().allMatch(state -> state.is(red ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM)),
                    "Each desert mushroom patch must contain exactly five mushrooms of one species");
            helper.assertTrue(actual.keySet().stream().allMatch(pos -> pos.getX() >= -2 && pos.getX() <= 3
                            && pos.getZ() >= -2 && pos.getZ() <= 3), "All mushrooms must fit within six by six blocks");
            WorldGenLevel world = fixture(helper, actual, cave);
            helper.assertFalse(DesertCaveVegetation.isCaveFloor(world, new BlockPos(0, 71, 0)),
                    "Exposed desert surface must never receive mushrooms");
            actual.clear();
            actual.put(anchor.below(), Blocks.DEEPSLATE.defaultBlockState());
            helper.assertTrue(DesertCaveVegetation.placePatch(world, desert.getChunkSource().getGenerator(),
                    RandomSource.create(451), anchor, red) == 0, "Deepslate support must reject mushrooms");
            actual.put(new BlockPos(0, 16, 0), Blocks.AIR.defaultBlockState());
            helper.assertFalse(DesertCaveVegetation.isCaveFloor(world, new BlockPos(0, 16, 0)),
                    "The entire deepslate transition must stay free of mushrooms");
        }
        helper.succeed();
    }

    private static void assertNear(GameTestHelper helper, Map<String, Integer> counts, String plant, int expected, double tolerance) {
        int actual = counts.getOrDefault(plant, 0);
        helper.assertTrue(Math.abs(actual - expected) <= expected * tolerance,
                "Unexpected historical replacement budget for " + plant + ": " + actual + ", expected near " + expected);
    }

    private static WorldGenLevel fixture(GameTestHelper helper, Map<BlockPos, BlockState> blocks,
                                         Function<BlockPos, BlockState> terrain) {
        var desert = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var biome = desert.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod:desert")));
        Function<BlockPos, BlockState> stateAt = pos -> blocks.getOrDefault(pos, terrain.apply(pos));
        return (WorldGenLevel) Proxy.newProxyInstance(DesertVegetationTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getLevel": return desert;
                        case "getSeed": return 58271L;
                        case "getBiome": return biome;
                        case "getHeight": return args == null || args.length == 0 ? desert.getHeight() : 71;
                        case "getBlockState": return stateAt.apply((BlockPos) args[0]);
                        case "getBlockEntity": return null;
                        case "getFluidState": return stateAt.apply((BlockPos) args[0]).getFluidState();
                        case "isStateAtPosition": return ((java.util.function.Predicate<BlockState>) args[1]).test(stateAt.apply((BlockPos) args[0]));
                        case "isFluidAtPosition": return ((java.util.function.Predicate<net.minecraft.world.level.material.FluidState>) args[1]).test(stateAt.apply((BlockPos) args[0]).getFluidState());
                        case "isEmptyBlock": return stateAt.apply((BlockPos) args[0]).isAir();
                        case "getRawBrightness", "getMaxLocalRawBrightness": return 0;
                        case "canSeeSky": return false;
                        case "setBlock":
                            blocks.put(((BlockPos) args[0]).immutable(), (BlockState) args[1]);
                            return true;
                        default:
                            try { return method.invoke(desert, args); }
                            catch (InvocationTargetException failure) { throw failure.getCause(); }
                    }
                });
    }
}
