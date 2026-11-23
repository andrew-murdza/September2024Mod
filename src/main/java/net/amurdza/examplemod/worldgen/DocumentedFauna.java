package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Natural fauna selected in the six dimension documents. Spawn eggs and breeding never use this policy. */
public final class DocumentedFauna {
    public enum Habitat { LAND, SMALL_WATER, LARGE_WATER, BOTTOM_WATER, SMALL_LAVA, LARGE_LAVA, LAVA_SURFACE }
    public enum Depth { ANY, SHALLOW, DEEP, BELOW_Y39, AT_OR_ABOVE_Y39 }
    public enum GroupSize {
        SINGLE(1, 1), MEDIUM(2, 5), SCHOOL(4, 8), LARGE(1, 3), VERY_LARGE(1, 2);
        public final int minimum, maximum;
        GroupSize(int minimum, int maximum) { this.minimum = minimum; this.maximum = maximum; }
    }
    public record Species(ResourceLocation id, Habitat habitat, Depth depth) {
        public EntityType<?> type() { return ForgeRegistries.ENTITY_TYPES.containsKey(id) ? ForgeRegistries.ENTITY_TYPES.getValue(id) : null; }
    }

    public static final int DEEP_WATER_THRESHOLD = 24;
    private static final ResourceLocation STALKER = new ResourceLocation("outer_end", "stalker");
    private static final Map<String, List<Species>> BIOMES = new LinkedHashMap<>();
    private static final Set<ResourceLocation> WATER_SPECIES;
    private static final Set<ResourceLocation> WATER_CREATURE_SPECIES;
    private static final Set<ResourceLocation> DEEP_SPECIES;

