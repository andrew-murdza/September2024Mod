package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraftforge.registries.ForgeRegistries;

/** Camp residents are authored once; ambient livestock and undead cannot refill the pens or streets. */
public final class SavannaCampSpawnRules {
    private SavannaCampSpawnRules() { }

    public static boolean blocked(ServerLevelAccessor level, BlockPos pos, EntityType<?> type) {
        if (!GameplayDimensions.is(level, GameplayDimensions.SAVANNA)) return false;
        var id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        String name = id.getPath();
        boolean orc = id.getNamespace().equals("orcz");
        boolean undead = name.contains("zombie") || name.contains("skeleton") || name.contains("husk")
                || name.equals("drowned") || name.equals("stray") || name.equals("phantom") || name.equals("zoglin")
                || name.equals("parched") || name.equals("wither") || name.equals("wraith");
        if (!undead && !orc && type.getCategory() != MobCategory.CREATURE) return false;
        WorldGenLevel world = level instanceof WorldGenLevel generation ? generation : level.getLevel();
        return StructurePlantExclusion.excludes(world, pos);
    }

    public static boolean blocked(ServerLevelAccessor level, BlockPos pos, Mob mob) {
        if (blocked(level, pos, mob.getType())) return true;
        // The position event has a real entity, so modded undead also honor the
        // rule even when their registry name does not mention their mob family.
        if (!GameplayDimensions.is(level, GameplayDimensions.SAVANNA) || mob.getMobType() != MobType.UNDEAD) return false;
        WorldGenLevel world = level instanceof WorldGenLevel generation ? generation : level.getLevel();
        return StructurePlantExclusion.excludes(world, pos);
    }
}
