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
        if (id.getPath().equals("deep_dark")) DeepDarkEcology.removeLegacyFeatures(builder);
        Set<String> vegetation = switch (id.getPath()) {
            // TropicalVegetation owns the rainforest/swamp trees and understory. The old
            // rainforest floor mixed every tropical tree and decorated whole chunks,
            // allowing mangroves/cherries to leak across the actual biome boundaries.
            case "rainforest", "mangrove_swamp" -> Set.of();
            case "cherry_grove" -> Set.of("aoemod:trees_cherry_spaced");
            // TropicalCoralReefs owns spacing and the documented species palette.
            case "coral_reef" -> Set.of();
            case "savanna", "savanna_lake" -> Set.of("aoemod:savanna/forest_floor", "aoemod:savanna/seafloor");
            case "plains", "plains_lake" -> Set.of("aoemod:plains/forest_floor", "minecraft:glow_lichen");
            // SnowyVegetation restores the old grove's exact per-column cover
            // and snowlogs it; native random patches would add a second budget.
            case "snowy_forest" -> Set.of("aoemod:snowy/trees");
            case "dappled_forest" -> Set.of("aoemod:dappled/trees", "aoemod:dappled/red_shrubs");
            case "cold_ocean", "mountain_lake" -> Set.of();
            case "mushroom_caves" -> Set.of("aoemod:mushroom_caves/forest_floor");
            case "desert", "mesa", "desert_lake" -> Set.of("aoemod:desert/forest_floor");
            // NetherVegetation owns the original floor mixtures and the half-chunk giant-fungus roll.
            case "crimson_forest" -> Set.of();
            case "warped_forest" -> Set.of("minecraft:twisting_vines");
            default -> Set.of();
        };
        // Save the grass pass so flowers and wild crops claim their terrain
        // first. The old all-surface grass carpet left them only bare riverbanks.
        var savannaGrass = builder.getGenerationSettings().getFeatures(GenerationStep.Decoration.VEGETAL_DECORATION)
                .stream().filter(feature -> (id.getPath().equals("savanna") || id.getPath().equals("savanna_lake")) && feature.unwrapKey()
                        .map(key -> key.location().toString().equals("aoemod:savanna/grass")).orElse(false))
                .findFirst();
        // The flora pass coats deep-dark surfaces before veins select their exposed faces.
        var deepDarkVeins = builder.getGenerationSettings().getFeatures(GenerationStep.Decoration.VEGETAL_DECORATION)
                .stream().filter(feature -> id.getPath().equals("deep_dark") && feature.unwrapKey()
                        .map(key -> key.location().toString().equals("aoemod:deep_dark/sculk_vein")).orElse(false))
                .findFirst();
        builder.getGenerationSettings().getFeatures(GenerationStep.Decoration.VEGETAL_DECORATION)
                .removeIf(feature -> feature.unwrapKey().map(key -> !vegetation.contains(key.location().toString())).orElse(true));
        if (id.getPath().equals("mushroom_caves")) {
            // One full patch per suitable chunk owns all cave mushrooms. Dependencies may add
            // independent glow-shroom patches in underground decoration steps.
            for (GenerationStep.Decoration step : GenerationStep.Decoration.values()) {
                builder.getGenerationSettings().getFeatures(step).removeIf(feature ->
                        feature.unwrapKey().map(key -> !vegetation.contains(key.location().toString())).orElse(true)
                                && !(feature.value().feature().value().feature() instanceof net.minecraft.world.level.levelgen.feature.OreFeature)
                                && (mushroomFeatureName(feature.unwrapKey().map(key -> key.location().getPath()).orElse(""))
                                || mushroomFeatureName(feature.value().feature().unwrapKey()
                                        .map(key -> key.location().getPath()).orElse(""))));
            }
        }
        if (Set.of("desert", "mesa", "desert_lake", "deep_dark").contains(id.getPath())) {
            // Descent tunnels own both mushroom patches. Remove other mods'
            // independent mushroom features even outside the vegetation step.
            for (GenerationStep.Decoration step : GenerationStep.Decoration.values()) {
                builder.getGenerationSettings().getFeatures(step).removeIf(feature ->
                        mushroomFeatureName(feature.unwrapKey().map(key -> key.location().getPath()).orElse(""))
                                || mushroomFeatureName(feature.value().feature().unwrapKey()
                                        .map(key -> key.location().getPath()).orElse("")));
            }
        }
        if (Set.of("crimson_forest", "warped_forest", "soul_sand_valley", "basalt_deltas").contains(id.getPath())) {
            for (GenerationStep.Decoration step : GenerationStep.Decoration.values()) {
                builder.getGenerationSettings().getFeatures(step).removeIf(feature -> feature.unwrapKey().map(key -> Set.of(
                        "minecraft:brown_mushroom_nether", "minecraft:red_mushroom_nether", "minecraft:patch_crimson_roots",
                        "minecraft:patch_nether_wart", "minecraft:weeping_vines", "minecraft:basalt_blobs", "minecraft:basalt_pillar",
                        "minecraft:delta", "minecraft:lake_lava_surface", "minecraft:lake_lava_underground",
                        "minecraft:spring_lava", "minecraft:spring_lava_frozen", "minecraft:spring_open",
                        "minecraft:spring_closed", "minecraft:spring_closed_double").contains(key.location().toString())).orElse(false));
            }
        }
        if (id.getPath().equals("basalt_deltas")) {
            // Third-party additions run before this final pass. Only in-rock
            // ore/blob features and hanging glowstone belong on the dry floor;
            // the authored flora pass supplies powder cane and the lava bed.
            for (GenerationStep.Decoration step : GenerationStep.Decoration.values()) {
                builder.getGenerationSettings().getFeatures(step).removeIf(feature -> {
                    var configured = feature.value().feature().value().feature();
                    return !(configured instanceof net.minecraft.world.level.levelgen.feature.OreFeature)
                            && !(configured instanceof net.minecraft.world.level.levelgen.feature.ScatteredOreFeature)
                            && !(configured instanceof net.minecraft.world.level.levelgen.feature.GlowstoneFeature);
                });
            }
        }
        var spawns = builder.getMobSpawnSettings();
        for (MobCategory category : MobCategory.values()) {
            spawns.getSpawner(category).clear();
        }
        for (DocumentedFauna.Species species : DocumentedFauna.forBiome(id.getPath())) {
            EntityType<?> type = species.type();
            if (type == null) continue;
            // Some neutral livestock use MONSTER; retain all other authored hostile entries.
            for (MobCategory category : MobCategory.values()) spawns.getSpawner(category).removeIf(entry -> entry.type == type);
            var size = DocumentedFauna.groupSize(species);
            boolean aquatic = DocumentedFauna.isWaterSpecies(type);
            int minimum = aquatic ? size.minimum : 2;
            int maximum = aquatic ? size.maximum : species.habitat() == DocumentedFauna.Habitat.LAND ? 4 : 5;
            spawns.addSpawn(DocumentedFauna.spawnCategory(species), new MobSpawnSettings.SpawnerData(type, DocumentedFauna.spawnWeight(species), minimum, maximum));
        }
        for (String monsterId : DocumentedMonsters.forBiome(id.getPath())) {
            var key = new net.minecraft.resources.ResourceLocation(monsterId);
            if (!net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.containsKey(key)) continue;
            EntityType<?> type = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(key);
            spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(type,
                    DocumentedMonsters.weight(monsterId), 1, monsterId.equals("minecraft:warden") ? 1 : 3));
        }
        if (Set.of("desert", "mesa", "desert_lake").contains(id.getPath())) {
            // Keep cave-floor ecology ahead of the surface cactus/dead-bush pass.
            builder.getGenerationSettings().getFeatures(GenerationStep.Decoration.VEGETAL_DECORATION).add(0, flora);
        } else {
            builder.getGenerationSettings().addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, flora);
        }
        savannaGrass.ifPresent(grass -> builder.getGenerationSettings()
                .addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, grass));
        deepDarkVeins.ifPresent(veins -> builder.getGenerationSettings()
                .addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, veins));
    }

    private static boolean mushroomFeatureName(String path) {
        return path.contains("mushroom") || path.contains("mushgloom") || path.contains("glow_shroom");
    }

    @Override
    public Codec<? extends BiomeModifier> codec() { return CODEC; }
}
