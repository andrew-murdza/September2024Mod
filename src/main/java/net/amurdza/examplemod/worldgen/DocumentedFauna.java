package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Natural fauna selected in the six dimension documents. Spawn eggs and breeding never use this policy. */
public final class DocumentedFauna {
    public enum Habitat { LAND, SMALL_WATER, LARGE_WATER, BOTTOM_WATER, SMALL_LAVA, LARGE_LAVA, LAVA_SURFACE }
    public enum Depth { ANY, SHALLOW, DEEP }
    public record Species(ResourceLocation id, Habitat habitat, Depth depth) {
        public EntityType<?> type() { return ForgeRegistries.ENTITY_TYPES.containsKey(id) ? ForgeRegistries.ENTITY_TYPES.getValue(id) : null; }
    }

    public static final int DEEP_WATER_THRESHOLD = 24;
    private static final Map<String, List<Species>> BIOMES = new LinkedHashMap<>();

    static {
        land("rainforest", "biodiverse:cassowary", "aoemod:arthropleura", "alexsmobs:gorilla", "alexsmobs:elephant", "minecraft:ocelot", "minecraft:parrot");
        land("mangrove_swamp", "biodiverse:capybara", "alexsmobs:anaconda", "wildernature:flamingo", "minecraft:frog");
        land("cherry_grove", "minecraft:rabbit", "minecraft:fox", "minecraft:panda", "minecraft:cat");
        land("coral_reef", "minecraft:turtle");
        water("coral_reef", Habitat.SMALL_WATER, Depth.ANY, "upgrade_aquatic:lionfish", "minecraft:tropical_fish", "angling:anomalocaris", "minecraft:pufferfish");
        water("coral_reef", Habitat.LARGE_WATER, Depth.ANY, "angling:mahi_mahi", "hybrid_aquatic:octopus", "aoemod:nautilus", "hybrid_aquatic:umbrella_octopus");
        water("coral_reef", Habitat.BOTTOM_WATER, Depth.ANY, "hybrid_aquatic:lightfoot_crab");
        water("mangrove_swamp", Habitat.SMALL_WATER, Depth.ANY, "minecraft:tropical_fish", "angling:anomalocaris", "minecraft:pufferfish", "hybrid_aquatic:trout", "minecraft:axolotl");
        water("mangrove_swamp", Habitat.LARGE_WATER, Depth.ANY, "hybrid_aquatic:octopus", "finsandtails:rubber_belly_glider", "collectorsreap:tiger_prawn", "hybrid_aquatic:umbrella_octopus");

        land("savanna", "biodiverse:wildebeest", "biodiverse:oryx", "alexsmobs:bison", "alexsmobs:emu", "alexsmobs:kangaroo", "twilightforest:boar", "minecraft:rabbit", "minecraft:sheep", "minecraft:horse", "minecraft:llama");
        land("plains", "minecraft:cow", "minecraft:sheep", "minecraft:chicken", "minecraft:pig", "minecraft:rabbit", "minecraft:horse", "minecraft:donkey", "minecraft:wolf");
        for (String biome : List.of("savanna", "plains")) {
            water(biome, Habitat.SMALL_WATER, Depth.ANY, "minecraft:cod", "upgrade_aquatic:perch", "hybrid_aquatic:carp", "finsandtails:swamp_mucker");
            water(biome, Habitat.LARGE_WATER, Depth.ANY, "minecraft:squid");
            water(biome, Habitat.BOTTOM_WATER, Depth.ANY, "alexsmobs:lobster");
        }

        land("snowy_forest", "minecraft:goat", "environmental:reindeer", "minecraft:rabbit", "minecraft:polar_bear", "minecraft:cow", "minecraft:fox");
        land("mushroom_caves", "minecraft:mooshroom");
        for (String biome : List.of("snowy_forest", "mushroom_caves")) {
            water(biome, Habitat.SMALL_WATER, Depth.SHALLOW, "bountiful_critters:krill", "minecraft:salmon", "alexsmobs:flying_fish", "hybrid_aquatic:needlefish");
            water(biome, Habitat.LARGE_WATER, Depth.SHALLOW, "minecraft:squid", "hybrid_aquatic:arrow_squid", "hybrid_aquatic:tuna", "minecraft:dolphin", "alexsmobs:orca", "hybrid_aquatic:bull_shark", "aoemod:swordfish");
            water(biome, Habitat.SMALL_WATER, Depth.DEEP, "angling:anglerfish", "hybrid_aquatic:dragonfish", "alexscaves:lanternfish", "alexscaves:tripodfish", "aoemod:end_fish", "betterend:cubozoa", "alexscaves:gossamer_worm");
            water(biome, Habitat.LARGE_WATER, Depth.DEEP, "minecraft:glow_squid", "hybrid_aquatic:vampire_squid", "alexsmobs:giant_squid");
            water(biome, Habitat.BOTTOM_WATER, Depth.DEEP, "hybrid_aquatic:spider_crab", "aquatica:isopod");
            water(biome, Habitat.LARGE_WATER, Depth.ANY, "aoemod:nautilus");
        }

        for (String biome : List.of("desert", "mesa")) land(biome, "minecraft:rabbit", "alexsmobs:jerboa", "minecraft:camel");
        land("deep_dark", "outer_end:stalker");
        for (String biome : List.of("desert", "mesa", "deep_dark")) {
            water(biome, Habitat.SMALL_WATER, Depth.ANY, "alexsmobs:triops", "finsandtails:golden_river_ray", "ambientadditions:blue_spotted_stingray", "aoemod:end_fish");
            water(biome, Habitat.LARGE_WATER, Depth.ANY, "hybrid_aquatic:hound_shark");
            water(biome, Habitat.BOTTOM_WATER, Depth.ANY, "ambientadditions:shame_faced_crab");
        }

        for (String biome : List.of("crimson_forest", "warped_forest", "soul_sand_valley", "basalt_deltas")) {
            land(biome, "minecraft:strider");
            water(biome, Habitat.LAVA_SURFACE, Depth.ANY, "alexsmobs:laviathan");
            water(biome, Habitat.SMALL_LAVA, Depth.ANY, "netherdepthsupgrade:magmacubefish", "netherdepthsupgrade:glowdine", "netherdepthsupgrade:blazefish", "netherdepthsupgrade:wither_bonefish");
            water(biome, Habitat.LARGE_LAVA, Depth.ANY, "aoemod:cubozoa");
        }
        land("crimson_forest", "minecraft:hoglin");
        land("warped_forest", "minecraft:hoglin");
    }

