package net.amurdza.examplemod.gametest;

import com.mojang.serialization.JsonOps;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.SnowySpruceHeight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TreeBalanceRevisionTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void dappledUsesSpacedPoplarsWithoutChangingNativeSizeOrTotalTreeBudget(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        var tree = registry.get(new ResourceLocation("aoemod:dappled/trees"));
        var encoded = ConfiguredFeature.DIRECT_CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE,
                helper.getLevel().registryAccess()), tree).result().orElseThrow().getAsJsonObject();
        Map<String, Integer> weights = new HashMap<>();
        for (var element : encoded.getAsJsonObject("config").getAsJsonArray("features")) {
            var entry = element.getAsJsonObject();
            weights.put(entry.get("data").getAsString(), entry.get("weight").getAsInt());
        }
        helper.assertTrue(weights.equals(Map.of("minecraft:red_poplar", 200,
                        "minecraft:orange_poplar", 240, "minecraft:yellow_poplar", 90)),
                "All trees must use the existing native poplar distributions, with no spruce or size filtering");
        var placed = helper.getLevel().registryAccess().registryOrThrow(Registries.PLACED_FEATURE)
                .get(new ResourceLocation("aoemod:dappled/trees"));
        helper.assertTrue(placed.feature().value().feature() instanceof
                        net.amurdza.examplemod.worldgen.feature.features.DappledTreesFeature,
                "Worldgen must use the spatial tree pass");
        int selected = 0;
        for (int x = -128; x < 128; x++) for (int z = -128; z < 128; z++) {
            BlockPos at = net.amurdza.examplemod.worldgen.feature.features.DappledTreesFeature.candidate(50881, x, z);
            if (at == null) continue;
            selected++;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos adjacent = net.amurdza.examplemod.worldgen.feature.features.DappledTreesFeature.candidate(50881, x + dx, z + dz);
                helper.assertTrue(adjacent == null || at.distSqr(adjacent) >= 100,
                        "Jittered trees must remain at least ten blocks apart, including across chunk seams");
            }
        }
        double perChunk = selected * 256.0 / (65536 * 144);
        helper.assertTrue(Math.abs(perChunk - 3.0 * 611 / 1114) < .015,
                "Spruce opportunities become poplars while the total expected density remains unchanged: " + perChunk);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void giantSpruceRaisesMinimumFourAndMeanTwoWithoutRaisingMaximum(GameTestHelper helper) {
        for (String feature : new String[]{"minecraft:mega_spruce", "minecraft:mega_pine"}) {
            var config = (TreeConfiguration) helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                    .get(new ResourceLocation(feature)).config();
            var snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
            var level = dimensionFixture(helper, new HashMap<>(), "snowy_forest", snowy);
            RandomSource random = RandomSource.create(124019);
            int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
            long sum = 0;
            for (int sample = 0; sample < 65536; sample++) {
                int height = SnowySpruceHeight.sampleHeight(level, new BlockPos(8, 101, 8), config, random,
                        () -> config.trunkPlacer.getTreeHeight(random));
                min = Math.min(min, height);
                max = Math.max(max, height);
                sum += height;
            }
            helper.assertTrue(min == 21 && max == 33 && Math.abs(sum / 65536.0 - 27) < .06,
                    "Giant spruce must change 17..33/mean25 to21..33/mean27: " + feature
                            + ", observed=" + min + ".." + max + "/" + sum / 65536.0);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void plainsNativeSaplingAndBeeTreesRiseOneAndLeaveTwoClearLayers(GameTestHelper helper) {
        var plains = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        for (String feature : new String[]{"minecraft:oak", "minecraft:birch", "aoemod:trees/oak", "aoemod:trees/birch",
                "aoemod:trees/oak_bees", "aoemod:trees/birch_bees", "aoemod:trees/birch_bees_05"}) {
            for (long seed = 0; seed < 16; seed++) {
                Map<BlockPos, BlockState> original = new HashMap<>(), raised = new HashMap<>();
                var ordinaryWorld = dimensionFixture(helper, original, "plains", helper.getLevel());
                var plainsWorld = dimensionFixture(helper, raised, "plains", plains);
                helper.assertTrue(TropicalTreeShapeTests.place(ordinaryWorld, feature, seed)
                                && TropicalTreeShapeTests.place(plainsWorld, feature, seed),
                        "Both native and raised Plains tree shapes must generate: " + feature);
                int oldTop = original.entrySet().stream().filter(entry -> entry.getValue().is(BlockTags.LOGS))
                        .mapToInt(entry -> entry.getKey().getY()).max().orElseThrow();
                int newTop = raised.entrySet().stream().filter(entry -> entry.getValue().is(BlockTags.LOGS))
                        .mapToInt(entry -> entry.getKey().getY()).max().orElseThrow();
                int lowestLeaf = raised.entrySet().stream().filter(entry -> entry.getValue().is(BlockTags.LEAVES))
                        .mapToInt(entry -> entry.getKey().getY()).min().orElseThrow();
                int extension = feature.equals("minecraft:oak") ? 2 : 1;
                helper.assertTrue(newTop == oldTop + extension && newTop >= 106 && newTop <= 108 && lowestLeaf >= 103,
                        "Plains oak and birch must share the six-to-eight block trunk distribution and two clear lower layers: "
                                + feature + ", seed=" + seed + ", tops=" + oldTop + "/" + newTop + ", bottomLeaf=" + lowestLeaf);
            }
        }
        helper.succeed();
    }

    static WorldGenLevel dimensionFixture(GameTestHelper helper, Map<BlockPos, BlockState> blocks,
                                                   String biome, ServerLevel dimension) {
        WorldGenLevel memory = TropicalTreeShapeTests.fixture(helper, blocks, biome, 0, false);
        return (WorldGenLevel) Proxy.newProxyInstance(TreeBalanceRevisionTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getLevel")) return dimension;
                    if (method.getName().equals("getBlockEntity")) return args.length == 1 ? null : Optional.empty();
                    try {
                        return method.invoke(memory, args);
                    } catch (InvocationTargetException exception) {
                        throw exception.getCause();
                    }
                });
    }
}
