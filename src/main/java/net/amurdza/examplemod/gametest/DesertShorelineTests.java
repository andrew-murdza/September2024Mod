package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DesertHeight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DesertShorelineTests {
    private DesertShorelineTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void interpolatedBanksHaveNoDryHolesBelowTheWaterline(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        int wetBanks = 0, dryBanks = 0, originalLakes = 0, currentLakes = 0;
        for (long seed : new long[]{0, 12345, 98765}) {
            var random = RandomState.create(generator.generatorSettings().value(),
                    level.registryAccess().lookupOrThrow(Registries.NOISE), seed);
            for (int x = -768; x <= 768; x += 13) for (int z = -768; z <= 768; z += 17) {
                var point = new DensityFunction.SinglePointContext(x, 0, z);
                double land = random.router().temperature().compute(point);
                boolean originalLake = land <= -.42;
                if (originalLake) originalLakes++;
                double height = random.router().initialDensityWithoutJaggedness().compute(point);
                boolean containsWater = Math.ceil(height + .5) < DesertHeight.SEA_LEVEL;
                if (containsWater) currentLakes++;
                helper.assertTrue(containsWater == originalLake
                                && (random.router().continents().compute(point) == 2) == originalLake,
                        "The shoreline repair must retain the original lake and biome extent at " + x + "," + z);
                if (height < 51.5 || height > 55.5) continue;
                var column = generator.getBaseColumn(x, z, level, random);
                boolean bank = false;
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    bank |= random.router().continents().compute(new DensityFunction.SinglePointContext(
                            x + direction.getStepX(), 0, z + direction.getStepZ())) == 2;
                }
                for (int y = 50; y < DesertHeight.SEA_LEVEL; y++) {
                    var state = column.getBlock(y);
                    if (!originalLake && !bank) {
                        helper.assertFalse(state.is(Blocks.WATER), "An isolated inland descent must stay dry");
                        continue;
                    }
                    helper.assertTrue(state.isSolid() || state.is(Blocks.WATER),
                            "Dry shoreline pocket at " + x + "," + y + "," + z + " seed=" + seed + " height=" + height);
                }
                if (originalLake) wetBanks++; else if (bank) dryBanks++;
            }
        }
        helper.assertTrue(wetBanks > 100 && dryBanks > 100, "Inspect substantial wet and dry shorelines");
        helper.assertTrue(currentLakes == originalLakes && currentLakes > 0,
                "Lake coverage must retain its original size: " + originalLakes + " -> " + currentLakes);
        AOEMod.LOGGER.info("Desert shoreline audit: {} wet banks, {} dry banks, original/current lake columns {} -> {}",
                wetBanks, dryBanks, originalLakes, currentLakes);
        helper.succeed();
    }
    @GameTest(template = "plant_gameplay_empty")
    public static void lowShoreGapsFillEvenWhenNoiseLabelsLandAndInlandOutletsStayDry(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        var random = RandomState.create(generator.generatorSettings().value(),
                level.registryAccess().lookupOrThrow(Registries.NOISE), 12345);
        var chunk = new net.minecraft.world.level.chunk.ProtoChunk(new net.minecraft.world.level.ChunkPos(-365 >> 4, -88 >> 4),
                net.minecraft.world.level.chunk.UpgradeData.EMPTY, level,
                level.registryAccess().registryOrThrow(Registries.BIOME), null);
        var pos = new BlockPos(-365, 50, -88);
        var base = generator.getBaseColumn(pos.getX(), pos.getZ(), level, random);
        helper.assertTrue(base.getBlock(50).isSolid() && base.getBlock(51).isAir() && base.getBlock(53).isAir(),
                "Preserve the real inland descent at seed 12345 that reaches Y50 forty-three blocks from lake water");
        for (int x = 0; x < 8; x++) {
            chunk.setBlockState(pos.east(x), Blocks.SAND.defaultBlockState(), false);
            helper.assertTrue(random.router().continents().compute(new DensityFunction.SinglePointContext(
                    pos.getX() + x, 0, pos.getZ())) != 2, "Exercise an actually dry noise label");
        }
        // Sand at the same height forms two channels separated by one full-height bank.
        // Only the one touching water should become a lake, all the way down to its bed.
        for (int y = 51; y <= 53; y++) {
            chunk.setBlockState(pos.east(3).atY(y), Blocks.SAND.defaultBlockState(), false);
            chunk.setBlockState(pos.east(7).atY(y), Blocks.WATER.defaultBlockState(), false);
        }
        net.amurdza.examplemod.worldgen.DesertLakeSurface.fillWater(chunk, random, generator);
        for (int x = 0; x < 8; x++) for (int y = 51; y <= 53; y++) {
            var state = chunk.getBlockState(pos.east(x).atY(y));
            helper.assertTrue(x < 3 ? state.isAir() : x == 3 ? state.is(Blocks.SAND) : state.is(Blocks.WATER),
                    "Only a source-connected exposed bed should fill, including nominal dry land");
        }
        boolean crossed = false;
        for (int z = -1024; z < 1024 && !crossed; z += 16) {
            int x = 1031;
            if (!generator.getBaseColumn(x, z - 1, level, random).getBlock(53).is(Blocks.WATER)) continue;
            var edge = new net.minecraft.world.level.chunk.ProtoChunk(new net.minecraft.world.level.ChunkPos(x >> 4, z >> 4),
                    net.minecraft.world.level.chunk.UpgradeData.EMPTY, level,
                    level.registryAccess().registryOrThrow(Registries.BIOME), null);
            var bed = new BlockPos(x, 51, z);
            for (int dz = 0; dz < 4; dz++) edge.setBlockState(bed.south(dz), Blocks.SAND.defaultBlockState(), false);
            edge.setBlockState(bed.south(3).above(4), Blocks.STONE.defaultBlockState(), false);
            net.amurdza.examplemod.worldgen.DesertLakeSurface.fillWater(edge, random, generator);
            for (int dz = 0; dz < 4; dz++) {
                var waterline = edge.getBlockState(bed.south(dz).atY(53));
                helper.assertTrue(dz < 3 ? waterline.is(Blocks.WATER) : waterline.isAir(),
                        "Water must cross the unloaded chunk edge and stop at the covered cave");
            }
            crossed = true;
        }
        helper.assertTrue(crossed, "Find natural adjacency to actual water across a chunk edge");
        helper.succeed();
    }
}
