package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DocumentedFauna;
import net.amurdza.examplemod.worldgen.MountainLakeOffset;
import net.amurdza.examplemod.worldgen.SnowyLakeCaves;
import net.amurdza.examplemod.worldgen.SnowyLakeVegetation;
import net.amurdza.examplemod.worldgen.SnowyTerrainProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SnowyLakeExpansionTests {
    private static final Set<String> DEEP_PLANTS = Set.of("alexscaves:ping_pong_sponge",
            "betterend:charnia_light_blue", "betterend:charnia_cyan", "alexscaves:bone_worms");

    @GameTest(template = "plant_gameplay_empty")
    public static void remainingForestPreservesItsShoreAndMountainEdge(GameTestHelper helper) {
        helper.assertTrue(Math.abs(SnowyTerrainProfile.height(MountainLakeOffset.LAKE_WEIRDNESS) - 62.5) < 1e-9,
                "The new halfway forest boundary must meet sea level continuously");
        helper.assertTrue(Math.abs(SnowyTerrainProfile.height(.4) - 69.15) < 1e-9,
                "Converting half the forest width must preserve the existing mountain-edge height");
        helper.assertTrue(Math.abs(SnowyTerrainProfile.height(MountainLakeOffset.LAKE_WEIRDNESS - 1e-8)
                        - SnowyTerrainProfile.height(MountainLakeOffset.LAKE_WEIRDNESS + 1e-8)) < .001,
                "The widened lake must not introduce a cliff at the shore");
        for (double w = .25; w < .31; w += .005) {
            helper.assertTrue(Math.abs((SnowyTerrainProfile.height(w + .001) - SnowyTerrainProfile.height(w)) - .5) < 1e-9,
                    "Widening the lake must quadruple the interim underwater bank grade");
        }
        helper.assertTrue(Math.abs(SnowyTerrainProfile.height(0)
                        - (SnowyTerrainProfile.SHORE_HEIGHT - MountainLakeOffset.MAX_LAKE_DEPTH)) < 1e-9,
                "The shallower Cold Ocean bottom must remain above the underground biome band");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void currentGeneratorRetainsForestAndDeepLakeWater(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        for (long seed : new long[]{0, 12345, 98765}) {
            var random = RandomState.create(generator.generatorSettings().value(),
                    level.registryAccess().lookupOrThrow(Registries.NOISE), seed);
            boolean[] activeProfile = {false};
            random.router().finalDensity().mapAll(function -> {
                if (function instanceof SnowyTerrainProfile profile && !profile.surfaceOnly()) activeProfile[0] = true;
                return function;
            });
            helper.assertTrue(activeProfile[0], "This audit must exercise the active snowy terrain graph");
            int converted = 0, retained = 0;
            search: for (int x = -2048; x <= 2048; x += 32) for (int z = -2048; z <= 2048; z += 32) {
                double w = Math.abs(random.router().ridges().compute(new DensityFunction.SinglePointContext(x, 0, z)));
                var biome = generator.getBiomeSource().getNoiseBiome(x >> 2, 16, z >> 2, random.sampler());
                if (w > .34 && w < .38 && retained < 16) {
                    helper.assertTrue(biome.is(AOEMod.makeID("dappled_forest")),
                            "The outer half of the former forest must remain dappled forest");
                    retained++;
                }
                if (w > .02 && w < .06 && converted < 16) {
                    helper.assertTrue(biome.is(AOEMod.makeID("cold_ocean")) && SnowyLakeCaves.lakeColumn(random, x, z),
                            "New lake columns need matching biome and protected substrate");
                    var column = generator.getBaseColumn(x, z, level, random);
                    int depth = 0;
                    while (depth < 64 && column.getBlock(62 - depth).getFluidState().is(FluidTags.WATER)) depth++;
                    helper.assertTrue(depth >= DocumentedFauna.DEEP_WATER_THRESHOLD,
                            "The lake center must contain actual continuous deep water, seed=" + seed + " depth=" + depth);
                    helper.assertTrue(column.getBlock(62 - depth).isSolid(), "Deep lake water requires a supported bed");
                    converted++;
                }
                if (converted == 16 && retained == 16) break search;
            }
            helper.assertTrue(converted == 16 && retained == 16,
                    "Every sampled seed must exercise both converted and retained halves of the forest");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void freshLakeChunkActuallyGeneratesDeepWaterPlants(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var random = level.getChunkSource().randomState();
        int fresh = 4_800_000 + (int) ((System.currentTimeMillis() / 1000) % 10000) * 512;
        BlockPos anchor = null;
        search: for (int x = fresh; x < fresh + 4096; x += 32) for (int z = -4_900_000; z < -4_895_904; z += 32) {
            double w = Math.abs(random.router().ridges().compute(new DensityFunction.SinglePointContext(x, 0, z)));
            if (w > .025 && w < .04) { anchor = new BlockPos(x, 0, z); break search; }
        }
        helper.assertTrue(anchor != null, "Could not locate a deep lake column");
        var chunk = level.getChunkAt(anchor);
        int deepColumns = 0, plants = 0;
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            int x = chunk.getPos().getMinBlockX() + dx, z = chunk.getPos().getMinBlockZ() + dz;
            double w = Math.abs(random.router().ridges().compute(new DensityFunction.SinglePointContext(x, 0, z)));
            if (w >= MountainLakeOffset.LAKE_WEIRDNESS) continue;
            for (int y = 1; y < 63; y++) {
                BlockPos pos = new BlockPos(x, y, z);
                var state = level.getBlockState(pos);
                if (!level.getFluidState(pos).is(FluidTags.WATER)
                        || !level.getBlockState(pos.below()).isSolid()) continue;
                if (!SnowyLakeVegetation.usesDeepPalette(level, pos)) continue;
                deepColumns++;
                helper.assertTrue(level.getBlockState(pos.below()).is(Blocks.MUD)
                                || level.getBlockState(pos.below()).is(Blocks.CLAY),
                        "Expanded deep lakes must receive the aquatic surface material");
                if (DEEP_PLANTS.contains(ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString())) plants++;
                break;
            }
        }
        helper.assertTrue(deepColumns >= 16 && plants > 0,
                "A freshly decorated lake chunk must include deep habitat and its plants: columns="
                        + deepColumns + ", plants=" + plants + ", chunk=" + chunk.getPos());
        helper.succeed();
    }
}
