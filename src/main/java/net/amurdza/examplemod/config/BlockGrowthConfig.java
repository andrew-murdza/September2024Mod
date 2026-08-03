package net.amurdza.examplemod.config;

import com.scouter.netherdepthsupgrade.blocks.NDUBlocks;
import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.violetmoon.quark.content.world.module.GlimmeringWealdModule;

public final class BlockGrowthConfig {
    private BlockGrowthConfig() {}

    public static void init() {
        BlockConfig.clearGrowthConfig();
        registerVanillaBlocks();
        registerModdedBlocks();
        registerSaplings();
    }

    private static void addToMap(TagKey<Biome> biomeTag, float value, Block block) {
        BlockConfig.addToMap(biomeTag, value, block);
    }

    private static void addKelp(Block block) {
        addToMap(ModTags.Biomes.tropicalBiomes, 0.7F, block);
        addToMap(ModTags.Biomes.savannaBiomes, 0.28F, block);
        addToMap(ModTags.Biomes.mountainBiomes, 0.7F, block);
    }

    private static void addCrimsonKelp(Block block) {
        addToMap(ModTags.Biomes.mountainBiomes, 0.7F, block);
        addToMap(ModTags.Biomes.basaltDeltasBiomes, 0.1F, block);
        addToMap(ModTags.Biomes.crimsonForestBiomes, 0.1F, block);
    }

    private static void addWarpedKelp(Block block) {
        addToMap(ModTags.Biomes.mountainBiomes, 0.7F, block);
        addToMap(ModTags.Biomes.warpedForestBiomes, 0.1F, block);
        addToMap(ModTags.Biomes.soulSandValleyBiomes, 0.1F, block);
    }

    private static void addWeepingTwistingWart(Block block, float crimson, float warped, float soul) {
        addToMap(ModTags.Biomes.crimsonForestBiomes, crimson, block);
        addToMap(ModTags.Biomes.warpedForestBiomes, warped, block);
        addToMap(ModTags.Biomes.soulSandValleyBiomes, soul, block);
    }

    private static void addAmethyst(Block block) {
        addToMap(ModTags.Biomes.mountainBiomes, 0.6F, block);
    }

    private static void addMushroom(Block block) {
        addToMap(ModTags.Biomes.tropicalBiomes, 0.6F, block);
        addToMap(ModTags.Biomes.savannaBiomes, 0.2F, block);
        addToMap(ModTags.Biomes.mountainBiomes, 0.6F, block);
        addToMap(ModTags.Biomes.desertBiomes, 0.1F, block);
        addToMap(ModTags.Biomes.plainsBiomes, 0.2F, block);
    }

    private static void addPointedDripstone() {
        addToMap(ModTags.Biomes.mountainBiomes, 0.2F, Blocks.POINTED_DRIPSTONE);
    }

    private static void addOverworldCrops(Block block, boolean tropicalOnly) {
        addToMap(ModTags.Biomes.tropicalBiomes, 1.0F, block);

        if (!tropicalOnly) {
            addToMap(ModTags.Biomes.savannaBiomes, 0.5F, block);
            addToMap(ModTags.Biomes.plainsBiomes, 0.34F, block);
        }
    }

    private static void addTropicalOnly(Block block, float value) {
        addToMap(ModTags.Biomes.tropicalBiomes, value, block);
    }

    private static void addSoulBerries(Block block) {
        addToMap(ModTags.Biomes.soulSandValleyBiomes, 0.1F, block);
    }

    private static void registerSaplings() {
        addToMap(ModTags.Biomes.mountainBiomes, 0.14F, Blocks.SPRUCE_SAPLING);
        addToMap(ModTags.Biomes.savannaBiomes, 0.22F, Blocks.ACACIA_SAPLING);
        addToMap(ModTags.Biomes.tropicalBiomes, 0.43F, Blocks.OAK_SAPLING);
        addToMap(ModTags.Biomes.plainsBiomes, 0.14F, Blocks.OAK_SAPLING);
        addToMap(ModTags.Biomes.plainsBiomes, 0.14F, Blocks.BIRCH_SAPLING);
        addToMap(ModTags.Biomes.tropicalBiomes, 0.43F, Blocks.CHERRY_SAPLING);
        addToMap(ModTags.Biomes.tropicalBiomes, 0.43F, Blocks.MANGROVE_PROPAGULE);
        addToMap(ModTags.Biomes.tropicalBiomes, 0.14F, Blocks.DARK_OAK_SAPLING);
        addToMap(ModTags.Biomes.tropicalBiomes, 0.56F, Blocks.JUNGLE_SAPLING);
        addToMap(ModTags.Biomes.soulSandValleyBiomes, 0.1F, ModBlocks.PALE_OAK_SAPLING.get());
    }

