package net.amurdza.examplemod.util;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class BiomePlacementRestrictions {
    private static final TagKey<Block> NOT_RAINFOREST = restrictionTag("not_rainforest");
    private static final TagKey<Block> NOT_RAINFOREST_EXCEPTIONS = restrictionTag("not_rainforest_exceptions");

    private static final List<Rule> ONLY_RULES = List.of(
            new Rule("AOE jungle", restrictionTag("aoe_jungle_only"),
                    biome -> biome.is(ModTags.Biomes.aoeJungleBiomes)),
            new Rule("crimson forest", restrictionTag("crimson_only"),
                    biome -> biome.is(ModTags.Biomes.crimsonForestBiomes)),
            new Rule("deep dark", restrictionTag("deep_dark_only"),
                    biome -> biome.is(ModTags.Biomes.deepDarkBiomes)),
            new Rule("desert", restrictionTag("desert_only"),
                    biome -> biome.is(ModTags.Biomes.desertBiomes)),
            new Rule("grove", restrictionTag("grove_only"),
                    biome -> biome.is(ModTags.Biomes.mountainBiomes)),
            new Rule("nether", restrictionTag("nether_only"),
                    biome -> biome.is(ModTags.Biomes.netherBiomes)),
            new Rule("overworld", restrictionTag("overworld_only"),
                    biome -> !biome.is(ModTags.Biomes.netherBiomes)),
            new Rule("plains", restrictionTag("plains_only"),
                    biome -> biome.is(ModTags.Biomes.plainsBiomes)),
            new Rule("rainforest or desert", restrictionTag("rainforest_desert_only"),
                    biome -> biome.is(ModTags.Biomes.tropicalBiomes) || biome.is(ModTags.Biomes.desertBiomes)),
            new Rule("rainforest or grove", restrictionTag("rainforest_grove_only"),
                    biome -> biome.is(ModTags.Biomes.tropicalBiomes) || biome.is(ModTags.Biomes.mountainBiomes)),
            new Rule("rainforest", restrictionTag("rainforest_only"),
                    biome -> biome.is(ModTags.Biomes.tropicalBiomes)),
            new Rule("rainforest or plains", restrictionTag("rainforest_plains_only"),
                    biome -> biome.is(ModTags.Biomes.tropicalBiomes) || biome.is(ModTags.Biomes.plainsBiomes)),
            new Rule("rainforest, savanna, or grove", restrictionTag("rainforest_savanna_grove_only"),
                    biome -> biome.is(ModTags.Biomes.tropicalBiomes)
                            || biome.is(ModTags.Biomes.savannaBiomes)
                            || biome.is(ModTags.Biomes.mountainBiomes)),
            new Rule("rainforest, savanna, plains, or grove", restrictionTag("rainforest_savanna_plains_grove_only"),
                    biome -> biome.is(ModTags.Biomes.tropicalBiomes)
                            || biome.is(ModTags.Biomes.savannaBiomes)
                            || biome.is(ModTags.Biomes.plainsBiomes)
                            || biome.is(ModTags.Biomes.mountainBiomes)),
            new Rule("rainforest, savanna, or plains", restrictionTag("rainforest_savanna_plains_only"),
                    biome -> biome.is(ModTags.Biomes.tropicalBiomes)
                            || biome.is(ModTags.Biomes.savannaBiomes)
                            || biome.is(ModTags.Biomes.plainsBiomes)),
            new Rule("savanna", restrictionTag("savanna_only"),
                    biome -> biome.is(ModTags.Biomes.savannaBiomes)),
            new Rule("soul sand valley", restrictionTag("soul_sand_valley_only"),
                    biome -> biome.is(ModTags.Biomes.soulSandValleyBiomes)),
            new Rule("warped or crimson forest", restrictionTag("warped_crimson_only"),
                    biome -> biome.is(ModTags.Biomes.warpedForestBiomes)
                            || biome.is(ModTags.Biomes.crimsonForestBiomes)),
            new Rule("warped forest", restrictionTag("warped_only"),
                    biome -> biome.is(ModTags.Biomes.warpedForestBiomes))
    );

    private BiomePlacementRestrictions() {
    }

    public static Optional<Component> placementFailureMessage(Level level, BlockPos pos, BlockState state) {
        Holder<Biome> biome = level.getBiome(pos);
        boolean matchedOnlyRule = false;
        boolean allowedByOnlyRule = false;

        for (Rule rule : ONLY_RULES) {
            if (!state.is(rule.blockTag())) {
                continue;
            }

            matchedOnlyRule = true;
            if (rule.allows().test(biome)) {
                allowedByOnlyRule = true;
            }
        }

        if (matchedOnlyRule && !allowedByOnlyRule) {
            return Optional.of(message(state, biome));
        }

        if (!allowedByOnlyRule
                && biome.is(ModTags.Biomes.tropicalBiomes)
                && state.is(NOT_RAINFOREST)
                && !state.is(NOT_RAINFOREST_EXCEPTIONS)) {
            return Optional.of(message(state, biome));
        }

        return Optional.empty();
    }

    private static Component message(BlockState state, Holder<Biome> biome) {
        return Component.literal(state.getBlock().getName().getString()
                + " cannot be placed in "
                + biomeName(biome)
                + " biome.");
    }

    private static String biomeName(Holder<Biome> biome) {
        return biome.unwrapKey()
                .map(ResourceKey::location)
                .map(ResourceLocation::getPath)
                .map(BiomePlacementRestrictions::humanize)
                .orElse("this");
    }

    private static String humanize(String path) {
        String[] words = path.split("_");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }

            if (result.length() > 0) {
                result.append(' ');
            }

            result.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                result.append(word.substring(1));
            }
        }

        return result.toString();
    }

    private static TagKey<Block> restrictionTag(String name) {
        return BlockTags.create(new ResourceLocation(AOEMod.MOD_ID, "biome_restrictions/" + name));
    }

    private record Rule(
            String label,
            TagKey<Block> blockTag,
            Predicate<Holder<Biome>> allows
    ) {
    }
}
