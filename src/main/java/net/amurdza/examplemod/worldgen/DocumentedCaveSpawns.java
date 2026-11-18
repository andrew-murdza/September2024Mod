package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.registry.ModMobCategories;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import javax.annotation.Nullable;

/** Give enclosed Nether and Desert rooms a floor lottery instead of an arbitrary air Y. */
public final class DocumentedCaveSpawns {
    private DocumentedCaveSpawns() { }

    public static boolean usesFloorSampling(LevelReader level, MobCategory category) {
        return GameplayDimensions.is(level, GameplayDimensions.NETHER)
                && (category == MobCategory.MONSTER || category == MobCategory.CREATURE
                || ModMobCategories.isNetherLivestock(category))
                || GameplayDimensions.is(level, GameplayDimensions.DESERT)
                && (category == MobCategory.MONSTER || category == net.amurdza.examplemod.registry.ModMobCategories.DEEP_DARK);
    }

    @Nullable
    public static BlockPos findSpawnPosition(LevelReader level, LevelChunk chunk, MobCategory category, RandomSource random) {
        if (!usesFloorSampling(level, category)) return null;
        BlockPos selected = null;
        int candidates = 0;
        // Livestock get eight floor columns per attempt; ordinary monsters retain two.
        // Reservoir selection treats every
        // discovered floor equally, including surface floors above the deep dark.
        int columns = category == net.amurdza.examplemod.registry.ModMobCategories.DEEP_DARK
                || ModMobCategories.isNetherLivestock(category) ? 8 : 2;
        for (int column = 0; column < columns; column++) {
            int x = chunk.getPos().getMinBlockX() + random.nextInt(16);
            int z = chunk.getPos().getMinBlockZ() + random.nextInt(16);
            int top = Math.min(level.getMaxBuildHeight() - 2,
                    chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15) + 1);
            BlockPos.MutableBlockPos feet = new BlockPos.MutableBlockPos();
            for (int y = level.getMinBuildHeight() + 1; y <= top; y++) {
                feet.set(x, y, z);
                if (!chunk.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                        || !chunk.getFluidState(feet).isEmpty()
                        || !chunk.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)
                        || !chunk.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                        || !chunk.getFluidState(feet.above()).isEmpty()) continue;
                String biome = level.getBiome(feet).unwrapKey().map(key ->
                        key.location().getNamespace().equals("aoemod") ? key.location().getPath() : "").orElse("");
                boolean eligible = category == MobCategory.MONSTER && !DocumentedMonsters.forBiome(biome).isEmpty();
                if (!eligible) {
                    for (var species : DocumentedFauna.forBiome(biome)) {
                        if (species.habitat() == DocumentedFauna.Habitat.LAND
                                && DocumentedFauna.spawnCategory(species) == category
                                && DocumentedFauna.hasSpace(level, feet, species)) {
                            eligible = true;
                            break;
                        }
                    }
                }
                if (eligible && random.nextInt(++candidates) == 0) selected = feet.immutable();
            }
        }
        return selected;
    }
}
