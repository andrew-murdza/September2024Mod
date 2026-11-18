package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.TropicalCoralReefs;
import net.amurdza.examplemod.worldgen.TropicalVegetation;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.Map;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TropicalDripleafTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void exclusiveDripleafAreasKeepFilledWalkingGapsAcrossChunkEdges(GameTestHelper helper) {
        for (int seed = 0; seed < 8; seed++) for (boolean reverse : new boolean[]{false, true}) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = AquaticPlantGenerationTests.fixture(helper, "mangrove_swamp", blocks, 1);
            // Cover a shared side and a diagonal corner, including negative chunks.
            int smallX = -16, smallZ = -16, largeX = 0, largeZ = seed % 2 == 0 ? -16 : 0;
            for (int step = 0; step < 2; step++) {
                boolean large = (step == 1) != reverse;
                int minX = large ? largeX : smallX, minZ = large ? largeZ : smallZ;
                String species = large ? "minecraft:big_dripleaf" : "minecraft:small_dripleaf";
                int roots = TropicalVegetation.placeDripleafPatches(world, minX, minZ, species, RandomSource.create(seed));
                helper.assertTrue(roots >= 8 && roots <= 14,
                        "Each exclusive area keeps two successful four-to-seven-root patch opportunities: " + species + "; roots=" + roots);
                // Match the real pipeline: finish this chunk's floor before the neighbour decorates.
                RandomSource floorRandom = RandomSource.create(seed + 9043);
                for (int x = minX; x < minX + 16; x++) for (int z = minZ; z < minZ + 16; z++) {
                    TropicalCoralReefs.placeFloorColumn(world, new BlockPos(x, 101, z), floorRandom);
                }
            }
            var smallRoots = blocks.entrySet().stream().filter(entry -> entry.getKey().getY() == 101
                    && entry.getValue().is(Blocks.SMALL_DRIPLEAF)).map(Map.Entry::getKey).toList();
            var largeRoots = blocks.entrySet().stream().filter(entry -> entry.getKey().getY() == 101
                    && entry.getValue().is(Blocks.BIG_DRIPLEAF_STEM)).map(Map.Entry::getKey).toList();
            helper.assertTrue(smallRoots.size() >= 8 && largeRoots.size() >= 8,
                    "Neither the neighbouring patch nor floor fill may erase existing dripleaves");
            for (BlockPos small : smallRoots) {
                helper.assertTrue(small.getX() >= smallX + 2 && small.getX() <= smallX + 13
                                && small.getZ() >= smallZ + 2 && small.getZ() <= smallZ + 13,
                        "Small dripleaf roots stay away from their chunk boundary");
                for (BlockPos large : largeRoots) helper.assertTrue(horizontalDistance(small, large) >= 5,
                        "Opposite types keep an empty separation even across diagonal chunk boundaries");
            }
            for (BlockPos large : largeRoots) {
                helper.assertTrue(large.getX() >= largeX + 2 && large.getX() <= largeX + 13
                                && large.getZ() >= largeZ + 2 && large.getZ() <= largeZ + 13,
                        "Large dripleaf roots stay away from their chunk boundary");
                for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    BlockState gap = world.getBlockState(large.offset(dx, 0, dz));
                    helper.assertTrue(gap.is(Blocks.SEAGRASS) || gap.is(Blocks.SEA_PICKLE),
                            "Every walking gap around a large root must receive base seagrass/pickles, never small dripleaf");
                }
            }
            helper.assertTrue(blocks.values().stream().anyMatch(state -> state.is(Blocks.SEAGRASS))
                            && blocks.values().stream().anyMatch(state -> state.is(Blocks.SEA_PICKLE)),
                    "The ordinary shallow floor keeps both seagrass and pickle decorations");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void oppositeDripleafPatchesRespectExistingRootsInEitherOrder(GameTestHelper helper) {
        BlockPos existing = new BlockPos(15, 101, 15);
        for (boolean largeFirst : new boolean[]{false, true}) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = AquaticPlantGenerationTests.fixture(helper, "mangrove_swamp", blocks, 1);
            blocks.put(existing, (largeFirst ? Blocks.BIG_DRIPLEAF_STEM : Blocks.SMALL_DRIPLEAF).defaultBlockState());
            String next = largeFirst ? "minecraft:small_dripleaf" : "minecraft:big_dripleaf";
            // The six-by-six footprint spans both chunk edges but lies entirely
            // within the existing root's three-block exclusion area.
            int added = TropicalVegetation.placeWetPatch(world, existing, next, RandomSource.create(7));
            helper.assertTrue(added == 0 && blocks.size() == 1,
                    "A later opposite-type patch must not invade an existing dripleaf area in either order");
            // Suitable shallow habitat further away remains available.
            added = TropicalVegetation.placeWetPatch(world, existing.offset(8, 0, 0), next, RandomSource.create(7));
            helper.assertTrue(added >= 4, "Excluding mixed patches must leave distant valid habitat usable");
            for (var entry : blocks.entrySet()) if (entry.getKey().getY() == existing.getY()
                    && !entry.getKey().equals(existing)) helper.assertTrue(horizontalDistance(existing, entry.getKey()) > 3,
                    "The cross-type separation applies at roots even when patch footprints cross chunks");
        }
        helper.succeed();
    }

    private static int horizontalDistance(BlockPos first, BlockPos second) {
        return Math.max(Math.abs(first.getX() - second.getX()), Math.abs(first.getZ() - second.getZ()));
    }
}
