package net.amurdza.examplemod.util;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;

import java.util.Set;

/** The six independent AOE gameplay regions. Biome identity is used only for local substrate rules. */
public final class GameplayDimensions {
    public static final ResourceKey<Level> TROPICAL = key("tropical_dimension");
    public static final ResourceKey<Level> SAVANNA = key("savanna_dimension");
    public static final ResourceKey<Level> PLAINS = key("plains_dimension");
    public static final ResourceKey<Level> SNOWY = key("snowy_dimension");
    public static final ResourceKey<Level> DESERT = key("desert_dimension");
    public static final ResourceKey<Level> NETHER = key("nether_dimension");
    public static final Set<ResourceKey<Level>> ALL = Set.of(TROPICAL, SAVANNA, PLAINS, SNOWY, DESERT, NETHER);

    private GameplayDimensions() {
    }

    public static ResourceKey<Level> dimension(LevelReader level) {
        if (level instanceof Level world) {
            return world.dimension();
        }
        if (level instanceof ServerLevelAccessor world) {
            return world.getLevel().dimension();
        }
        return null;
    }

    public static boolean is(LevelReader level, ResourceKey<Level> dimension) {
        return dimension.equals(dimension(level));
    }

    public static boolean isCustom(LevelReader level) {
        ResourceKey<Level> dimension = dimension(level);
        return dimension != null && ALL.contains(dimension);
    }

    public static boolean biomeIs(LevelReader level, BlockPos pos, String path) {
        return level.getBiome(pos).is(ResourceKey.create(Registries.BIOME,
                new ResourceLocation(AOEMod.MOD_ID, path)));
    }

    private static ResourceKey<Level> key(String path) {
        return ResourceKey.create(Registries.DIMENSION, new ResourceLocation(AOEMod.MOD_ID, path));
    }
}
