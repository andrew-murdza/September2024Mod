package net.amurdza.examplemod.config;

import com.scouter.netherdepthsupgrade.blocks.NDUBlocks;
import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

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
        DocumentedContentBalance.registerGrowth();
    }

    private static void addToMap(ResourceKey<Level> dimension, float value, Block block) {
        BlockConfig.addToMap(dimension, value, block);
    }

    private static void addKelp(Block block) {
        addToMap(GameplayDimensions.TROPICAL, 0.7F, block);
        addToMap(GameplayDimensions.SAVANNA, 0.28F, block);
        addToMap(GameplayDimensions.SNOWY, 0.7F, block);
    }

    private static void addLavaKelp(Block block) {
        addToMap(GameplayDimensions.SNOWY, 0.7F, block);
        addToMap(GameplayDimensions.NETHER, 0.1F, block);
    }

    private static void addAmethyst(Block block) {
        addToMap(GameplayDimensions.SNOWY, 0.6F, block);
    }

    private static void addMushroom(Block block) {
        addToMap(GameplayDimensions.TROPICAL, 0.6F, block);
        addToMap(GameplayDimensions.SAVANNA, 0.2F, block);
        addToMap(GameplayDimensions.SNOWY, 0.6F, block);
        addToMap(GameplayDimensions.DESERT, 0.1F, block);
        addToMap(GameplayDimensions.PLAINS, 0.2F, block);
    }

    private static void addPointedDripstone() {
        addToMap(GameplayDimensions.SNOWY, 0.2F, Blocks.POINTED_DRIPSTONE);
    }

    private static void addOverworldCrops(Block block, boolean tropicalOnly) {
        addToMap(GameplayDimensions.TROPICAL, 1.0F, block);

        if (!tropicalOnly) {
            addToMap(GameplayDimensions.SAVANNA, 0.5F, block);
            addToMap(GameplayDimensions.PLAINS, 0.34F, block);
        }
    }

    private static void addTropicalOnly(Block block, float value) {
        addToMap(GameplayDimensions.TROPICAL, value, block);
    }

    private static void addSoulBerries(Block block) {
        addToMap(GameplayDimensions.NETHER, 0.1F, block);
    }

    private static void registerSaplings() {
        addToMap(GameplayDimensions.SNOWY, 0.14F, Blocks.SPRUCE_SAPLING);
        addToMap(GameplayDimensions.SAVANNA, 0.22F, Blocks.ACACIA_SAPLING);
        addToMap(GameplayDimensions.TROPICAL, 0.43F, Blocks.OAK_SAPLING);
        addToMap(GameplayDimensions.PLAINS, 0.14F, Blocks.OAK_SAPLING);
        addToMap(GameplayDimensions.PLAINS, 0.14F, Blocks.BIRCH_SAPLING);
        addToMap(GameplayDimensions.TROPICAL, 0.43F, Blocks.CHERRY_SAPLING);
        addToMap(GameplayDimensions.TROPICAL, 0.43F, Blocks.MANGROVE_PROPAGULE);
        addToMap(GameplayDimensions.TROPICAL, 0.14F, Blocks.DARK_OAK_SAPLING);
        addToMap(GameplayDimensions.TROPICAL, 0.14F, ModBlocks.AZALEA_SAPLING.get());
        addToMap(GameplayDimensions.TROPICAL, 0.56F, Blocks.JUNGLE_SAPLING);
        addToMap(GameplayDimensions.NETHER, 0.1F, ModBlocks.PALE_OAK_SAPLING.get());
    }

    private static void registerVanillaBlocks() {
        addKelp(Blocks.KELP);

        addAmethyst(Blocks.BUDDING_AMETHYST);

        addMushroom(Blocks.BROWN_MUSHROOM);
        addMushroom(Blocks.RED_MUSHROOM);

        addToMap(GameplayDimensions.NETHER, 0.07F, Blocks.CRIMSON_FUNGUS);
        addToMap(GameplayDimensions.NETHER, 0.07F, Blocks.WARPED_FUNGUS);

        addToMap(GameplayDimensions.NETHER, 0.1F, Blocks.WEEPING_VINES);
        addToMap(GameplayDimensions.NETHER, 0.1F, Blocks.TWISTING_VINES);
        addToMap(GameplayDimensions.NETHER, 0.1F, Blocks.NETHER_WART);

        addToMap(GameplayDimensions.DESERT, 0.1F, Blocks.CHORUS_FLOWER);

        addPointedDripstone();

        addToMap(GameplayDimensions.TROPICAL, 0.6F, Blocks.SWEET_BERRY_BUSH);
        addToMap(GameplayDimensions.SNOWY, 0.2F, Blocks.SWEET_BERRY_BUSH);
        addToMap(GameplayDimensions.TROPICAL, 0.6F, ModBlocks.JUNGLE_SWEET_BERRY_BUSH.get());
        addToMap(GameplayDimensions.SNOWY, 0.2F, ModBlocks.JUNGLE_SWEET_BERRY_BUSH.get());
        addToMap(GameplayDimensions.TROPICAL, 0.6F, ModBlocks.BLUE_BERRY_BUSH.get());
        addToMap(GameplayDimensions.SNOWY, 0.2F, ModBlocks.BLUE_BERRY_BUSH.get());
        addSoulBerries(ModBlocks.SOUL_BERRY_BUSH.get());
        addToMap(GameplayDimensions.TROPICAL, 0.6F, Blocks.COCOA);
        addToMap(GameplayDimensions.TROPICAL, 8.0F, Blocks.CACTUS);
        addToMap(GameplayDimensions.DESERT, 4.0F, Blocks.CACTUS);

        addOverworldCrops(Blocks.MELON_STEM, true);
        addOverworldCrops(Blocks.PUMPKIN_STEM, false);
        addToMap(GameplayDimensions.TROPICAL, 0.5F, Blocks.BEETROOTS);
        addToMap(GameplayDimensions.SNOWY, 0.06F, Blocks.BEETROOTS);
        addToMap(GameplayDimensions.SAVANNA, 0.25F, Blocks.BEETROOTS);
        addToMap(GameplayDimensions.PLAINS, 0.17F, Blocks.BEETROOTS);
        addOverworldCrops(Blocks.WHEAT, false);
        addOverworldCrops(Blocks.CARROTS, false);
        addOverworldCrops(Blocks.POTATOES, false);

        addTropicalOnly(Blocks.CAVE_VINES, 0.55F);
        addTropicalOnly(Blocks.CAVE_VINES_PLANT, 0.55F);
        addTropicalOnly(Blocks.VINE, 0.75F);
        addTropicalOnly(Blocks.TURTLE_EGG, 0.2F);

        addToMap(GameplayDimensions.TROPICAL, 1.0F, Blocks.SEA_PICKLE);
        addToMap(GameplayDimensions.SAVANNA, 0.4F, Blocks.SEA_PICKLE);
        addToMap(GameplayDimensions.SNOWY, 0.7F, Blocks.SEA_PICKLE);

        addTropicalOnly(Blocks.BAMBOO, 1.0F);
        addTropicalOnly(Blocks.BAMBOO_SAPLING, 1.0F);
    }

    private static void registerModdedBlocks() {
        addOverworldCrops(vectorwing.farmersdelight.common.registry.ModBlocks.ONION_CROP.get(), true);
        addOverworldCrops(vectorwing.farmersdelight.common.registry.ModBlocks.TOMATO_CROP.get(), true);
        addOverworldCrops(vectorwing.farmersdelight.common.registry.ModBlocks.CABBAGE_CROP.get(), true);
        addOverworldCrops(vectorwing.farmersdelight.common.registry.ModBlocks.RICE_CROP.get(), true);

        addToMap(GameplayDimensions.SNOWY, 0.8F, GlimmeringWealdModule.glow_shroom);

        addLavaKelp(NDUBlocks.CRIMSON_KELP_PLANT.get());
        addLavaKelp(NDUBlocks.WARPED_KELP_PLANT.get());
        addLavaKelp(NDUBlocks.CRIMSON_KELP.get());
        addLavaKelp(NDUBlocks.WARPED_KELP.get());

        addToMap(GameplayDimensions.TROPICAL, 16.0F, samebutdifferent.ecologics.registry.ModBlocks.PRICKLY_PEAR.get());
        addToMap(GameplayDimensions.DESERT, 4.0F, samebutdifferent.ecologics.registry.ModBlocks.PRICKLY_PEAR.get());
    }
}
