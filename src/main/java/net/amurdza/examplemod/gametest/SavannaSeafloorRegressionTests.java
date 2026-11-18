package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.SavannaSeafloor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SavannaSeafloorRegressionTests {
    private static final int WATER_Y = 62;
    private SavannaSeafloorRegressionTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void shallowSeafloorFillsOnlyBareChoicesWithKelp(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel world = fixture(helper, blocks, WATER_Y - 1, 1, GameplayDimensions.SAVANNA);
        helper.assertTrue(world.getSeaLevel() == 63 && world.getLevel().getChunkSource().getGenerator().getSeaLevel() == WATER_Y + 1,
                "The fixture and Savanna generator must use restored sea level 63");
        var feature = world.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(new ResourceLocation("aoemod:savanna/water/water_plants_shallow_full"));
        RandomSource random = RandomSource.create(79163);
        int gaps = 0;
        for (int x = 0; x < 32; x++) for (int z = 0; z < 32; z++) {
            BlockPos pos = new BlockPos(x, WATER_Y, z);
            feature.place(world, world.getLevel().getChunkSource().getGenerator(), random, pos);
            if (world.getBlockState(pos).is(Blocks.WATER)) gaps++;
        }
        helper.assertTrue(gaps > 10 && gaps < 90,
                "The original shallow weighted kelp choice must exercise its empty-cell failure; gaps=" + gaps);
        Map<BlockPos, BlockState> originals = new HashMap<>(blocks);
        for (int x = 0; x < 32; x += 16) for (int z = 0; z < 32; z += 16) {
            SavannaSeafloor.fillShallowGaps(world, x, z);
        }
        int kelp = 0;
        for (int x = 0; x < 32; x++) for (int z = 0; z < 32; z++) {
            BlockPos pos = new BlockPos(x, WATER_Y, z);
            BlockState state = world.getBlockState(pos);
            helper.assertFalse(state.is(Blocks.WATER), "A valid one-deep mud column must not stay bare");
            helper.assertTrue(world.getFluidState(pos).isSource() && world.getFluidState(pos).is(FluidTags.WATER)
                            && world.isEmptyBlock(pos.above()) && world.getBlockState(pos.below()).is(Blocks.MUD),
                    "The repair must keep the existing water footprint and mud bed");
            if (state.is(Blocks.KELP)) kelp++;
            if (originals.containsKey(pos)) helper.assertTrue(originals.get(pos).equals(state),
                    "Existing seagrass and pickles must not be replaced by kelp");
        }
        helper.assertTrue(kelp == gaps, "Only originally bare shallow choices should become kelp");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void shallowRepairPreservesDeepPlantsDryBanksAndOtherDimensions(GameTestHelper helper) {
        for (int depth : new int[]{2, 7}) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = fixture(helper, blocks, WATER_Y - depth, depth, GameplayDimensions.SAVANNA);
            var feature = world.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                    .get(new ResourceLocation("aoemod:savanna/water/water_plants_full"));
            RandomSource random = RandomSource.create(1337);
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                feature.place(world, world.getLevel().getChunkSource().getGenerator(), random, new BlockPos(x, WATER_Y + 1 - depth, z));
            }
            Map<BlockPos, BlockState> before = new HashMap<>(blocks);
            helper.assertFalse(SavannaSeafloor.fillShallowGaps(world, 0, 0), "Deeper columns must retain their original palette");
            helper.assertTrue(before.equals(blocks), "Deep seagrass, sponges, pickles and kelp must remain unchanged");
        }
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel dry = fixture(helper, blocks, WATER_Y - 1, 0, GameplayDimensions.SAVANNA);
        helper.assertFalse(SavannaSeafloor.fillShallowGaps(dry, 0, 0), "Dry shore must never receive kelp or new water");
        helper.assertTrue(blocks.isEmpty(), "Repair must not write into air");
        WorldGenLevel other = fixture(helper, blocks, WATER_Y - 1, 1, GameplayDimensions.PLAINS);
        helper.assertFalse(SavannaSeafloor.fillShallowGaps(other, 0, 0), "Shallow repair belongs only to Savanna");
        helper.assertTrue(blocks.isEmpty(), "Other dimensions must remain unchanged");

        WorldGenLevel shallow = fixture(helper, blocks, WATER_Y - 1, 1, GameplayDimensions.SAVANNA);
        BlockPos flowing = new BlockPos(1, WATER_Y, 1), covered = new BlockPos(2, WATER_Y, 1), unsupported = new BlockPos(3, WATER_Y, 1);
        BlockPos sponge = new BlockPos(4, WATER_Y, 1);
        blocks.put(flowing, Blocks.WATER.defaultBlockState().setValue(BlockStateProperties.LEVEL, 1));
        blocks.put(covered.above(), Blocks.STONE.defaultBlockState());
        blocks.put(unsupported.below(), Blocks.AIR.defaultBlockState());
        blocks.put(sponge.below(), Blocks.WET_SPONGE.defaultBlockState());
        SavannaSeafloor.fillShallowGaps(shallow, 0, 0);
        helper.assertTrue(shallow.getBlockState(flowing).is(Blocks.WATER) && !shallow.getFluidState(flowing).isSource(),
                "Flowing water must not be converted to a source by kelp");
        helper.assertTrue(shallow.getBlockState(covered).is(Blocks.WATER) && shallow.getBlockState(unsupported).is(Blocks.WATER),
                "Kelp requires exposed one-deep water over a valid bed");
        helper.assertTrue(shallow.getBlockState(sponge).is(Blocks.WATER)
                        && shallow.getBlockState(sponge.below()).is(Blocks.WET_SPONGE),
                "An existing wet sponge must not be mistaken for a native shallow lake bed");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void generatedSavannaShallowsHaveNoBaldWaterCells(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        var random = level.getChunkSource().randomState();
        var generator = level.getChunkSource().getGenerator();
        // The dedicated fresh QA world makes this distant probe repeatable.
        int originX = 6_000_000, originZ = -6_200_000;
        int chunks = 0, shallow = 0, kelp = 0;
        int waterY = level.getChunkSource().getGenerator().getSeaLevel() - 1;
        search:
        for (int dx = 0; dx < 4096; dx += 16) for (int dz = 0; dz < 4096; dz += 16) {
            int x = originX + dx, z = originZ + dz;
            if (Math.abs(random.router().initialDensityWithoutJaggedness()
                    .compute(new DensityFunction.SinglePointContext(x + 8, waterY + 1, z + 8))) > 1.5) continue;
            int surface = generator.getBaseHeight(x + 8, z + 8, Heightmap.Types.OCEAN_FLOOR_WG, level, random) - 1;
            if (surface != waterY - 1) continue;
            level.getChunk(Math.floorDiv(x, 16), Math.floorDiv(z, 16));
            chunks++;
            for (int localX = x; localX < x + 16; localX++) for (int localZ = z; localZ < z + 16; localZ++) {
                BlockPos pos = new BlockPos(localX, waterY, localZ);
                BlockState state = level.getBlockState(pos), bed = level.getBlockState(pos.below());
                if (!state.getFluidState().is(FluidTags.WATER) || !state.getFluidState().isSource()
                        || !level.isEmptyBlock(pos.above()) || !bed.getFluidState().isEmpty()
                        || !(bed.is(Blocks.MUD) || bed.is(Blocks.GRASS_BLOCK)
                        || bed.is(Blocks.DIRT) || bed.is(Blocks.ROOTED_DIRT))
                        || !bed.isFaceSturdy(level, pos.below(), Direction.UP)
                        || !Blocks.KELP.defaultBlockState().canSurvive(level, pos)) continue;
                shallow++;
                helper.assertFalse(state.is(Blocks.WATER), "Generated Savanna has a bald one-deep cell at " + pos);
                if (state.is(Blocks.KELP)) kelp++;
            }
            if (chunks == 32) break search;
        }
        helper.assertTrue(chunks == 32 && shallow >= 100 && kelp > 0,
                "Generated shoreline audit needs substantial actual shallow water and repaired kelp; chunks="
                        + chunks + ", shallow=" + shallow + ", kelp=" + kelp);
        AOEMod.LOGGER.info("Savanna shallow seafloor: {} valid one-deep cells across {} generated chunks, {} kelp, zero bald cells",
                shallow, chunks, kelp);
        helper.succeed();
    }

    private static WorldGenLevel fixture(GameTestHelper helper, Map<BlockPos, BlockState> blocks,
                                          int floor, int waterDepth, ResourceKey<net.minecraft.world.level.Level> dimension) {
        ServerLevel level = helper.getLevel().getServer().getLevel(dimension);
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod:savanna")));
        java.util.function.Function<BlockPos, BlockState> stateAt = pos -> blocks.getOrDefault(pos,
                pos.getY() <= floor ? Blocks.MUD.defaultBlockState()
                        : pos.getY() <= floor + waterDepth ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState());
        return (WorldGenLevel) Proxy.newProxyInstance(SavannaSeafloorRegressionTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getLevel": return level;
                        case "getSeed": return 79163L;
                        case "getBiome": return biome;
                        case "getHeight": return args == null || args.length == 0 ? level.getHeight() : floor + waterDepth + 1;
                        case "getBlockState": return stateAt.apply((BlockPos) args[0]);
                        case "getBlockEntity": return null;
                        case "getFluidState": return stateAt.apply((BlockPos) args[0]).getFluidState();
                        case "isStateAtPosition": return ((java.util.function.Predicate<BlockState>) args[1]).test(stateAt.apply((BlockPos) args[0]));
                        case "isFluidAtPosition": return ((java.util.function.Predicate<net.minecraft.world.level.material.FluidState>) args[1])
                                .test(stateAt.apply((BlockPos) args[0]).getFluidState());
                        case "isEmptyBlock": return stateAt.apply((BlockPos) args[0]).isAir();
                        case "getRawBrightness", "getMaxLocalRawBrightness": return 15;
                        case "canSeeSky": return true;
                        case "setBlock": blocks.put(((BlockPos) args[0]).immutable(), (BlockState) args[1]); return true;
                        default:
                            try { return method.invoke(level, args); }
                            catch (InvocationTargetException failure) { throw failure.getCause(); }
                    }
                });
    }
}
