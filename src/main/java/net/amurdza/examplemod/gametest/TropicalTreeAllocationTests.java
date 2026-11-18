package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.amurdza.examplemod.worldgen.TropicalLayout;
import net.amurdza.examplemod.worldgen.TropicalVegetation;
import net.amurdza.examplemod.worldgen.feature.features.TropicalCherryTreesFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TropicalTreeAllocationTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void fruitingMangrovesSupplyMulberriesAndPie(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var berry = ForgeRegistries.ITEMS.getValue(new ResourceLocation("upgrade_aquatic:mulberry"));
        var pie = ForgeRegistries.ITEMS.getValue(new ResourceLocation("upgrade_aquatic:mulberry_pie"));
        var vine = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("upgrade_aquatic:mulberry_vine"));
        helper.assertTrue(net.amurdza.examplemod.util.RetainedContent.isKeptItem(berry.getDefaultInstance())
                        && level.getRecipeManager().getRecipes().stream().anyMatch(recipe ->
                        recipe.getResultItem(level.registryAccess()).is(pie)), "Mulberries and their pie must be obtainable");
        var layout = DimensionTerrain.tropicalLayout(level.getChunkSource().randomState().router().temperature());
        var sites = net.amurdza.examplemod.worldgen.TropicalMulberryTrees.sites(layout, layout.island(81, -19));
        int fruit = 0;
        for (var site : sites) {
            level.getChunk(site.chunkX(), site.chunkZ());
            for (var pos : BlockPos.betweenClosed(site.x() - 7, 63, site.z() - 7, site.x() + 7, 105, site.z() + 7)) {
                if (level.getBlockState(pos).is(vine) && level.getBlockState(pos.above()).is(Blocks.MANGROVE_LEAVES)) fruit++;
            }
            if (fruit >= 6) break;
        }
        helper.assertTrue(fruit >= 6, "A generated fruiting mangrove must bear supported mulberry vines");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void unselectedMangrovesGrowOnDryLandAndInWater(GameTestHelper helper) {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var feature = tropical.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE)
                .get(new ResourceLocation("aoemod:rainforest/tree/natural_mangrove"));
        var mulberry = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("upgrade_aquatic:mulberry_vine"));
        for (int waterDepth : new int[]{0, 6}) {
            int successes = 0;
            for (int seed = 0; seed < 16; seed++) {
                Map<BlockPos, BlockState> blocks = new HashMap<>();
                var world = TropicalTreeShapeTests.fixture(helper, blocks, "mangrove_swamp", waterDepth, true);
                if (feature.place(world, tropical.getChunkSource().getGenerator(), RandomSource.create(seed),
                        new BlockPos(8, 101, 8))) successes++;
                helper.assertFalse(blocks.values().stream().anyMatch(state -> state.is(mulberry)),
                        "Unselected mangroves must leave fruit to the reserved trees, water depth=" + waterDepth);
            }
            helper.assertTrue(successes >= 4, "Mangroves must still grow both on land and in water: " + waterDepth);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void wholeGrovesHaveExactlyTwoNestsAndSetBackTheirTrees(GameTestHelper helper) {
        var feature = new TropicalCherryTreesFeature(NoneFeatureConfiguration.CODEC);
        for (int seed = 0; seed < 16; seed++) {
            var layout = new TropicalLayout(TropicalLayout.mix(seed * 1000003L));
            var island = layout.island(seed % 2, 0);
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            var world = TropicalTreeShapeTests.fixture(helper, blocks, "cherry_grove", 0, true);
            int radius = (int) Math.ceil(TropicalLayout.CHERRY_RADIUS / 16) + 1;
            int cx = Math.floorDiv((int) Math.floor(island.x()), 16);
            int cz = Math.floorDiv((int) Math.floor(island.z()), 16);
            for (int x = cx - radius; x <= cx + radius; x++) for (int z = cz - radius; z <= cz + radius; z++) {
                feature.placeInLayout(new FeaturePlaceContext<>(Optional.empty(), world,
                        world.getLevel().getChunkSource().getGenerator(), RandomSource.create(TropicalLayout.mix(x * 38131L + z)),
                        new BlockPos(x * 16, 0, z * 16), NoneFeatureConfiguration.INSTANCE), layout);
            }
            long nests = blocks.values().stream().filter(state -> state.is(Blocks.BEE_NEST)).count();
            helper.assertTrue(nests == 2, "Each generated grove must retain exactly two nests: seed=" + seed + ", nests=" + nests);
            blocks.forEach((pos, state) -> {
                if (state.is(Blocks.CHERRY_LOG) && pos.getY() == 101) {
                    var sample = layout.sample(pos.getX(), pos.getZ());
                    helper.assertTrue(sample.distance() <= sample.cherryEdge() - TropicalLayout.CHERRY_TREE_SETBACK,
                            "Cherry trunk bases must stay clear of the rainforest boundary");
                }
            });
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void fullyDecoratedNaturalGroveKeepsExactlyTwoPopulatedNests(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var layout = DimensionTerrain.tropicalLayout(level.getChunkSource().randomState().router().temperature());
        int candidateColumn = 19731;
        var island = layout.island(candidateColumn, 117);
        int radius = (int) Math.ceil(TropicalLayout.CHERRY_RADIUS + 16);
        int minX = Math.floorDiv((int) Math.floor(island.x()) - radius, 16);
        int maxX = Math.floorDiv((int) Math.floor(island.x()) + radius, 16);
        int minZ = Math.floorDiv((int) Math.floor(island.z()) - radius, 16);
        int maxZ = Math.floorDiv((int) Math.floor(island.z()) + radius, 16);
        Set<BlockPos> nests = new HashSet<>();
        AOEMod.LOGGER.info("Generating full cherry grove for two-nest audit at {},{}", island.x(), island.z());
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            var chunk = level.getChunk(x, z);
            for (var entry : chunk.getBlockEntities().entrySet()) {
                if (!entry.getValue().getBlockState().is(Blocks.BEE_NEST)) continue;
                var sample = layout.sample(entry.getKey().getX(), entry.getKey().getZ());
                if (sample.island().hash() != island.hash() || sample.distance() > TropicalLayout.CHERRY_RADIUS) continue;
                nests.add(entry.getKey());
                helper.assertTrue(entry.getValue().saveWithFullMetadata().getList("Bees", 10).size() >= 2,
                        "Each selected natural tree must retain a populated native nest: " + entry.getKey());
            }
        }
        helper.assertTrue(nests.size() == 2, "Full terrain and decoration must retain exactly two populated nests: " + nests);
        helper.succeed();
    }
}
