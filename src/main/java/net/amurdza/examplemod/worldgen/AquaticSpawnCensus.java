package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;

/** Loaded residents outside the spawning area must not suppress fauna near exploring players. */
public final class AquaticSpawnCensus {
    private AquaticSpawnCensus() { }

    public static MobCategory category(Entity entity, MobCategory original) {
        MobCategory category = DocumentedFauna.spawnCategory(entity, original);
        // Mooshrooms never naturally despawn and can spawn beyond this aquatic range.
        // Excluding them here repeatedly refilled their budget while the herd survived.
        boolean scopedResident = DocumentedFauna.isWaterSpecies(entity.getType());
        if (GameplayDimensions.isCustom(entity.level()) && scopedResident) {
            var player = entity.level().getNearestPlayer(entity.getX(), entity.getY(), entity.getZ(), -1, false);
            int range = AquaticSpawnDistanceRules.category(entity.level(), entity.getType()).getDespawnDistance();
            if (player == null || player.distanceToSqr(entity) > range * range) {
                // MISC is skipped by NaturalSpawner's census. Keep the animal itself,
                // including turtles and other residents whose native AI never despawns.
                return MobCategory.MISC;
            }
        }
        return category;
    }
}
