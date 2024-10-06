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
    public static class Blocks{
        public static final TagKey<Block> netherFlowers=tag("nether_flowers");
        public static final TagKey<Block> netherRootsPlaceable=tag("nether_roots_placeable");
        public static final TagKey<Block> sugarCaneCactusLike=tag("sugarcane_cactus_like");
        public static final TagKey<Block> crimsonRootsGroundBlocks=tag("crimson_roots_ground_blocks");
        public static final TagKey<Block> duplicatedByBonemeal=tag("bone_meal_duplicates");

        private static TagKey<Block> tag(String name){
            return BlockTags.create(new ResourceLocation(AOEMod.MOD_ID,name));
        }
    }
    public static class Items{
        public static final TagKey<Item> smallerFlowers=tag("small_flowers");
        public static final TagKey<Item> vegetables=tag("vegetables");

        private static TagKey<Item> tag(String name){
            return ItemTags.create(new ResourceLocation(AOEMod.MOD_ID,name));
        }
    }
    public static class Biomes{
        public static final TagKey<Biome> tropicalBiomes=tag("tropical_biomes");

        private static TagKey<Biome> tag(String name){
            return TagKey.create(Registries.BIOME,new ResourceLocation(AOEMod.MOD_ID,name));
        }
    }
}
