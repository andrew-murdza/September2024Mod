package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.AquaticSpawnAttempts;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AquaticSpawnAttemptTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void largeAquaticPassesRunOncePerFourTicksWithoutRestrictingOtherPools(GameTestHelper helper) {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        for (ChunkPos chunk : new ChunkPos[]{new ChunkPos(0, 0), new ChunkPos(3, -7), new ChunkPos(-12, -9)}) {
            int largeAttempts = 0;
            for (long tick = 101; tick < 105; tick++) {
                if (AquaticSpawnAttempts.allows(tropical, MobCategory.WATER_CREATURE, chunk, tick)) largeAttempts++;
                helper.assertTrue(AquaticSpawnAttempts.allows(tropical, MobCategory.WATER_AMBIENT, chunk, tick)
                                && AquaticSpawnAttempts.allows(tropical, MobCategory.CREATURE, chunk, tick)
                                && AquaticSpawnAttempts.allows(tropical, MobCategory.MONSTER, chunk, tick)
                                && AquaticSpawnAttempts.allows(helper.getLevel(), MobCategory.WATER_CREATURE, chunk, tick),
                        "Small fish, unrelated categories and vanilla dimensions must retain every spawning pass");
            }
            helper.assertTrue(largeAttempts == 1, "Every chunk must get exactly one large-aquatic pass per four ticks");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 200)
    public static void nativeChunkSpawnPassSkipsLargeAnimalsBeforeDrawingACandidate(GameTestHelper helper) {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        int z = 128;
        int openX = 128 - (int) Math.floorMod(tropical.getGameTime() + 128 + (long) z,
                AquaticSpawnAttempts.LARGE_WATER_INTERVAL);
        LevelChunk open = tropical.getChunkAt(new BlockPos(openX * 16, 100, z * 16));
        LevelChunk closed = tropical.getChunkAt(new BlockPos((openX + 1) * 16, 100, z * 16));
        helper.assertFalse(consumesRandom(tropical, closed, MobCategory.WATER_CREATURE),
                "A closed large-aquatic pass must stop before vanilla samples any candidate coordinates");
        helper.assertTrue(consumesRandom(tropical, open, MobCategory.WATER_CREATURE),
                "The large-aquatic open phase must enter the actual vanilla candidate sampler");
        helper.assertTrue(consumesRandom(tropical, closed, MobCategory.WATER_AMBIENT),
                "Small fish must still enter the native sampler during closed large-animal phases");
        LevelChunk outside = helper.getLevel().getChunkAt(helper.absolutePos(BlockPos.ZERO));
        helper.assertTrue(consumesRandom(helper.getLevel(), outside, MobCategory.WATER_CREATURE),
                "The native water-creature sampler must remain unchanged outside custom dimensions");
        helper.succeed();
    }

    private static boolean consumesRandom(ServerLevel level, LevelChunk chunk, MobCategory category) {
        long seed = 8192371L;
        level.random.setSeed(seed);
        long untouched = level.random.nextLong();
        level.random.setSeed(seed);
        NaturalSpawner.spawnCategoryForChunk(category, level, chunk, (type, pos, candidateChunk) -> false,
                (mob, candidateChunk) -> { });
        return level.random.nextLong() != untouched;
    }
}