    static {
        land("rainforest", "alexsmobs:gorilla", "biodiverse:cassowary", "alexsmobs:elephant",
                "minecraft:parrot", "minecraft:ocelot", "aoemod:arthropleura");
        // Capybaras and frogs belong to eight seeded mound pairs, not the passive spawn pool.
        land("mangrove_swamp");
        land("cherry_grove", "minecraft:rabbit", "minecraft:panda", "minecraft:fox", "minecraft:cat");
        for (String biome : List.of("coral_reef", "mangrove_swamp")) {
            water(biome, Habitat.SMALL_WATER, Depth.ANY, "minecraft:tropical_fish", "minecraft:pufferfish");
            water(biome, Habitat.LARGE_WATER, Depth.ANY, "hybrid_aquatic:umbrella_octopus",
                    "minecraft:turtle", "minecraft:axolotl", "minecraft:dolphin");
            water(biome, Habitat.BOTTOM_WATER, Depth.ANY, "hybrid_aquatic:starfish");
        }
        water("coral_reef", Habitat.LARGE_WATER, Depth.ANY, "aoemod:nautilus");

        land("savanna", "biodiverse:wildebeest", "biodiverse:oryx", "alexsmobs:bison", "alexsmobs:emu", "alexsmobs:kangaroo", "twilightforest:boar", "minecraft:rabbit", "minecraft:sheep", "minecraft:horse", "minecraft:llama");
        land("plains", "minecraft:cow", "minecraft:sheep", "minecraft:chicken", "minecraft:pig", "minecraft:rabbit", "minecraft:horse", "minecraft:donkey", "minecraft:wolf");
        for (String biome : List.of("savanna_lake", "plains_lake")) {
            water(biome, Habitat.SMALL_WATER, Depth.ANY, "minecraft:cod", "minecraft:salmon");
            water(biome, Habitat.LARGE_WATER, Depth.ANY, "hybrid_aquatic:octopus");
            water(biome, Habitat.BOTTOM_WATER, Depth.ANY, "alexsmobs:lobster");
        }

        land("snowy_forest", "minecraft:goat", "biodiverse:reindeer", "minecraft:rabbit", "minecraft:polar_bear", "minecraft:fox");
        land("dappled_forest", "aoemod:yak", "minecraft:rabbit", "minecraft:fox");
        // Lakes and oceans have aquatic fauna only; forest wildlife may still wander onto their shores.
        land("cold_ocean");
        land("mountain_lake");
        land("mushroom_caves", "minecraft:mooshroom");
        land("deep_dark", "outer_end:stalker");
        for (String biome : List.of("snowy_forest", "dappled_forest", "mushroom_caves", "cold_ocean", "mountain_lake")) {
            // Consecutive depth ranges: the first deep block is directly below the last shallow block.
            water(biome, Habitat.SMALL_WATER, Depth.SHALLOW, "minecraft:salmon", "alexsmobs:flying_fish");
            water(biome, Habitat.SMALL_WATER, Depth.DEEP, "alexscaves:tripodfish");
            water(biome, Habitat.LARGE_WATER, Depth.SHALLOW, "hybrid_aquatic:arrow_squid", "hybrid_aquatic:tuna",
                    "alexsmobs:orca");
            water(biome, Habitat.LARGE_WATER, Depth.DEEP, "alexscaves:gossamer_worm", "minecraft:glow_squid",
                    "hybrid_aquatic:vampire_squid");
            water(biome, Habitat.BOTTOM_WATER, Depth.BELOW_Y39, "hybrid_aquatic:spider_crab");
            water(biome, Habitat.BOTTOM_WATER, Depth.AT_OR_ABOVE_Y39, "alexsmobs:lobster");
        }

        for (String biome : List.of("cold_ocean", "mountain_lake")) {
            water(biome, Habitat.LARGE_WATER, Depth.SHALLOW, "aoemod:nautilus");
        }

        for (String biome : List.of("desert", "mesa")) land(biome, "minecraft:rabbit", "minecraft:camel");
        for (String biome : List.of("desert_lake", "deep_dark")) {
            water(biome, Habitat.SMALL_WATER, Depth.ANY, "alexsmobs:triops", "finsandtails:golden_river_ray", "ambientadditions:blue_spotted_stingray");
            water(biome, Habitat.LARGE_WATER, Depth.ANY, "hybrid_aquatic:hound_shark");
            water(biome, Habitat.BOTTOM_WATER, Depth.ANY, "hybrid_aquatic:fiddler_crab");
        }

        for (String biome : List.of("crimson_forest", "warped_forest", "soul_sand_valley", "basalt_deltas")) {
            water(biome, Habitat.SMALL_LAVA, Depth.ANY, "netherdepthsupgrade:magmacubefish", "netherdepthsupgrade:glowdine", "netherdepthsupgrade:blazefish", "netherdepthsupgrade:wither_bonefish");
            water(biome, Habitat.LARGE_LAVA, Depth.ANY, "aoemod:cubozoa");
        }
        land("crimson_forest", "minecraft:hoglin");
        land("warped_forest", "minecraft:strider");
        BIOMES.replaceAll((biome, species) -> species.stream()
                .filter(entry -> net.amurdza.examplemod.util.RetainedContent.isKeptEntity(entry.id())).toList());
        WATER_SPECIES = BIOMES.values().stream().flatMap(List::stream)
                .filter(species -> species.habitat() == Habitat.SMALL_WATER
                        || species.habitat() == Habitat.LARGE_WATER || species.habitat() == Habitat.BOTTOM_WATER)
                .map(Species::id).collect(Collectors.toUnmodifiableSet());
        DEEP_SPECIES = BIOMES.values().stream().flatMap(List::stream)
                .filter(species -> species.depth() == Depth.DEEP || species.depth() == Depth.BELOW_Y39)
                .map(Species::id).collect(Collectors.toUnmodifiableSet());
        WATER_CREATURE_SPECIES = BIOMES.values().stream().flatMap(List::stream)
                .filter(species -> species.habitat() == Habitat.LARGE_WATER)
                .map(Species::id).collect(Collectors.toUnmodifiableSet());
    }

    private DocumentedFauna() { }

    private static void land(String biome, String... ids) { water(biome, Habitat.LAND, Depth.ANY, ids); }

    private static void water(String biome, Habitat habitat, Depth depth, String... ids) {
        List<Species> species = BIOMES.computeIfAbsent(biome, ignored -> new ArrayList<>());
        for (String id : ids) species.add(new Species(new ResourceLocation(id), habitat, depth));
    }

    public static List<Species> forBiome(String biome) { return BIOMES.getOrDefault(biome, List.of()); }
    public static boolean hasBiome(String biome) { return BIOMES.containsKey(biome); }

