package net.amurdza.examplemod.mixins.mob_spawning;

import net.amurdza.examplemod.worldgen.AquaticSpawnAttempts;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NaturalSpawner.class)
public final class DocumentedAquaticSpawnAttempts {
    @Inject(method = "spawnCategoryForChunk", at = @At("HEAD"), cancellable = true)
    private static void aoemod$lessFrequentLargeAquatics(MobCategory category, ServerLevel level, LevelChunk chunk,
            NaturalSpawner.SpawnPredicate filter, NaturalSpawner.AfterSpawnCallback callback, CallbackInfo ci) {
        if (!AquaticSpawnAttempts.allows(level, category, chunk.getPos(), level.getGameTime())) ci.cancel();
    }

    @ModifyVariable(method = "spawnCategoryForChunk", at = @At("STORE"), ordinal = 0)
    private static BlockPos aoemod$sampleTropicalSurfaceWater(BlockPos sample, MobCategory category,
            ServerLevel level, LevelChunk chunk, NaturalSpawner.SpawnPredicate filter, NaturalSpawner.AfterSpawnCallback callback) {
        if ((category != MobCategory.WATER_AMBIENT && category != MobCategory.WATER_CREATURE
                && !net.amurdza.examplemod.registry.ModMobCategories.isTropicalFish(category))
                || !GameplayDimensions.is(level, GameplayDimensions.TROPICAL) || !level.random.nextBoolean()) return sample;
        int sea = level.getSeaLevel() - 1;
        if (!level.getFluidState(sample.atY(sea)).is(FluidTags.WATER)) return sample;
        int bottom = sea;
        while (bottom > sea - 8 && level.getFluidState(sample.atY(bottom - 1)).is(FluidTags.WATER)) bottom--;
        return sample.atY(bottom + level.random.nextInt(sea - bottom + 1));
    }
}
