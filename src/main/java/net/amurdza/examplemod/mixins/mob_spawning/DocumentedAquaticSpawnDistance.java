package net.amurdza.examplemod.mixins.mob_spawning;

import net.amurdza.examplemod.worldgen.AquaticSpawnDistanceRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NaturalSpawner.class)
public final class DocumentedAquaticSpawnDistance {
    @Redirect(method = "isValidSpawnPostitionForType", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/EntityType;getCategory()Lnet/minecraft/world/entity/MobCategory;"))
    private static MobCategory aoemod$aquaticCandidateRange(EntityType<?> type, ServerLevel level,
            MobCategory category, StructureManager structures, ChunkGenerator generator,
            MobSpawnSettings.SpawnerData data, BlockPos.MutableBlockPos pos, double distance) {
        return AquaticSpawnDistanceRules.category(level, type);
    }

    @Redirect(method = "isValidPositionForMob", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/EntityType;getCategory()Lnet/minecraft/world/entity/MobCategory;"))
    private static MobCategory aoemod$aquaticPositionRange(EntityType<?> type, ServerLevel level, Mob mob, double distance) {
        return AquaticSpawnDistanceRules.category(level, type);
    }
}
