package net.amurdza.examplemod.gametest;

import com.mojang.serialization.JsonOps;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.PlainsCaveVegetation;
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
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.MultifaceGrowthConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlainsCaveVegetationTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void bothMushroomColoursUseCompleteContainedCavePatches(GameTestHelper helper) {
        for (Block mushroom : new Block[]{Blocks.RED_MUSHROOM, Blocks.BROWN_MUSHROOM}) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = fixture(helper, blocks, true, false);
            BlockPos anchor = new BlockPos(-8, 31, -8);
            helper.assertTrue(PlainsCaveVegetation.placePatch(world, anchor, mushroom, RandomSource.create(1691)) == 5,
                    "Both colours must place exactly five mushrooms in a suitable cave");
            helper.assertTrue(blocks.size() == 5, "Only the five mushroom roots should change");
            blocks.forEach((pos, state) -> helper.assertTrue(state.is(mushroom)
                            && pos.getY() == 31 && pos.getX() >= -10 && pos.getX() <= -5
                            && pos.getZ() >= -10 && pos.getZ() <= -5,
                    "Mushrooms must form one same-species patch on the underground floor"));
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void daylightSurvivalDoesNotAllowSurfaceOrTreeCanopyMushrooms(GameTestHelper helper) {
        for (boolean leaves : new boolean[]{false, true}) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = fixture(helper, blocks, false, leaves);
            BlockPos anchor = new BlockPos(8, 31, 8);
            helper.assertTrue(Blocks.BROWN_MUSHROOM.defaultBlockState().canSurvive(world, anchor),
                    "Fixture must reproduce the pack's daylight mushroom survival");
            helper.assertFalse(PlainsCaveVegetation.isCaveFloor(world, anchor),
                    "Surface shade or a tree canopy is not an underground cave");
            helper.assertTrue(PlainsCaveVegetation.placePatch(world, anchor, Blocks.BROWN_MUSHROOM,
                    RandomSource.create(14)) == 0 && blocks.isEmpty(), "No surface mushroom patch may be written");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void crampedCavesNeverLeaveSingletonMushrooms(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel world = fixture(helper, blocks, true, false);
        for (int x = 6; x <= 11; x++) for (int z = 6; z <= 11; z++) {
            for (int y = 31; y <= 34; y++) blocks.put(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState());
        }
        for (int x = 6; x <= 9; x++) blocks.remove(new BlockPos(x, 31, 8));
        Map<BlockPos, BlockState> before = Map.copyOf(blocks);
        helper.assertTrue(PlainsCaveVegetation.placePatch(world, new BlockPos(8, 31, 8), Blocks.RED_MUSHROOM,
                RandomSource.create(14)) == 0, "Four suitable roots must reject the whole five-mushroom patch");
        helper.assertTrue(before.equals(blocks), "An incomplete patch must leave the cave unchanged");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void equallyLikelyMushroomColoursInOneOfThreeCaveColumns(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel world = fixture(helper, blocks, true, false);
        int brown = 0, red = 0;
        for (int sample = 0; sample < 1536; sample++) {
            blocks.clear();
            boolean success = PlainsCaveVegetation.place(new FeaturePlaceContext<>(Optional.empty(), world,
                    world.getLevel().getChunkSource().getGenerator(), RandomSource.create(29003L + sample * 7919L),
                    new BlockPos(sample * 16, 0, -16), NoneFeatureConfiguration.INSTANCE));
            helper.assertTrue(blocks.size() == (success ? 5 : 0), "Only complete successful patches count toward density");
            for (var entry : blocks.entrySet()) helper.assertTrue(entry.getKey().getX() >= sample * 16
                    && entry.getKey().getX() < sample * 16 + 16 && entry.getKey().getZ() >= -16 && entry.getKey().getZ() < 0,
                    "A mushroom patch must stay in its chunk column");
            if (success) {
                if (blocks.values().iterator().next().is(Blocks.BROWN_MUSHROOM)) brown++;
                else red++;
            }
        }
        helper.assertTrue(brown + red >= 450 && brown + red <= 575
                        && brown >= 210 && red >= 210 && Math.abs(brown - red) <= 70,
                "1,536 actual feature calls need about 512 successful patches split evenly between colors: " + brown + "," + red);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void patchCandidatesKeepDensityWithoutLongDroughtsOrDenseRows(GameTestHelper helper) {
        for (long seed : new long[]{0, 1, -1, 29003, 987654321}) {
            for (int tileX = -16; tileX < 16; tileX++) for (int tileZ = -16; tileZ < 16; tileZ++) {
                int count = 0;
                int[] rows = new int[3], columns = new int[3];
                for (int dx = 0; dx < 3; dx++) for (int dz = 0; dz < 3; dz++) {
                    if (PlainsCaveVegetation.patchChunk(seed, tileX * 3 + dx, tileZ * 3 + dz)) {
                        count++;
                        rows[dz]++;
                        columns[dx]++;
                    }
                }
                helper.assertTrue(count == 3, "Every nine-chunk tile must retain three patch opportunities");
                for (int i = 0; i < 3; i++) helper.assertTrue(rows[i] == 1 && columns[i] == 1,
                        "The three candidates must be dispersed across both horizontal axes");
            }
            for (int x = -24; x < 24; x++) for (int z = -24; z < 24; z++) {
                int nearby = 0, row = 0, column = 0;
                for (int dx = 0; dx < 5; dx++) for (int dz = 0; dz < 5; dz++) {
                    if (PlainsCaveVegetation.patchChunk(seed, x + dx, z + dz)) nearby++;
                }
                for (int offset = 0; offset < 3; offset++) {
                    if (PlainsCaveVegetation.patchChunk(seed, x + offset, z)) row++;
                    if (PlainsCaveVegetation.patchChunk(seed, x, z + offset)) column++;
                }
                helper.assertTrue(nearby >= 3, "Every five-by-five neighborhood must have nearby patch opportunities");
                helper.assertTrue(row < 3 && column < 3, "Independent tile borders must not create three crowded adjacent patches");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void suitableCavesKeepThreeCompletePatchesPerTileRegardlessOfDecorationOrder(GameTestHelper helper) {
        Map<BlockPos, BlockState> reference = new HashMap<>();
        for (boolean reverse : new boolean[]{false, true}) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = fixture(helper, blocks, true, false);
            int patches = 0;
            for (int index = 0; index < 81; index++) {
                int cell = reverse ? 80 - index : index;
                int chunkX = cell % 9 - 3, chunkZ = cell / 9 - 3;
                boolean placed = PlainsCaveVegetation.place(new FeaturePlaceContext<>(Optional.empty(), world,
                        world.getLevel().getChunkSource().getGenerator(), RandomSource.create(reverse ? index * 7919L : 19L),
                        new BlockPos(chunkX * 16, 0, chunkZ * 16), NoneFeatureConfiguration.INSTANCE));
                helper.assertTrue(placed == PlainsCaveVegetation.patchChunk(world.getSeed(), chunkX, chunkZ),
                        "Every selected candidate must produce a complete patch on a fully suitable cave floor");
                if (placed) patches++;
            }
            helper.assertTrue(patches == 27 && blocks.size() == 135,
                    "Nine suitable tiles must retain exactly 27 five-mushroom patches");
            if (reverse) helper.assertTrue(blocks.equals(reference),
                    "Caller random state and neighboring decoration order must not change mushroom positions or species");
            else reference.putAll(blocks);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1600)
    public static void generatedPlainsCavesMeetSuccessfulPatchDensity(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        // Each QA run has a fresh isolated world, so fixed coordinates make
        // actual placement density reproducible instead of changing with time.
        int baseX = 68000, baseZ = 193;
        for (int x = baseX; x < baseX + 8; x++) for (int z = baseZ; z < baseZ + 12; z++) level.getChunk(x, z);
        int roots = 0;
        for (int x = baseX * 16; x < (baseX + 8) * 16; x++) for (int z = baseZ * 16; z < (baseZ + 12) * 16; z++) {
            int surface = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR, x, z);
            for (int y = level.getMinBuildHeight() + 1; y < surface - 2; y++) {
                var state = level.getBlockState(new BlockPos(x, y, z));
                if (state.is(Blocks.RED_MUSHROOM) || state.is(Blocks.BROWN_MUSHROOM)) roots++;
            }
        }
        helper.assertTrue(roots >= 95 && roots <= 230,
                "96 newly generated X/Z chunks should retain roughly 32 successful five-mushroom patches; roots=" + roots);
        AOEMod.LOGGER.info("Actual Plains mushroom audit: {} roots, about {} successful patches in 96 chunk columns", roots, roots / 5.0);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void plainsUsesTheExactVanillaCaveLichenFeature(GameTestHelper helper) {
        var biome = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME)
                .get(new ResourceLocation("aoemod:plains"));
        var lichen = helper.getLevel().registryAccess().registryOrThrow(Registries.PLACED_FEATURE)
                .get(new ResourceLocation("minecraft:glow_lichen"));
        var vegetation = biome.getGenerationSettings().features().get(GenerationStep.Decoration.VEGETAL_DECORATION.ordinal());
        helper.assertTrue(vegetation.stream().anyMatch(feature -> feature.value() == lichen),
                "Plains must reference the exact vanilla glow-lichen placed feature");
        var config = (MultifaceGrowthConfiguration) lichen.feature().value().config();
        helper.assertFalse(config.canPlaceOnFloor, "Vanilla cave lichen cannot place on grass floors");
        helper.assertTrue(config.canPlaceOnWall && config.canPlaceOnCeiling, "Cave walls and ceilings must remain eligible");
        helper.assertFalse(config.canBePlacedOn.contains(Blocks.GRASS_BLOCK.builtInRegistryHolder()),
                "Grass cannot be a vanilla glow-lichen substrate");
        helper.succeed();
    }

    private static WorldGenLevel fixture(GameTestHelper helper, Map<BlockPos, BlockState> blocks,
                                         boolean cave, boolean leafCanopy) {
        var plains = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        var biome = plains.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod:plains")));
        java.util.function.Function<BlockPos, BlockState> stateAt = pos -> blocks.getOrDefault(pos,
                pos.getY() <= 30 || cave && pos.getY() >= 45 && pos.getY() <= 70 ? Blocks.STONE.defaultBlockState()
                        : leafCanopy && pos.getY() == 70 ? Blocks.OAK_LEAVES.defaultBlockState() : Blocks.AIR.defaultBlockState());
        return (WorldGenLevel) Proxy.newProxyInstance(PlainsCaveVegetationTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getLevel": return plains;
                        case "getSeed": return 29003L;
                        case "getBiome": return biome;
                        case "getHeight": return args == null || args.length == 0 ? plains.getHeight() : cave || leafCanopy ? 71 : 31;
                        case "getBlockState": return stateAt.apply((BlockPos) args[0]);
                        case "getBlockEntity": return null;
                        case "getFluidState": return stateAt.apply((BlockPos) args[0]).getFluidState();
                        case "isStateAtPosition":
                            return ((java.util.function.Predicate<BlockState>) args[1]).test(stateAt.apply((BlockPos) args[0]));
                        case "isFluidAtPosition":
                            return ((java.util.function.Predicate<net.minecraft.world.level.material.FluidState>) args[1])
                                    .test(stateAt.apply((BlockPos) args[0]).getFluidState());
                        case "isEmptyBlock": return stateAt.apply((BlockPos) args[0]).isAir();
                        case "getRawBrightness", "getMaxLocalRawBrightness": return 15;
                        case "canSeeSky": return true;
                        case "setBlock":
                            blocks.put(((BlockPos) args[0]).immutable(), (BlockState) args[1]);
                            return true;
                        default:
                            try { return method.invoke(plains, args); }
                            catch (InvocationTargetException failure) { throw failure.getCause(); }
                    }
                });
    }
}
