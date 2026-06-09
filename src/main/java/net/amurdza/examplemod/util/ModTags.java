package net.amurdza.examplemod.util;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

public class ModTags {
    public static class Blocks {
        public static final TagKey<Block> netherRootsPlaceable = tag("nether_roots_placeable");
        public static final TagKey<Block> netherPlantsPlaceable = tag("nether_plants_placeable");
        public static final TagKey<Block> sugarCaneCactusLike = tag("sugarcane_cactus_like");
        public static final TagKey<Block> duplicatedByBonemeal = tag("bone_meal_duplicates");
        public static final TagKey<Block> basaltStones = tag("basalt_stones");
        public static final TagKey<Block> soulSediments = tag("soul_sediments");

        // Biome placement restriction tags
        public static final TagKey<Block> crimsonOnly = tag("biome_restrictions/crimson_only");
        public static final TagKey<Block> deepDarkOnly = tag("biome_restrictions/deep_dark_only");
        public static final TagKey<Block> desertOnly = tag("biome_restrictions/desert_only");
        public static final TagKey<Block> groveOnly = tag("biome_restrictions/grove_only");
        public static final TagKey<Block> mushroomCavesOnly = tag("biome_restrictions/mushroom_caves_only");
        public static final TagKey<Block> netherNotWarped = tag("biome_restrictions/nether_not_warped");
        public static final TagKey<Block> netherOnly = tag("biome_restrictions/nether_only");
        public static final TagKey<Block> notRainforest = tag("biome_restrictions/not_rainforest");
        public static final TagKey<Block> notRainforestExceptions = tag("biome_restrictions/not_rainforest_exceptions");
        public static final TagKey<Block> overworldOnly = tag("biome_restrictions/overworld_only");
        public static final TagKey<Block> plainsOnly = tag("biome_restrictions/plains_only");
        public static final TagKey<Block> rainforestDesertOnly = tag("biome_restrictions/rainforest_desert_only");
        public static final TagKey<Block> rainforestGroveOnly = tag("biome_restrictions/rainforest_grove_only");
        public static final TagKey<Block> rainforestOnly = tag("biome_restrictions/rainforest_only");
        public static final TagKey<Block> rainforestPlainsOnly = tag("biome_restrictions/rainforest_plains_only");
        public static final TagKey<Block> rainforestSavannaGroveOnly = tag("biome_restrictions/rainforest_savanna_grove_only");
        public static final TagKey<Block> rainforestSavannaPlains = tag("biome_restrictions/rainforest_savanna_plains");
        public static final TagKey<Block> rainforestSavannaPlainsGroveOnly = tag("biome_restrictions/rainforest_savanna_plains_grove_only");
        public static final TagKey<Block> savannaOnly = tag("biome_restrictions/savanna_only");
        public static final TagKey<Block> soulSandValleyOnly = tag("biome_restrictions/soul_sand_valley_only");
        public static final TagKey<Block> warpedCrimsonOnly = tag("biome_restrictions/warped_crimson_only");
        public static final TagKey<Block> warpedOnly = tag("biome_restrictions/warped_only");

        private static TagKey<Block> tag(String name) {
            return BlockTags.create(new ResourceLocation(AOEMod.MOD_ID, name));
        }
    }
    public static class Items{
        public static final TagKey<Item> smallerFlowers=tag("small_flowers");
        public static final TagKey<Item> vegetables=tag("vegetables");
        public static final TagKey<Item> mushrooms=tag("mushrooms");
        public static final TagKey<Item> rawFish=tag("raw_fish");

        private static TagKey<Item> tag(String name){
            return ItemTags.create(new ResourceLocation(AOEMod.MOD_ID,name));
        }
    }
    public static class Biomes{
        public static final TagKey<Biome> tropicalBiomes=tag("tropical_biomes");
        public static final TagKey<Biome> mountainBiomes=tag("mountain_biomes");
        public static final TagKey<Biome> netherBiomes=tag("nether_biomes");
        public static final TagKey<Biome> desertBiomes=tag("desert_biomes");
        public static final TagKey<Biome> savannaBiomes=tag("savanna_biomes");
        public static final TagKey<Biome> plainsBiomes=tag("plains_biomes");

        public static final TagKey<Biome> mushroomCaves=tag("mushroom_caves_biomes");
        public static final TagKey<Biome> basaltDeltasBiomes  = tag("basalt_deltas_biomes");
        public static final TagKey<Biome> crimsonForestBiomes = tag("crimson_forest_biomes");
        public static final TagKey<Biome> warpedForestBiomes  = tag("warped_forest_biomes");
        public static final TagKey<Biome> soulSandValleyBiomes= tag("soul_sand_valley_biomes");

        public static final TagKey<Biome> deepDarkBiomes= tag("deep_dark_biomes");

        private static TagKey<Biome> tag(String name){
            return TagKey.create(Registries.BIOME,new ResourceLocation(AOEMod.MOD_ID,name));
        }
    }
}
