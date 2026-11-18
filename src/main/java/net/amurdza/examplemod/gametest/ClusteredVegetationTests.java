package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.CropTiles;
import net.amurdza.examplemod.worldgen.feature.configs.GridChoiceConfig;
import net.amurdza.examplemod.worldgen.feature.configs.RegionalGridChoiceConfig;
import net.amurdza.examplemod.worldgen.feature.features.RegionalGridChoiceFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ClusteredVegetationTests {
    private ClusteredVegetationTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void cropClustersUseAdjacentUniqueChunksAndExactSpeciesQuotas(GameTestHelper helper) {
        for (long seed : new long[]{0, 50881, -49835}) for (int tx = -2; tx <= 2; tx++) for (int tz = -2; tz <= 2; tz++) {
            for (boolean savanna : new boolean[]{false, true}) {
                var species = savanna ? CropTiles.SAVANNA_SPECIES : CropTiles.PLAINS_SPECIES;
                var plan = CropTiles.plan(seed, tx, tz, species, (x, z) -> true);
                var crops = plan.stream().filter(p -> !p.species().endsWith("_mushroom")).toList();
                int width = 3;
                helper.assertTrue(crops.size() == width * width, "A dry tile needs its complete crop cluster");
                Map<String, Integer> counts = new HashMap<>();
                var chunks = new HashSet<BlockPos>();
                int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
                for (var patch : crops) {
                    int cx = Math.floorDiv(patch.x(), 16), cz = Math.floorDiv(patch.z(), 16);
                    helper.assertTrue(chunks.add(new BlockPos(cx, 0, cz)), "Each crop must own a distinct chunk");
                    minX = Math.min(minX, cx); minZ = Math.min(minZ, cz);
                    maxX = Math.max(maxX, cx); maxZ = Math.max(maxZ, cz);
                    counts.merge(patch.species(), 1, Integer::sum);
                }
                helper.assertTrue(maxX - minX == width - 1 && maxZ - minZ == width - 1,
                        "Crop chunks must form a filled adjacent square");
                helper.assertTrue(counts.size() == width && counts.values().stream().allMatch(n -> n == width),
                        "Cluster species quotas must survive permutation: " + counts);
                for (var patch : plan) {
                    helper.assertTrue(CropTiles.centralFootprint(patch, tx, tz), "Unobstructed crops and mushrooms must prefer the middle 144 square");
                    helper.assertTrue(Math.floorDiv(patch.x() - 2, 288) == tx && Math.floorDiv(patch.x() + 3, 288) == tx
                                    && Math.floorDiv(patch.z() - 2, 288) == tz && Math.floorDiv(patch.z() + 3, 288) == tz,
                            "Patch escaped its tile, including negative coordinates");
                }
                if (savanna) {
                    for (String mushroom : List.of("minecraft:red_mushroom", "minecraft:brown_mushroom")) {
                        helper.assertTrue(plan.stream().filter(p -> p.species().equals(mushroom)).count() == 1
                                        && CropTiles.plantsPerPatch(mushroom) == 10,
                                "Savanna needs one ten-root patch per mushroom species");
                    }
                }
            }
        }
        // A cluster may move within its tile, but cannot scatter across disconnected dry islands.
        helper.assertTrue(CropTiles.plan(42, 0, 0, CropTiles.PLAINS_SPECIES,
                (x, z) -> Math.floorMod(Math.floorDiv(x, 16), 2) == 0).isEmpty(),
                "Isolated dry chunk columns must not create a non-adjacent crop cluster");
        helper.succeed();
    }





    @GameTest(template = "plant_gameplay_empty")
    public static void floodedCentersMoveWholeClustersToSuitableOuterLand(GameTestHelper helper) {
        for (boolean savanna : new boolean[]{false, true}) {
            var plan = CropTiles.plan(50881, -1, 1, savanna ? CropTiles.SAVANNA_SPECIES : CropTiles.PLAINS_SPECIES,
                    (x, z) -> x < -216 || x >= -72 || z < 360 || z >= 504);
            helper.assertTrue(plan.size() == (savanna ? 11 : 9), "Outer dry land must recover the complete tile quota");
            helper.assertTrue(plan.stream().noneMatch(patch -> CropTiles.centralFootprint(patch, -1, 1)),
                    "A wholly flooded center cannot receive any patch");
            var crops = plan.stream().filter(patch -> !patch.species().endsWith("_mushroom")).toList();
            int width = 3;
            helper.assertTrue(crops.stream().mapToInt(patch -> Math.floorDiv(patch.x(), 16)).max().orElseThrow()
                            - crops.stream().mapToInt(patch -> Math.floorDiv(patch.x(), 16)).min().orElseThrow() == width - 1
                            && crops.stream().mapToInt(patch -> Math.floorDiv(patch.z(), 16)).max().orElseThrow()
                            - crops.stream().mapToInt(patch -> Math.floorDiv(patch.z(), 16)).min().orElseThrow() == width - 1,
                    "Moving outside the center must retain the complete adjacent square");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void validCentralSitesWinBeforeEasierOuterLand(GameTestHelper helper) {
        for (long seed = 0; seed < 8; seed++) {
            var crops = CropTiles.plan(seed, 0, 0, CropTiles.SAVANNA_SPECIES, (x, z) ->
                    x < 72 || x >= 216 || z < 72 || z >= 216
                            || x >= 80 && x < 128 && z >= 80 && z < 128
                            && Math.floorMod(x, 16) < 5 && Math.floorMod(z, 16) == 0);
            helper.assertTrue(crops.stream().filter(patch -> !patch.species().endsWith("_mushroom")).count() == 9
                            && crops.stream().filter(patch -> !patch.species().endsWith("_mushroom")).allMatch(patch -> CropTiles.centralFootprint(patch, 0, 0)),
                    "A complete central crop square must beat any easier outer square");
            var mushrooms = CropTiles.plan(seed, 0, 0, CropTiles.SAVANNA_SPECIES, (x, z) ->
                    x < 72 || x >= 216 || z < 72 || z >= 216
                            || (x == 118 || x == 119 || x == 126 || x == 127) && z >= 112 && z <= 116);
            var patches = mushrooms.stream().filter(patch -> patch.species().endsWith("_mushroom")).toList();
            helper.assertTrue(patches.size() == 2 && patches.stream().allMatch(patch -> CropTiles.centralFootprint(patch, 0, 0)),
                    "Central mushroom cells requiring an alternate anchor must beat easy outer anchors");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void plainsAndSavannaTreeOffsetsCoverMinusTwoThroughTwo(GameTestHelper helper) throws Exception {
        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        var method = RegionalGridChoiceFeature.class.getDeclaredMethod("applyOffsetType", BlockPos.class,
                RandomSource.class, GridChoiceConfig.OffsetType.class);
        method.setAccessible(true);
        for (String biome : List.of("plains", "savanna")) {
            var feature = registry.get(new ResourceLocation("aoemod", biome + "/floor/grid_choice"));
            var config = (RegionalGridChoiceConfig) feature.config();
            var seen = new HashSet<BlockPos>();
            RandomSource random = RandomSource.create(8732);
            for (int i = 0; i < 1024; i++) {
                BlockPos pos = (BlockPos) method.invoke(feature.feature(), BlockPos.ZERO, random, config.offsetType);
                helper.assertTrue(pos.getX() >= 5 && pos.getX() <= 9 && pos.getZ() >= 5 && pos.getZ() <= 9,
                        biome + " tree jitter escaped +/-2 around the original (7,7) center");
                seen.add(pos);
            }
            helper.assertTrue(seen.size() == 25, biome + " tree offsets must independently reach all 25 positions");
        }
        helper.succeed();
    }
}
