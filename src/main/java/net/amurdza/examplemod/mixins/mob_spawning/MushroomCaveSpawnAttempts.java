package net.amurdza.examplemod.mixins.mob_spawning;

import net.amurdza.examplemod.registry.ModMobCategories;
import net.amurdza.examplemod.worldgen.MushroomCaveSpawns;
import net.amurdza.examplemod.worldgen.DocumentedCaveSpawns;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NaturalSpawner.class)
public abstract class MushroomCaveSpawnAttempts {
    @Redirect(method = "spawnForChunk", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/MobCategory;isPersistent()Z"))
    private static boolean aoemod$moreFrequentCaveAttempts(MobCategory category, ServerLevel level,
            LevelChunk chunk, NaturalSpawner.SpawnState state, boolean animals, boolean monsters, boolean passiveTick) {
        // Only lift the passive 400-tick gate on our one-second schedule. The
        // surrounding native loop still enforces gamerules and both mob caps.
        boolean caveFauna = category == ModMobCategories.MUSHROOM_CAVE && GameplayDimensions.is(level, GameplayDimensions.SNOWY)
                || category == ModMobCategories.DEEP_DARK && GameplayDimensions.is(level, GameplayDimensions.DESERT)
                || ModMobCategories.isNetherLivestock(category) && GameplayDimensions.is(level, GameplayDimensions.NETHER);
        if (caveFauna && MushroomCaveSpawns.isAttemptTick(level.getGameTime())) return false;
        return category.isPersistent();
    }

    @Shadow
    private static BlockPos getRandomPosWithin(Level level, LevelChunk chunk) {
        throw new AssertionError();
    }

    @Redirect(method = "spawnCategoryForChunk", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/NaturalSpawner;getRandomPosWithin(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/chunk/LevelChunk;)Lnet/minecraft/core/BlockPos;"))
    private static BlockPos aoemod$chooseMyceliumFloor(Level level, LevelChunk chunk,
            MobCategory category, ServerLevel server, LevelChunk spawningChunk,
            NaturalSpawner.SpawnPredicate predicate, NaturalSpawner.AfterSpawnCallback afterSpawn) {
        BlockPos candidate;
        if (category == ModMobCategories.MUSHROOM_CAVE) {
            candidate = MushroomCaveSpawns.findSpawnPosition(server, chunk.getPos(), server.random);
        } else if (DocumentedCaveSpawns.usesFloorSampling(server, category)) {
            candidate = DocumentedCaveSpawns.findSpawnPosition(server, chunk, category, server.random);
        } else return getRandomPosWithin(level, chunk);
        // Native NaturalSpawner retains all cap, distance, biome, collision and pack checks.
        return candidate == null ? new BlockPos(chunk.getPos().getMinBlockX(), level.getMinBuildHeight(),
                chunk.getPos().getMinBlockZ()) : candidate;
    }
}
