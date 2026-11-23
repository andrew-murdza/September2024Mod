package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.entity.future.NautilusEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;

/** Loaded residents outside the spawning area must not suppress fauna near exploring players. */
public final class AquaticSpawnCensus {
    private AquaticSpawnCensus() { }

    public static MobCategory category(Entity entity, MobCategory original) {
        MobCategory category = DocumentedFauna.spawnCategory(entity, original);
        // Mooshrooms never naturally despawn and can spawn beyond this aquatic range.
        // Excluding them here repeatedly refilled their budget while the herd survived.
        // Wild nautili inherit Animal's permanent residency. Dropping them from
        // the census when players move away repeatedly refills the same lake.
        // Count them in WATER_CREATURE for their entire loaded lifetime.
        boolean scopedResident = DocumentedFauna.isWaterSpecies(entity.getType())
                && !(entity instanceof NautilusEntity);
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
