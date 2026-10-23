package net.amurdza.examplemod.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;

import java.util.Set;

/** Applies after other mods' normal additions, so their default animals do not leak into AOE ecosystems. */
public record DocumentedBiomeModifier(Holder<PlacedFeature> flora) implements BiomeModifier {
    public static final Codec<DocumentedBiomeModifier> CODEC = PlacedFeature.CODEC.fieldOf("flora")
            .xmap(DocumentedBiomeModifier::new, DocumentedBiomeModifier::flora).codec();

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        var id = biome.unwrapKey().map(key -> key.location()).orElse(null);
        if (id == null || !id.getNamespace().equals("aoemod") || !DocumentedFauna.hasBiome(id.getPath())) return;
        if (phase != Phase.AFTER_EVERYTHING) return;
        Set<String> vegetation = switch (id.getPath()) {
            case "rainforest" -> Set.of("aoemod:rainforest/forest_floor");
            case "mangrove_swamp" -> Set.of("minecraft:trees_mangrove");
            case "cherry_grove" -> Set.of("minecraft:trees_cherry");
            case "coral_reef" -> Set.of("minecraft:warm_ocean_vegetation");
            case "savanna" -> Set.of("aoemod:savanna/forest_floor");
            case "plains" -> Set.of("aoemod:plains/forest_floor");
            case "snowy_forest" -> Set.of("aoemod:mountains/forest_floor");
            case "mushroom_caves" -> Set.of("aoemod:mushroom_caves/forest_floor");
            case "crimson_forest" -> Set.of("minecraft:crimson_fungi", "minecraft:weeping_vines");
            case "warped_forest" -> Set.of("minecraft:warped_fungi", "minecraft:twisting_vines");
            default -> Set.of();
        };
        builder.getGenerationSettings().getFeatures(GenerationStep.Decoration.VEGETAL_DECORATION)
                .removeIf(feature -> feature.unwrapKey().map(key -> !vegetation.contains(key.location().toString())).orElse(true));
        if (Set.of("crimson_forest", "warped_forest", "soul_sand_valley", "basalt_deltas").contains(id.getPath())) {
            builder.getGenerationSettings().getFeatures(GenerationStep.Decoration.UNDERGROUND_DECORATION)
                    .removeIf(feature -> feature.unwrapKey().map(key -> Set.of("minecraft:brown_mushroom_nether",
                            "minecraft:red_mushroom_nether", "minecraft:patch_crimson_roots").contains(key.location().toString())).orElse(false));
        }
        var spawns = builder.getMobSpawnSettings();
        for (MobCategory category : MobCategory.values()) {
            if (category != MobCategory.MONSTER && category != MobCategory.MISC) spawns.getSpawner(category).clear();
        }
        for (DocumentedFauna.Species species : DocumentedFauna.forBiome(id.getPath())) {
            EntityType<?> type = species.type();
            if (type == null) continue;
            // Some neutral livestock use MONSTER; retain all other authored hostile entries.
            for (MobCategory category : MobCategory.values()) spawns.getSpawner(category).removeIf(entry -> entry.type == type);
            int maximum = species.habitat() == DocumentedFauna.Habitat.LAND ? 4 : 5;
            spawns.addSpawn(type.getCategory(), new MobSpawnSettings.SpawnerData(type, 10, 2, maximum));
        }
        builder.getGenerationSettings().addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, flora);
    }

    @Override
    public Codec<? extends BiomeModifier> codec() { return CODEC; }
}