    private static void registerVanillaBlocks() {
        addKelp(Blocks.KELP);

        addAmethyst(Blocks.BUDDING_AMETHYST);

        addMushroom(Blocks.BROWN_MUSHROOM);
        addMushroom(Blocks.RED_MUSHROOM);

        addToMap(ModTags.Biomes.crimsonForestBiomes, 0.07F, Blocks.CRIMSON_FUNGUS);
        addToMap(ModTags.Biomes.warpedForestBiomes, 0.07F, Blocks.WARPED_FUNGUS);

        addToMap(ModTags.Biomes.crimsonForestBiomes, 0.1F, Blocks.WEEPING_VINES);
        addToMap(ModTags.Biomes.warpedForestBiomes, 0.1F, Blocks.TWISTING_VINES);
        addToMap(ModTags.Biomes.soulSandValleyBiomes, 0.1F, Blocks.NETHER_WART);

        addToMap(ModTags.Biomes.deepDarkBiomes, 0.1F, Blocks.CHORUS_FLOWER);

        addPointedDripstone();

        addToMap(ModTags.Biomes.tropicalBiomes, 0.6F, Blocks.SWEET_BERRY_BUSH);
        addToMap(ModTags.Biomes.mountainBiomes, 0.2F, Blocks.SWEET_BERRY_BUSH);
        addToMap(ModTags.Biomes.tropicalBiomes, 0.6F, ModBlocks.JUNGLE_SWEET_BERRY_BUSH.get());
        addToMap(ModTags.Biomes.mountainBiomes, 0.2F, ModBlocks.JUNGLE_SWEET_BERRY_BUSH.get());
        addToMap(ModTags.Biomes.tropicalBiomes, 0.6F, ModBlocks.BLUE_BERRY_BUSH.get());
        addToMap(ModTags.Biomes.mountainBiomes, 0.2F, ModBlocks.BLUE_BERRY_BUSH.get());
        addSoulBerries(ModBlocks.SOUL_BERRY_BUSH.get());
        addToMap(ModTags.Biomes.soulSandValleyBiomes, 0.15F, ModBlocks.ASHEN_WHEAT_CROP.get());

        addToMap(ModTags.Biomes.tropicalBiomes, 0.6F, Blocks.COCOA);
        addToMap(ModTags.Biomes.tropicalBiomes, 8.0F, Blocks.CACTUS);
        addToMap(ModTags.Biomes.desertBiomes, 4.0F, Blocks.CACTUS);

        addOverworldCrops(Blocks.MELON_STEM, true);
        addOverworldCrops(Blocks.PUMPKIN_STEM, false);
        addToMap(ModTags.Biomes.tropicalBiomes, 0.5F, Blocks.BEETROOTS);
        addToMap(ModTags.Biomes.mountainBiomes, 0.06F, Blocks.BEETROOTS);
        addOverworldCrops(Blocks.WHEAT, false);
        addOverworldCrops(Blocks.CARROTS, false);
        addOverworldCrops(Blocks.POTATOES, false);

        addTropicalOnly(Blocks.CAVE_VINES, 0.55F);
        addTropicalOnly(Blocks.CAVE_VINES_PLANT, 0.55F);
        addTropicalOnly(Blocks.VINE, 0.75F);
        addTropicalOnly(Blocks.TURTLE_EGG, 0.2F);

        addToMap(ModTags.Biomes.tropicalBiomes, 1.0F, Blocks.SEA_PICKLE);
        addToMap(ModTags.Biomes.savannaBiomes, 0.4F, Blocks.SEA_PICKLE);
        addToMap(ModTags.Biomes.mountainBiomes, 0.7F, Blocks.SEA_PICKLE);

        addTropicalOnly(Blocks.BAMBOO, 1.0F);
        addTropicalOnly(Blocks.BAMBOO_SAPLING, 1.0F);
    }

    private static void registerModdedBlocks() {
        addOverworldCrops(vectorwing.farmersdelight.common.registry.ModBlocks.ONION_CROP.get(), true);
        addOverworldCrops(vectorwing.farmersdelight.common.registry.ModBlocks.TOMATO_CROP.get(), true);
        addOverworldCrops(vectorwing.farmersdelight.common.registry.ModBlocks.CABBAGE_CROP.get(), true);
        addOverworldCrops(vectorwing.farmersdelight.common.registry.ModBlocks.RICE_CROP.get(), true);

        addToMap(ModTags.Biomes.mountainBiomes, 0.8F, GlimmeringWealdModule.glow_shroom);

        addCrimsonKelp(NDUBlocks.CRIMSON_KELP_PLANT.get());
        addWarpedKelp(NDUBlocks.WARPED_KELP_PLANT.get());
        addCrimsonKelp(NDUBlocks.CRIMSON_KELP.get());
        addWarpedKelp(NDUBlocks.WARPED_KELP.get());

        addToMap(ModTags.Biomes.tropicalBiomes, 16.0F, samebutdifferent.ecologics.registry.ModBlocks.PRICKLY_PEAR.get());
        addToMap(ModTags.Biomes.desertBiomes, 4.0F, samebutdifferent.ecologics.registry.ModBlocks.PRICKLY_PEAR.get());
    }
}
