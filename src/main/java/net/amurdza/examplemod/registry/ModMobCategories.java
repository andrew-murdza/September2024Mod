package net.amurdza.examplemod.registry;

import net.minecraft.world.entity.MobCategory;

/** Independent natural-spawn budgets for the authored ecosystems. */
public final class ModMobCategories {
    public static final MobCategory MUSHROOM_CAVE = MobCategory.create(
            "AOEMOD_MUSHROOM_CAVE", "aoemod_mushroom_cave", 40, true, true, 128);
    public static final MobCategory HOGLIN = MobCategory.create(
            "AOEMOD_HOGLIN", "aoemod_hoglin", 20, true, true, 128);
    public static final MobCategory STRIDER = MobCategory.create(
            "AOEMOD_STRIDER", "aoemod_strider", 20, true, true, 128);
    public static final MobCategory DEEP_DARK = MobCategory.create(
            "AOEMOD_DEEP_DARK", "aoemod_deep_dark", 20, true, true, 128);

    /** Twice vanilla ambient water capacity, shared equally by the common tropical roster. */
    public static final MobCategory COMMON_AQUATIC = MobCategory.create(
            "AOEMOD_COMMON_AQUATIC", "aoemod_common_aquatic", 40, true, false, 64);

    public static final MobCategory DEEP_WATER_AMBIENT = aquaticCategory("AMBIENT", MobCategory.WATER_AMBIENT);
    public static final MobCategory DEEP_WATER_CREATURE = aquaticCategory("CREATURE", MobCategory.WATER_CREATURE);

    private static MobCategory aquaticCategory(String name, MobCategory regular) {
        return MobCategory.create("AOEMOD_DEEP_WATER_" + name, "aoemod_deep_" + regular.getName(),
                regular.getMaxInstancesPerChunk(), regular.isFriendly(), regular.isPersistent(), regular.getDespawnDistance());
    }

    public static boolean isTropicalFish(MobCategory category) {
        return category == COMMON_AQUATIC;
    }

    private ModMobCategories() { }

    /** Register before biome builders and NaturalSpawner cache the enum values. */
    public static void initialize() { }

    public static boolean isNetherLivestock(MobCategory category) {
        return category == HOGLIN || category == STRIDER;
    }
}