    public static boolean isWaterSpecies(EntityType<?> type) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        return id != null && WATER_SPECIES.contains(id);
    }

    /** Small fish and bottom fauna have their own budget, separate from larger water creatures. */
    public static MobCategory spawnCategory(Species species) {
        return spawnCategory(species.type());
    }

    public static MobCategory spawnCategory(EntityType<?> type) {
        return spawnCategory(type, true);
    }

    static MobCategory spawnCategory(EntityType<?> type, boolean snowyDeepPools) {
        if (type == EntityType.MOOSHROOM) return net.amurdza.examplemod.registry.ModMobCategories.MUSHROOM_CAVE;
        if (type == EntityType.HOGLIN) return net.amurdza.examplemod.registry.ModMobCategories.HOGLIN;
        if (type == EntityType.STRIDER) return net.amurdza.examplemod.registry.ModMobCategories.STRIDER;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (id != null) {
            switch (id.toString()) {
                case "minecraft:tropical_fish", "minecraft:pufferfish",
                     "hybrid_aquatic:umbrella_octopus" -> {
                    return net.amurdza.examplemod.registry.ModMobCategories.COMMON_AQUATIC;
                }
            }
        }
        if (isDeepDarkFauna(type)) return net.amurdza.examplemod.registry.ModMobCategories.DEEP_DARK;
        if (snowyDeepPools && DEEP_SPECIES.contains(id)) return WATER_CREATURE_SPECIES.contains(id)
                ? net.amurdza.examplemod.registry.ModMobCategories.DEEP_WATER_CREATURE
                : net.amurdza.examplemod.registry.ModMobCategories.DEEP_WATER_AMBIENT;
        if (WATER_CREATURE_SPECIES.contains(id)) return MobCategory.WATER_CREATURE;
        return WATER_SPECIES.contains(id) ? MobCategory.WATER_AMBIENT : type.getCategory();
    }

    /** Used both by the next-tick census and by the immediate post-spawn counter. */
    public static MobCategory spawnCategory(Entity entity, MobCategory original) {
        return GameplayDimensions.isCustom(entity.level())
                && (isWaterSpecies(entity.getType()) || entity.getType() == EntityType.MOOSHROOM
                || isDeepDarkFauna(entity.getType()) || isNetherFauna(entity.getType()))
                ? spawnCategory(entity.getType(), GameplayDimensions.is(entity.level(), GameplayDimensions.SNOWY)) : original;
    }

    public static boolean isDeepDarkFauna(EntityType<?> type) {
        return STALKER.equals(ForgeRegistries.ENTITY_TYPES.getKey(type));
    }

    public static boolean isNetherFauna(EntityType<?> type) {
        return type == EntityType.HOGLIN || type == EntityType.STRIDER;
    }

    public static int spawnWeight(Species species) {
        return switch (species.id().toString()) {
            case "minecraft:turtle" -> 2;
            default -> 10;
        };
    }

    public static GroupSize groupSize(Species species) {
        String id = species.id().toString();
        if (id.equals("minecraft:turtle")) return GroupSize.SINGLE;
        // Tiny fish and bottom animals form the largest schools; broad-bodied fish and octopi do not.
        if (id.equals("minecraft:tropical_fish") || id.equals("hybrid_aquatic:starfish")) return GroupSize.SCHOOL;
        if (id.equals("hybrid_aquatic:octopus")) return GroupSize.LARGE;
        if (net.amurdza.examplemod.registry.ModMobCategories.isTropicalFish(spawnCategory(species))) return GroupSize.MEDIUM;
        if (id.equals("alexsmobs:orca")) return GroupSize.VERY_LARGE;
        return species.habitat() == Habitat.LARGE_WATER ? GroupSize.LARGE : GroupSize.MEDIUM;
    }

    public static Species find(LevelReader level, BlockPos pos, EntityType<?> type) {
        if (!GameplayDimensions.isCustom(level)) return null;
        ResourceLocation entity = ForgeRegistries.ENTITY_TYPES.getKey(type);
        ResourceLocation biome = level.getBiome(pos).unwrapKey().map(key -> key.location()).orElse(null);
        if (biome == null || !biome.getNamespace().equals("aoemod")) return null;
        for (Species species : forBiome(biome.getPath())) if (species.id().equals(entity)) return species;
        return null;
    }

    public static boolean hasSpace(LevelReader level, BlockPos pos, Species species) {
        if (!SpawnCollisionSafety.canInspect(level, pos)) return false;
        pos = groundedSpawnPos(level, pos, species);
        if (species.habitat() == Habitat.LAND) {
            if (GameplayDimensions.is(level, GameplayDimensions.PLAINS)
                    && !level.getBlockState(pos.below()).is(BlockTags.DIRT)) return false;
            if (GameplayDimensions.is(level, GameplayDimensions.SNOWY)
                    && GameplayDimensions.biomeIs(level, pos, "snowy_forest")
                    && snowBlockSupport(level, pos)) return false;
            if (species.type() == EntityType.MOOSHROOM
                    && !level.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.MYCELIUM)) return false;
            if (GameplayDimensions.is(level, GameplayDimensions.DESERT)
                    && (species.id().toString().equals("minecraft:camel") || species.id().toString().equals("minecraft:rabbit"))
                    && !level.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.SAND)) return false;
            return !PowderCaneShape.isPlant(level.getBlockState(pos))
                    && !PowderCaneShape.isPlant(level.getBlockState(pos.above()))
                    && !TropicalSpawnSurfaces.blocked(level, pos)
                    && (level.getFluidState(pos).isEmpty() || shallowMangroveLandSpawn(level, pos, species))
                    && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                    && (level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                    || snowyLayerSupport(level, pos));
        }
        TagKey<Fluid> fluid = switch (species.habitat()) {
            case SMALL_LAVA, LARGE_LAVA, LAVA_SURFACE -> FluidTags.LAVA;
            default -> FluidTags.WATER;
        };
        if (!level.getFluidState(pos).is(fluid)) return false;
        if (species.habitat() == Habitat.LAVA_SURFACE) return level.getFluidState(pos.below()).is(fluid);
        // Three connected water blocks admit the four-deep desert pools.
        // PositionCheck separately tests each animal's maximum-size bounds;
        // a five-block blanket rule incorrectly excluded every desert shark.
        // Starfish fit in one water block, including the shallow mangrove fringe.
        boolean smallBottomAnimal = species.habitat() == Habitat.BOTTOM_WATER
                && species.id().toString().equals("hybrid_aquatic:starfish");
        int clearance = smallBottomAnimal ? 0 : species.habitat() == Habitat.LARGE_LAVA || species.habitat() == Habitat.BOTTOM_WATER ? 2 : 1;
        for (int i = 1; i <= clearance; i++) if (!level.getFluidState(pos.above(i)).is(fluid)) return false;
        if (species.habitat() == Habitat.BOTTOM_WATER) {
            if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) return false;
        } else {
            for (int i = 1; i <= clearance; i++) if (!level.getFluidState(pos.below(i)).is(fluid)) return false;
        }
        return switch (species.depth()) {
            case ANY -> true;
            case BELOW_Y39 -> pos.getY() < 39;
            case AT_OR_ABOVE_Y39 -> pos.getY() >= 39;
            case DEEP -> waterDepth(level, pos) >= DEEP_WATER_THRESHOLD;
            case SHALLOW -> waterDepth(level, pos) < DEEP_WATER_THRESHOLD;
        };
    }

    /** Settle bottom animals in their connected water column; shallow waders stand on the bed. */
    public static BlockPos groundedSpawnPos(LevelReader level, BlockPos pos, Species species) {
        if (species.habitat() == Habitat.LAND && GameplayDimensions.is(level, GameplayDimensions.SNOWY)
                && GameplayDimensions.biomeIs(level, pos, "snowy_forest")
                && level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.SNOW)) return pos.above();
        if (species.habitat() == Habitat.BOTTOM_WATER && level.getFluidState(pos).is(FluidTags.WATER)) {
            // NaturalSpawner chooses a random Y in the whole water column. Let a selected
            // bottom animal settle through that same column instead of requiring the one
            // exact seabed Y to win a second, strongly biased lottery.
            BlockPos bottom = pos;
            while (bottom.getY() > level.getMinBuildHeight()) {
                BlockPos below = bottom.below();
                // Waterlogged roots still form a solid floor. Descend through
                // passable plants, but never place the animal inside an obstacle.
                if (!level.getFluidState(below).is(FluidTags.WATER)
                        || !level.getBlockState(below).getCollisionShape(level, below).isEmpty()) break;
                bottom = below;
            }
            return bottom;
        }
        return species.habitat() == Habitat.LAND && level.getFluidState(pos).isEmpty()
                && shallowMangroveLandSpawn(level, pos.below(), species) ? pos.below() : pos;
    }

    public static boolean shallowMangroveLandSpawn(LevelReader level, BlockPos pos, Species species) {
        return species.habitat() == Habitat.LAND && GameplayDimensions.is(level, GameplayDimensions.TROPICAL)
                && GameplayDimensions.biomeIs(level, pos, "mangrove_swamp")
                && level.getFluidState(pos).is(FluidTags.WATER)
                && level.getFluidState(pos.above()).isEmpty()
                && level.getFluidState(pos.below()).isEmpty()
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    private static boolean snowBlockSupport(LevelReader level, BlockPos pos) {
        BlockPos support = pos.below();
        if (level.getBlockState(support).is(net.minecraft.world.level.block.Blocks.SNOW)) support = support.below();
        return level.getBlockState(support).is(net.minecraft.world.level.block.Blocks.SNOW_BLOCK);
    }

    private static boolean snowyLayerSupport(LevelReader level, BlockPos pos) {
        return GameplayDimensions.is(level, GameplayDimensions.SNOWY)
                && GameplayDimensions.biomeIs(level, pos, "snowy_forest")
                && level.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.SNOW)
                && !snowBlockSupport(level, pos)
                && level.getBlockState(pos.below(2)).isFaceSturdy(level, pos.below(2), Direction.UP);
    }

    public static int waterDepth(LevelReader level, BlockPos pos) {
        int depth = 0;
        while (depth < DEEP_WATER_THRESHOLD && level.getFluidState(pos.above(depth + 1)).is(FluidTags.WATER)) depth++;
        return depth;
    }
}