    private DocumentedFauna() { }

    private static void land(String biome, String... ids) { water(biome, Habitat.LAND, Depth.ANY, ids); }

    private static void water(String biome, Habitat habitat, Depth depth, String... ids) {
        List<Species> species = BIOMES.computeIfAbsent(biome, ignored -> new ArrayList<>());
        for (String id : ids) species.add(new Species(new ResourceLocation(id), habitat, depth));
    }

    public static List<Species> forBiome(String biome) { return BIOMES.getOrDefault(biome, List.of()); }
    public static boolean hasBiome(String biome) { return BIOMES.containsKey(biome); }

    public static Species find(LevelReader level, BlockPos pos, EntityType<?> type) {
        if (!GameplayDimensions.isCustom(level)) return null;
        ResourceLocation entity = ForgeRegistries.ENTITY_TYPES.getKey(type);
        ResourceLocation biome = level.getBiome(pos).unwrapKey().map(key -> key.location()).orElse(null);
        if (biome == null || !biome.getNamespace().equals("aoemod")) return null;
        for (Species species : forBiome(biome.getPath())) if (species.id().equals(entity)) return species;
        return null;
    }

    public static boolean hasSpace(LevelReader level, BlockPos pos, Species species) {
        if (species.habitat() == Habitat.LAND) {
            return level.getFluidState(pos).isEmpty() && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                    && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
        }
        TagKey<Fluid> fluid = switch (species.habitat()) {
            case SMALL_LAVA, LARGE_LAVA, LAVA_SURFACE -> FluidTags.LAVA;
            default -> FluidTags.WATER;
        };
        if (!level.getFluidState(pos).is(fluid)) return false;
        if (species.habitat() == Habitat.LAVA_SURFACE) return level.getFluidState(pos.below()).is(fluid);
        int clearance = species.habitat() == Habitat.LARGE_WATER || species.habitat() == Habitat.LARGE_LAVA
                || species.habitat() == Habitat.BOTTOM_WATER ? 2 : 1;
        for (int i = 1; i <= clearance; i++) if (!level.getFluidState(pos.above(i)).is(fluid)) return false;
        if (species.habitat() == Habitat.BOTTOM_WATER) {
            if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) return false;
        } else {
            for (int i = 1; i <= clearance; i++) if (!level.getFluidState(pos.below(i)).is(fluid)) return false;
        }
        int depth = waterDepth(level, pos);
        return species.depth() == Depth.ANY || (species.depth() == Depth.DEEP ? depth >= DEEP_WATER_THRESHOLD : depth < DEEP_WATER_THRESHOLD);
    }

    public static int waterDepth(LevelReader level, BlockPos pos) {
        int depth = 0;
        while (depth < DEEP_WATER_THRESHOLD && level.getFluidState(pos.above(depth + 1)).is(FluidTags.WATER)) depth++;
        return depth;
    }
}
