package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;

/** Exclusive natural hostile rosters. Bosses and heart-bound creakings are placed by their homes. */
public final class DocumentedMonsters {
    private static final Map<String, List<String>> BIOMES = Map.ofEntries(
            Map.entry("rainforest", List.of("twilightforest:slime_beetle", "minecraft:spider", "minecraft:cave_spider")),
            Map.entry("mangrove_swamp", List.of("aoemod:bogged", "minecraft:slime", "minecraft:drowned")),
            Map.entry("savanna", List.of("minecraft:skeleton", "minecraft:zombie", "aoemod:skeleton_horseman",
                    "aoemod:zombie_horseman", "orcz:orcsavagemace", "orcz:orcshield")),
            Map.entry("plains", List.of("minecraft:zombie_villager", "minecraft:witch", "minecraft:pillager",
                    "minecraft:vindicator", "minecraft:evoker", "minecraft:ravager")),
            Map.entry("snowy_forest", List.of("minecraft:stray", "aoemod:breeze", "twilightforest:yeti")),
            Map.entry("dappled_forest", List.of("twilightforest:upper_goblin_knight", "twilightforest:blockchain_goblin", "twilightforest:redcap")),
            Map.entry("desert", List.of("minecraft:parched", "minecraft:husk", "minecraft:shulker")),
            Map.entry("mesa", List.of("minecraft:parched", "minecraft:husk", "minecraft:shulker")),
            Map.entry("deep_dark", List.of("minecraft:enderman", "minecraft:endermite", "minecraft:warden")),
            Map.entry("soul_sand_valley", List.of("minecraft:ghast", "minecraft:skeleton")),
            Map.entry("warped_forest", List.of("minecraft:zoglin", "minecraft:zombified_piglin", "minecraft:creeper")),
            Map.entry("basalt_deltas", List.of("minecraft:blaze", "minecraft:magma_cube", "minecraft:wither_skeleton")));

    private DocumentedMonsters() { }

    public static List<String> forBiome(String biome) {
        return BIOMES.getOrDefault(biome, List.of());
    }

    public static boolean allows(LevelReader level, BlockPos pos, EntityType<?> type) {
        if (type == EntityType.SHULKER && !hasShulkerSurface(level, pos)) return false;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        return level.getBiome(pos).unwrapKey().map(key -> key.location().getNamespace().equals("aoemod")
                && forBiome(key.location().getPath()).contains(String.valueOf(id))).orElse(false);
    }

    /** Both the registered placement rule and final regional events must enforce the same attachment surface. */
    public static boolean hasShulkerSurface(LevelReader level, BlockPos pos) {
        if (GameplayDimensions.is(level, GameplayDimensions.DESERT)
                && pos.getY() < DesertHeight.FIRST_TERRACOTTA_Y) return false;
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).is(BlockTags.TERRACOTTA)) return true;
        }
        return false;
    }

    /** Shared with NaturalSpawner so land-dwelling drowned pass its native IN_WATER gate. */
    public static boolean hasSpace(LevelReader level, BlockPos pos) {
        return SpawnCollisionSafety.canInspect(level, pos)
                && !PowderCaneShape.isPlant(level.getBlockState(pos))
                && !PowderCaneShape.isPlant(level.getBlockState(pos.above()))
                && !TropicalSpawnSurfaces.blocked(level, pos) && level.getFluidState(pos).isEmpty()
                && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    public static boolean canSpawn(ServerLevelAccessor level, BlockPos pos, EntityType<?> type) {
        if (!allows(level, pos, type) || level.getDifficulty() == Difficulty.PEACEFUL
                || !hasSpace(level, pos)) return false;
        // Nether light sources are part of the cave palette; do not exclude their own inhabitants.
        return GameplayDimensions.is(level, GameplayDimensions.NETHER)
                || Monster.isDarkEnoughToSpawn(level, pos, level.getRandom());
    }

    public static int weight(String id) {
        return id.equals("minecraft:warden") ? 1 : id.equals("minecraft:ravager") || id.equals("minecraft:evoker") ? 3 : 10;
    }
}
