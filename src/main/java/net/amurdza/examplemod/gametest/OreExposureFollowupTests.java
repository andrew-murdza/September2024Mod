package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.OrePlacementContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OreExposureFollowupTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void actualOreAndScatteredVeinsShareExposureDecisionsAndKeepBuriedBlocks(GameTestHelper helper) {
        var snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        int index = 0;
        for (var feature : List.of(Feature.ORE, Feature.SCATTERED_ORE)) {
            verifyDistribution(helper, feature, helper.getLevel(), "plains", 30, .5F, .5, index++);
            verifyDistribution(helper, feature, snowy, "snowy_forest", 30, .2F, 1 - .25 / 3, index++);
            verifyDistribution(helper, feature, snowy, "dappled_forest", 30, .2F, 1 - .8 / 3, index++);
            verifyDistribution(helper, feature, snowy, "dappled_forest", -54, .2F, 1 - .8 / 9, index++);
        }
        helper.succeed();
    }

    private static void verifyDistribution(GameTestHelper helper, Feature<OreConfiguration> feature, ServerLevel level,
            String biome, int y, float chance, double expected, int index) {
        var origin = new BlockPos(8808 + index * 32, y, 8808);
        var chunk = prepare(level, origin, true);
        var control = helper.getLevel();
        if (level != control) prepare(control, origin, true);
        var targetBiome = biome(level, biome);
        // Scattered ore probes the neighbors of targets up to seven blocks
        // away. At the chunk center, that last air check can cross an edge.
        var chunks = new java.util.ArrayList<net.minecraft.world.level.chunk.ChunkAccess>();
        var center = chunk.getPos();
        for (int z = center.z - 1; z <= center.z + 1; z++) {
            for (int x = center.x - 1; x <= center.x + 1; x++) chunks.add(level.getChunk(x, z));
        }
        var region = new WorldGenRegion(level, chunks, ChunkStatus.FEATURES, 1) {
            @Override public Holder<Biome> getBiome(BlockPos pos) { return targetBiome; }
        };
        var clear = configured(feature, Blocks.COAL_ORE, 0);
        var selected = configured(feature, Blocks.COAL_ORE, chance);
        RandomSource seeds = RandomSource.create(0x1234ABCDL + index);
        int eligible = 0, discarded = 0, mixedVeins = 0;
        for (int sample = 0; sample < 256; sample++) {
            long seed = seeds.nextLong();
            // The same one-sample RNG prelude keeps geometry identical between
            // the exposed control in the Overworld and its matching filtered snowy vein.
            clear.place(control, control.getChunkSource().getGenerator(), RandomSource.create(seed), origin);
            Set<BlockPos> baseline = collectAndReset(control, origin, Blocks.COAL_ORE);
            Set<BlockPos> exposed = new HashSet<>();
            for (BlockPos pos : baseline) if (isExposed(level, pos)) exposed.add(pos);
            Set<BlockPos> buried = new HashSet<>(baseline);
            buried.removeAll(exposed);
            selected.place(region, level.getChunkSource().getGenerator(), RandomSource.create(seed), origin);
            Set<BlockPos> actual = collectAndReset(level, origin, Blocks.COAL_ORE);
            helper.assertTrue(actual.containsAll(buried), "An exposure roll must keep buried members: "
                    + feature + " " + biome + " sample=" + sample + " expected=" + buried + " actual=" + actual);
            helper.assertTrue(actual.equals(baseline) || actual.equals(buried),
                    "Every exposed block in one actual feature invocation must share one retain/discard decision: " + biome);
            if (exposed.size() >= 2) {
                eligible++;
                if (!actual.containsAll(exposed)) discarded++;
                if (!buried.isEmpty()) mixedVeins++;
            }
            helper.assertTrue(OrePlacementContext.level() == null && OrePlacementContext.skipAirCheck() == null,
                    "The feature must release both its world and random-decision scope");
        }
        helper.assertTrue(eligible >= 50 && mixedVeins >= 30, "The runtime sample must contain many multi-block exposed and partly buried veins");
        double observed = (double) discarded / eligible;
        helper.assertTrue(Math.abs(observed - expected) < .13,
                "Per-vein exposure frequency differs from its configured/biome probability: " + biome + " expected=" + expected + " actual=" + observed);
        AOEMod.LOGGER.info("Ore exposure {} {}: {} of {} exposed veins discarded; {} mixed buried/exposed veins",
                feature, biome, discarded, eligible, mixedVeins);
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void buriedVeinsAndNonOreBlobsKeepTheirNativeContents(GameTestHelper helper) {
        var snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var origin = new BlockPos(9064, 30, 8808);
        var chunk = prepare(snowy, origin, false);
        for (var feature : List.of(Feature.ORE, Feature.SCATTERED_ORE)) {
            for (int sample = 0; sample < 24; sample++) {
                long seed = 7821L + sample * 341873128712L;
                setBiome(snowy, chunk, "dappled_forest");
                configured(feature, Blocks.COAL_ORE, 0).place(snowy, snowy.getChunkSource().getGenerator(), RandomSource.create(seed), origin);
                var baseline = collectAndReset(snowy, origin, Blocks.COAL_ORE);
                setBiome(snowy, chunk, "snowy_forest");
                configured(feature, Blocks.COAL_ORE, 1).place(snowy, snowy.getChunkSource().getGenerator(), RandomSource.create(seed), origin);
                helper.assertTrue(collectAndReset(snowy, origin, Blocks.COAL_ORE).equals(baseline),
                        "A fully buried Snowy-forest vein must keep every ore block");
            }
        }
        prepare(snowy, origin, true);
        for (int sample = 0; sample < 32; sample++) {
            long seed = 9832L + sample * 341873128712L;
            setBiome(snowy, chunk, "dappled_forest");
            configured(Feature.ORE, Blocks.GRANITE, .5F).place(snowy, snowy.getChunkSource().getGenerator(), RandomSource.create(seed), origin);
            var baseline = collectAndReset(snowy, origin, Blocks.GRANITE);
            setBiome(snowy, chunk, "snowy_forest");
            configured(Feature.ORE, Blocks.GRANITE, .5F).place(snowy, snowy.getChunkSource().getGenerator(), RandomSource.create(seed), origin);
            helper.assertTrue(collectAndReset(snowy, origin, Blocks.GRANITE).equals(baseline),
                    "Non-ore blobs must retain their original random stream and configured blockwise exposure behavior");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void regionOriginBiomeAndNestedPlacementScopesSelectExactlyOneDecision(GameTestHelper helper) {
        var snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var chunk = snowy.getChunk(570, 570);
        Holder<Biome> forest = biome(snowy, "snowy_forest"), caves = biome(snowy, "mushroom_caves");
        var region = new WorldGenRegion(snowy, List.of(chunk), ChunkStatus.FEATURES, -1) {
            @Override public Holder<Biome> getBiome(BlockPos pos) { return pos.getY() < 0 ? caves : forest; }
        };
        var ore = configured(Feature.ORE, Blocks.COAL_ORE, 0).config();
        var blob = configured(Feature.ORE, Blocks.GRANITE, 0).config();
        var high = new FeaturePlaceContext<>(Optional.empty(), region, snowy.getChunkSource().getGenerator(),
                RandomSource.create(51), new BlockPos(chunk.getPos().getMinBlockX() + 8, 30, chunk.getPos().getMinBlockZ() + 8), ore);
        var low = new FeaturePlaceContext<>(Optional.empty(), region, snowy.getChunkSource().getGenerator(),
                RandomSource.create(51), high.origin().atY(-60), ore);
        boolean expectedHigh = RandomSource.create(51).nextFloat() >= 1 - .25F / 3;
        OrePlacementContext.runVein(high, () -> {
            helper.assertTrue(OrePlacementContext.skipAirCheck() == expectedHigh,
                    "Snowy forest stone must retain one third of its earlier exposed veins");
            OrePlacementContext.runVein(low, () -> {
                helper.assertTrue(OrePlacementContext.skipAirCheck() == (RandomSource.create(51).nextFloat() >= 1 - 1F / 9),
                        "Deepslate must retain one ninth of its earlier exposed veins");
                return true;
            });
            var nestedBlob = new FeaturePlaceContext<>(Optional.empty(), region, snowy.getChunkSource().getGenerator(),
                    RandomSource.create(51), high.origin(), blob);
            OrePlacementContext.runVein(nestedBlob, () -> {
                helper.assertTrue(OrePlacementContext.skipAirCheck() == null, "A nested non-ore blob cannot inherit an ore's decision");
                return true;
            });
            helper.assertTrue(OrePlacementContext.skipAirCheck() == expectedHigh, "Nested placement must restore the parent vein's decision");
            return true;
        });
        helper.assertTrue(OrePlacementContext.skipAirCheck() == null && OrePlacementContext.level() == null,
                "Completed nested placement must clear thread-local state");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void deepDarkExposureIsOneSixthAndOneDecisionPerVein(GameTestHelper helper) {
        var desert = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        for (float original : new float[]{0, .5F, 1}) {
            int retained = 0;
            RandomSource seeds = RandomSource.create(818232);
            for (int sample = 0; sample < 6000; sample++) {
                var config = configured(Feature.ORE, Blocks.COAL_ORE, original).config();
                var context = new FeaturePlaceContext<>(Optional.empty(), desert, desert.getChunkSource().getGenerator(),
                        RandomSource.create(seeds.nextLong()), new BlockPos(0, -16, 0), config);
                boolean accepted = OrePlacementContext.runVein(context, () -> {
                    boolean first = Boolean.TRUE.equals(OrePlacementContext.skipAirCheck());
                    for (int block = 0; block < 32; block++) helper.assertTrue(
                            Boolean.TRUE.equals(OrePlacementContext.skipAirCheck()) == first,
                            "All ore members must share their invocation's exposure decision");
                    return first;
                });
                if (accepted) retained++;
            }
            double expected = (1 - original) / 6;
            helper.assertTrue(Math.abs(retained / 6000.0 - expected) < .02,
                    "Deep-dark exposed-vein proportion must be one sixth of its configured value: " + original + " retained=" + retained);
        }
        helper.assertTrue(OrePlacementContext.skipAirCheck() == null && OrePlacementContext.level() == null,
                "Deep-dark vein decisions cannot leak into subsequent feature calls");
        helper.succeed();
    }

    private static ConfiguredFeature<OreConfiguration, ?> configured(Feature<OreConfiguration> feature, Block output, float chance) {
        return new ConfiguredFeature<>(feature, new OreConfiguration(new BlockMatchTest(Blocks.STONE), output.defaultBlockState(), 32, chance));
    }

    private static LevelChunk prepare(ServerLevel level, BlockPos origin, boolean exposed) {
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-9, -9, -9), origin.offset(9, 9, 9)))
            level.getChunkAt(pos).setBlockState(pos, exposed && pos.getZ() >= origin.getZ()
                    ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState(), false);
        LevelChunk chunk = level.getChunkAt(origin);
        net.minecraft.world.level.levelgen.Heightmap.primeHeightmaps(chunk,
                java.util.EnumSet.of(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG));
        return chunk;
    }

    private static Set<BlockPos> collectAndReset(ServerLevel level, BlockPos origin, Block output) {
        Set<BlockPos> found = new HashSet<>();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-8, -8, -8), origin.offset(8, 8, 8))) {
            var chunk = level.getChunkAt(pos);
            if (chunk.getBlockState(pos).is(output)) {
                found.add(pos.immutable());
                chunk.setBlockState(pos, Blocks.STONE.defaultBlockState(), false);
            }
        }
        return found;
    }

    private static boolean isExposed(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) if (level.getBlockState(pos.relative(direction)).isAir()) return true;
        return false;
    }

    private static Holder<Biome> biome(ServerLevel level, String name) {
        return level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(ResourceKey.create(Registries.BIOME, AOEMod.makeID(name)));
    }

    private static void setBiome(ServerLevel level, LevelChunk chunk, String name) {
        var holder = biome(level, name);
        chunk.fillBiomesFromNoise((x, y, z, sampler) -> holder, level.getChunkSource().randomState().sampler());
    }
}
