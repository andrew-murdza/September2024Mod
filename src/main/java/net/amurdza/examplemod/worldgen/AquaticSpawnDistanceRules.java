package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.LevelReader;

/** Match each aquatic budget's spawn and despawn range: common fish and bottom fauna 64 blocks, larger creatures 128. */
public final class AquaticSpawnDistanceRules {
    private AquaticSpawnDistanceRules() { }

    public static MobCategory category(LevelReader level, EntityType<?> type) {
        return GameplayDimensions.isCustom(level)
                && (DocumentedFauna.isWaterSpecies(type) || DocumentedFauna.isDeepDarkFauna(type)
                || DocumentedFauna.isNetherFauna(type) || type == EntityType.MOOSHROOM)
                ? DocumentedFauna.spawnCategory(type, GameplayDimensions.is(level, GameplayDimensions.SNOWY)) : type.getCategory();
    }
}
