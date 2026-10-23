package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.util.BiomePlacementRestrictions;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.GrowthPlacementContext;
import net.amurdza.examplemod.util.TreeGrowthPlacementRules;
import net.amurdza.examplemod.worldgen.PaleOakTreeGrower;
import net.amurdza.examplemod.worldgen.GrowthPlacementBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ConfiguredTreeGrowthTests {
    private static final String TEMPLATE = "plant_gameplay_empty";

    private ConfiguredTreeGrowthTests() { }

    @GameTest(template = TEMPLATE)
    public static void singleAzaleaSaplingGrowsOnTropicalMoss(GameTestHelper helper) {
        ServerLevel tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        BlockPos origin = new BlockPos(40, 220, 40);
        clear(tropical, origin);
        tropical.setBlock(origin.below(), Blocks.MOSS_BLOCK.defaultBlockState(), 2);
        TreeConfiguration configuration = (TreeConfiguration) tropical.registryAccess()
                .registryOrThrow(Registries.CONFIGURED_FEATURE).getHolderOrThrow(TreeFeatures.AZALEA_TREE).value().config();
        // Quark's enabled azalea-wood module replaces this vanilla feature's trunk provider on reload.
        BlockState expectedTrunk = configuration.trunkProvider.getState(RandomSource.create(7), origin);
        helper.assertTrue(expectedTrunk.is(BlockTags.LOGS), "Loaded azalea feature must provide a real log trunk");
        try {
            BlockState sapling = ModBlocks.AZALEA_SAPLING.get().defaultBlockState().setValue(SaplingBlock.STAGE, 1);
            helper.assertTrue(tropical.setBlock(origin, sapling, 2), "Fixture must place one tropical azalea sapling");
            ModBlocks.AZALEA_SAPLING.get().advanceTree(tropical, origin, sapling, RandomSource.create(7));
            if (!tropical.getBlockState(origin).is(expectedTrunk.getBlock())) {
                String actual = tropical.getBlockState(origin).toString();
                tropical.setBlock(origin, Blocks.AIR.defaultBlockState(), 2);
                GrowthPlacementBuffer proposed = new GrowthPlacementBuffer(tropical);
                boolean rawPlaced = tropical.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                        .getHolderOrThrow(TreeFeatures.AZALEA_TREE).value().place(proposed,
                                tropical.getChunkSource().getGenerator(), RandomSource.create(7), origin);
                Set<String> denied = new HashSet<>();
                proposed.isValid((pos, state) -> {
                    if (!BiomePlacementRestrictions.isAllowed(proposed, pos, state)) denied.add(state.toString());
                    return true;
                });
                throw new net.minecraft.gametest.framework.GameTestAssertException(
                        "Azalea growth failed: origin=" + actual + ", support=" + tropical.getBlockState(origin.below())
                                + ", rawFeature=" + rawPlaced + ", deniedStates=" + denied);
            }
            helper.assertTrue(tropical.getBlockState(origin).is(expectedTrunk.getBlock()),
                    "One azalea sapling must grow its loaded configured tree on tropical moss; origin="
                            + tropical.getBlockState(origin) + ", support=" + tropical.getBlockState(origin.below())
                            + ", expectedTrunk=" + expectedTrunk + ", logs=" + count(tropical, origin, expectedTrunk)
                            + ", azaleaLeaves=" + (count(tropical, origin, Blocks.AZALEA_LEAVES.defaultBlockState())
                            + count(tropical, origin, Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState())));
            helper.assertTrue(count(tropical, origin, expectedTrunk) >= 4,
                    "Azalea growth must build a complete trunk using the configured palette");
            helper.assertTrue(tropical.getBlockState(origin.below()).is(Blocks.MOSS_BLOCK),
                    "Azalea must retain its tropical moss instead of introducing forbidden rooted dirt");
            helper.assertFalse(BiomePlacementRestrictions.isAllowed(tropical, origin.below(), Blocks.ROOTED_DIRT.defaultBlockState()),
                    "The tree adapter must not widen rooted-dirt placement permissions");
            helper.assertTrue(count(tropical, origin, Blocks.AZALEA_LEAVES.defaultBlockState())
                            + count(tropical, origin, Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState()) > 20,
                    "Azalea growth must include its actual leafy canopy");
        } finally {
            clear(tropical, origin);
        }
        // The same configured feature must retain its normal rooted-dirt behavior outside custom dimensions.
        ServerLevel vanilla = helper.getLevel();
        BlockPos control = new BlockPos(helper.absolutePos(BlockPos.ZERO).getX(), 220,
                helper.absolutePos(BlockPos.ZERO).getZ());
        clear(vanilla, control);
        vanilla.setBlock(control.below(), Blocks.MOSS_BLOCK.defaultBlockState(), 2);
        try {
            boolean placed = vanilla.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                    .getHolderOrThrow(TreeFeatures.AZALEA_TREE).value().place(vanilla,
                            vanilla.getChunkSource().getGenerator(), RandomSource.create(7), control);
            helper.assertTrue(placed && vanilla.getBlockState(control.below()).is(Blocks.ROOTED_DIRT),
                    "Vanilla-dimensional azalea must still generate rooted dirt");
        } finally {
            clear(vanilla, control);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void overlappingJungleTreeRejectsBeforeCocoaDecoration(GameTestHelper helper) {
        ServerLevel tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        BlockPos origin = new BlockPos(40, 220, 40);
        clear(tropical, origin);
        tropical.setBlock(origin.below(), Blocks.MOSS_BLOCK.defaultBlockState(), 2);
        for (int y = 0; y <= 15; y++) tropical.setBlock(origin.above(y), Blocks.JUNGLE_LOG.defaultBlockState(), 2);
        try {
            boolean placed = tropical.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                    .getHolderOrThrow(TreeFeatures.JUNGLE_TREE).value().place(tropical,
                            tropical.getChunkSource().getGenerator(), RandomSource.create(7), origin);
            helper.assertFalse(placed, "A jungle tree with no new trunk logs must reject before its cocoa decorator runs");
            for (BlockPos pos : volume(origin)) {
                if (pos.getY() < origin.getY()) continue;
                boolean existingTrunk = pos.getX() == origin.getX() && pos.getZ() == origin.getZ()
                        && pos.getY() <= origin.getY() + 15;
                helper.assertTrue(existingTrunk ? tropical.getBlockState(pos).is(Blocks.JUNGLE_LOG) : tropical.isEmptyBlock(pos),
                        "Rejected overlapping tree must preserve old logs and leave no new leaves or cocoa: " + pos);
            }
        } finally {
            clear(tropical, origin);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void singlePaleOakSaplingUsesVariableVanillaShape(GameTestHelper helper) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(GameplayDimensions.NETHER);
        BlockPos origin = new BlockPos(40, 104, 40);
        setValleyBiome(helper, nether, origin);
        Set<Integer> heights = new HashSet<>();
        try {
            for (long seed : new long[]{1, 7, 19, 42}) {
                prepareSoulGround(nether, origin);
                BlockState sapling = ModBlocks.PALE_OAK_SAPLING.get().defaultBlockState().setValue(SaplingBlock.STAGE, 1);
                helper.assertTrue(nether.setBlock(origin, sapling, 2), "Fixture must place one pale-oak sapling");
                ModBlocks.PALE_OAK_SAPLING.get().advanceTree(nether, origin, sapling, RandomSource.create(seed));
                if (!nether.getBlockState(origin).is(ModBlocks.PALE_OAK_LOG.get())) {
                    String actual = nether.getBlockState(origin).toString();
                    nether.setBlock(origin, Blocks.AIR.defaultBlockState(), 2);
                    GrowthPlacementBuffer proposed = new GrowthPlacementBuffer(nether);
                    boolean rawPlaced = nether.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                            .getHolderOrThrow(PaleOakTreeGrower.PALE_OAK).value().place(proposed,
                                    nether.getChunkSource().getGenerator(), RandomSource.create(seed), origin);
                    Set<String> denied = new HashSet<>();
                    GrowthPlacementContext.runTree(proposed, origin, () -> proposed.isValid((pos, state) -> {
                        if (!TreeGrowthPlacementRules.isAllowed(proposed, origin, pos, state)
                                || !BiomePlacementRestrictions.isAllowed(proposed, pos, state)) {
                            denied.add(pos + "=" + state + "/" + proposed.getBiome(pos).unwrapKey());
                        }
                        return true;
                    }));
                    throw new net.minecraft.gametest.framework.GameTestAssertException(
                            "Pale oak sapling failed at seed=" + seed + ", origin=" + actual
                                    + ", support=" + nether.getBlockState(origin.below())
                                    + ", rawFeature=" + rawPlaced + ", denied=" + denied);
                }
                helper.assertTrue(nether.getBlockState(origin).is(ModBlocks.PALE_OAK_LOG.get())
                                && nether.getBlockState(origin.east().south()).is(ModBlocks.PALE_OAK_LOG.get()),
                        "A single pale-oak sapling must grow its full vanilla two-wide trunk using saved AoE block IDs");
                helper.assertTrue(count(nether, origin, ModBlocks.PALE_OAK_LEAVES.get().defaultBlockState()) > 40,
                        "Pale oak must generate its actual canopy on soul soil");
                int height = 0;
                for (BlockPos pos : volume(origin)) {
                    if (nether.getBlockState(pos).is(ModBlocks.PALE_OAK_LOG.get())) {
                        height = Math.max(height, pos.getY() - origin.getY() + 1);
                        helper.assertTrue(GameplayDimensions.biomeIs(nether, pos, "soul_sand_valley"),
                                "Every grown pale-oak log must stay inside the valley");
                    }
                }
                heights.add(height);
            }
            helper.assertTrue(heights.size() > 1, "Pale-oak trees must vary in actual height instead of always using the old fixed height");
        } finally {
            clear(nether, origin);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void paleOakRejectsWholeTreeAtBiomeBoundary(GameTestHelper helper) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(GameplayDimensions.NETHER);
        BlockPos origin = new BlockPos(88, 104, 40);
        setValleyBiome(helper, nether, origin);
        prepareSoulGround(nether, origin);
        var biomes = nether.registryAccess().registryOrThrow(Registries.BIOME);
        var valley = biomes.getHolderOrThrow(ResourceKey.create(Registries.BIOME, AOEMod.makeID("soul_sand_valley")));
        var wrong = biomes.getHolderOrThrow(ResourceKey.create(Registries.BIOME, AOEMod.makeID("basalt_deltas")));
        // A horizontal boundary remains strict even though a canopy may extend upward into another layer.
        WorldGenLevel boundary = (WorldGenLevel) Proxy.newProxyInstance(ConfiguredTreeGrowthTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getBiome")) {
                        return ((BlockPos) args[0]).getX() >= origin.getX() + 2 ? wrong : valley;
                    }
                    try {
                        return method.invoke(nether, args);
                    } catch (InvocationTargetException exception) {
                        throw exception.getCause();
                    }
                });
        try {
            boolean placed = nether.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                    .getHolderOrThrow(PaleOakTreeGrower.PALE_OAK).value().place(boundary,
                            nether.getChunkSource().getGenerator(), RandomSource.create(7), origin);
            helper.assertFalse(placed, "A pale-oak tree that crosses a horizontal valley boundary must fail");
            for (BlockPos pos : volume(origin)) {
                if (pos.getY() >= origin.getY()) {
                    helper.assertTrue(nether.isEmptyBlock(pos), "Rejected pale oak left part of its trunk, canopy, or moss at " + pos);
                }
            }
            helper.assertTrue(nether.getBlockState(origin.below()).is(Blocks.SOUL_SOIL),
                    "Rejected growth must preserve its support");
        } finally {
            clear(nether, origin);
        }
        helper.succeed();
    }

    private static Iterable<BlockPos> volume(BlockPos origin) {
        // The tree's existing spacing checks inspect two blocks below the origin as well.
        return BlockPos.betweenClosed(origin.offset(-8, -2, -8), origin.offset(8, 16, 8));
    }

    private static void clear(ServerLevel level, BlockPos origin) {
        for (BlockPos pos : volume(origin)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
    }

    private static long count(ServerLevel level, BlockPos origin, BlockState target) {
        long count = 0;
        for (BlockPos pos : volume(origin)) if (level.getBlockState(pos).is(target.getBlock())) count++;
        return count;
    }

    private static void prepareSoulGround(ServerLevel level, BlockPos origin) {
        clear(level, origin);
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-2, -1, -2), origin.offset(3, -1, 3))) {
            level.setBlock(pos, Blocks.SOUL_SOIL.defaultBlockState(), 2);
        }
    }

    private static void setValleyBiome(GameTestHelper helper, ServerLevel level, BlockPos origin) {
        BlockPos min = origin.offset(-12, -4, -12);
        BlockPos max = origin.offset(12, 20, 12);
        for (int x = min.getX() >> 4; x <= max.getX() >> 4; x++) {
            for (int z = min.getZ() >> 4; z <= max.getZ() >> 4; z++) level.getChunk(x, z);
        }
        int filled = level.getServer().getCommands().performPrefixedCommand(
                level.getServer().createCommandSourceStack().withLevel(level).withSuppressedOutput(),
                "fillbiome " + min.getX() + " " + min.getY() + " " + min.getZ() + " "
                        + max.getX() + " " + max.getY() + " " + max.getZ() + " aoemod:soul_sand_valley");
        helper.assertTrue(filled > 0 && GameplayDimensions.biomeIs(level, origin, "soul_sand_valley"),
                "Pale-oak fixture must have the actual soul-sand-valley biome");
    }
}
