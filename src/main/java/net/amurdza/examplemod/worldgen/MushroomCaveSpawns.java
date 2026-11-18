package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nullable;

/** Sample actual cave floors instead of the full surface-to-bedrock random-Y lottery. */
public final class MushroomCaveSpawns {
    public static final int ATTEMPT_INTERVAL_TICKS = 20;

    private MushroomCaveSpawns() { }

    public static boolean isAttemptTick(long gameTime) {
        return Math.floorMod(gameTime, ATTEMPT_INTERVAL_TICKS) == 0;
    }

    @Nullable
    public static BlockPos findSpawnPosition(LevelReader level, ChunkPos chunk, RandomSource random) {
        if (!GameplayDimensions.is(level, GameplayDimensions.SNOWY)) return null;
        BlockPos selected = null;
        int candidates = 0;
        for (int attempt = 0; attempt < 8; attempt++) {
            int x = chunk.getMinBlockX() + random.nextInt(16);
            int z = chunk.getMinBlockZ() + random.nextInt(16);
            // This is the snowy dimension's underground mushroom biome band.
            for (int y = -32; y < 0; y++) {
                BlockPos feet = new BlockPos(x, y, z);
                if (!level.getBlockState(feet.below()).is(Blocks.MYCELIUM)
                        || !level.getFluidState(feet.above()).isEmpty()
                        || !level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) continue;
                var species = DocumentedFauna.find(level, feet, EntityType.MOOSHROOM);
                if (species != null && DocumentedFauna.hasSpace(level, feet, species)
                        && random.nextInt(++candidates) == 0) selected = feet;
            }
        }
        return selected;
    }
}
