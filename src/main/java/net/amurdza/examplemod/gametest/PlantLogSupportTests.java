package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.PlantGenerationSupport;
import net.amurdza.examplemod.worldgen.DocumentedFloraFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlantLogSupportTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void flowersAndWildCropsCannotReplaceOrGrowOnLogs(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos root = helper.absolutePos(new BlockPos(3, 3, 3));
        for (String species : new String[]{"minecraft:poppy", "minecraft:lilac", "minecraft:pink_petals",
                "farmersdelight:wild_carrots", "farmersdelight:wild_cabbages", "pineapple_delight:pineapple_wild_crop"}) {
            var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(species));
            helper.assertTrue(block != null && block != Blocks.AIR, "Required plant exists: " + species);
            for (var log : new net.minecraft.world.level.block.Block[]{Blocks.OAK_LOG, Blocks.STRIPPED_ACACIA_LOG, Blocks.MANGROVE_WOOD}) {
                level.setBlock(root.below(), log.defaultBlockState(), 2);
                level.setBlock(root, Blocks.AIR.defaultBlockState(), 2);
                helper.assertFalse(PlantGenerationSupport.isAllowed(level, root, block.defaultBlockState()),
                        "No flower or wild crop may use log support: " + species);
                helper.assertFalse(DocumentedFloraFeature.placePlant(level, root, species, RandomSource.create(14)),
                        "The actual flora path must reject log support");
                level.setBlock(root.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                level.setBlock(root, log.defaultBlockState(), 2);
                helper.assertFalse(DocumentedFloraFeature.placePlant(level, root, species, RandomSource.create(14)),
                        "The actual flora path must not replace a log");
                helper.assertTrue(level.getBlockState(root).is(log), "Rejected plant must preserve its log");
            }
        }
        level.setBlock(root, Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(root.below(), Blocks.OAK_LOG.defaultBlockState(), 2);
        helper.assertTrue(PlantGenerationSupport.isAllowed(level, root, Blocks.COCOA.defaultBlockState()),
                "Cocoa and other intentional tree decorations keep their own support rules");
        for (var leaves : new net.minecraft.world.level.block.Block[]{Blocks.CHERRY_LEAVES, Blocks.FLOWERING_AZALEA_LEAVES}) {
            helper.assertTrue(leaves.defaultBlockState().is(net.minecraft.tags.BlockTags.FLOWERS),
                    "Fixture must exercise the vanilla pollination tag that also contains tree crowns");
            helper.assertTrue(PlantGenerationSupport.isAllowed(level, root, leaves.defaultBlockState()),
                    "Flowering crowns must still generate above their own trunks");
        }
        helper.succeed();
    }
}
