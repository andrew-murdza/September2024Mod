package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.util.DocumentedPlacementRules;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.GrowthPlacementContext;
import net.amurdza.examplemod.util.TreeGrowthPlacementRules;
import net.amurdza.examplemod.worldgen.OrePlacementContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VerticalCanopyAndOreTests {
    private static final String TEMPLATE = "plant_gameplay_empty";

    @GameTest(template = TEMPLATE)
    public static void canopyAllowanceIsVerticalBoundedAndScoped(GameTestHelper helper) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(GameplayDimensions.NETHER);
        WorldGenLevel level = layered(nether, new HashMap<>());
        BlockPos origin = new BlockPos(0, 60, 0);
        BlockState stem = Blocks.CRIMSON_STEM.defaultBlockState();
        helper.assertFalse(DocumentedPlacementRules.isBiomeAllowed(level, origin.atY(64), stem, false),
                "Player-placed raw stems must retain the strict habitat rule");
        GrowthPlacementContext.runTree(level, origin, () -> {
            helper.assertTrue(DocumentedPlacementRules.isBiomeAllowed(level, origin.atY(79), stem, true),
                    "The sixteenth block above the crimson boundary must be allowed during tree growth");
            helper.assertFalse(DocumentedPlacementRules.isBiomeAllowed(level, origin.atY(80), stem, true),
                    "The seventeenth block above the native boundary must be rejected");
            helper.assertFalse(TreeGrowthPlacementRules.isAllowed(level, origin, new BlockPos(16, 65, 0), stem),
                    "A horizontal biome boundary must not gain a canopy allowance");
            helper.assertFalse(TreeGrowthPlacementRules.isAllowed(level, origin.atY(66), origin.atY(70), stem),
                    "A foreign planting origin must fail even when the target is within sixteen blocks of crimson");
        });
        helper.assertFalse(GrowthPlacementContext.isGrowing(), "Tree context must not leak to later placement");
        helper.assertFalse(DocumentedPlacementRules.isBiomeAllowed(level, origin.atY(65), stem, true),
                "An unrelated growth update must not inherit a tree's canopy permission");
        helper.assertFalse(TreeGrowthPlacementRules.isAllowed(level, origin.atY(120), origin.atY(128),
                ModBlocks.PALE_OAK_LEAVES.get().defaultBlockState()), "Pale oak must never cross the top dimension seam");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void realFungusGrowthCrossesUpperBoundaryAtomically(GameTestHelper helper) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(GameplayDimensions.NETHER);
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel level = layered(nether, blocks);
        var fungus = nether.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getHolderOrThrow(ResourceKey.create(Registries.CONFIGURED_FEATURE,
                        new ResourceLocation("minecraft", "crimson_fungus_planted"))).value();
        BlockPos origin = new BlockPos(0, 60, 0);
        blocks.put(origin.below(), Blocks.CRIMSON_NYLIUM.defaultBlockState());
        helper.assertTrue(fungus.place(level, nether.getChunkSource().getGenerator(), RandomSource.create(7), origin),
                "A crimson fungus rooted below Y64 must grow into the sixteen-block upper allowance");
        helper.assertTrue(blocks.entrySet().stream().anyMatch(e -> e.getKey().getY() >= 64
                        && (e.getValue().is(Blocks.CRIMSON_STEM) || e.getValue().is(Blocks.NETHER_WART_BLOCK))),
                "The actual generated tree must have committed a trunk or crown across the boundary");
        blocks.clear();
        BlockPos foreignOrigin = origin.atY(68);
        blocks.put(foreignOrigin.below(), Blocks.CRIMSON_NYLIUM.defaultBlockState());
        helper.assertFalse(fungus.place(level, nether.getChunkSource().getGenerator(), RandomSource.create(7), foreignOrigin),
                "Even forced foreign soil cannot authorize a crimson planting origin in warped forest");
        helper.assertTrue(blocks.size() == 1 && blocks.get(foreignOrigin.below()).is(Blocks.CRIMSON_NYLIUM),
                "A rejected whole-tree attempt must leave no partial stems, wart blocks or cleared cells");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void directOreWritesRespectTheBiomeAbove(GameTestHelper helper) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(GameplayDimensions.NETHER);
        WorldGenLevel level = layered(nether, new HashMap<>());
        var target = OreConfiguration.target(new BlockMatchTest(Blocks.NETHERRACK), Blocks.NETHER_GOLD_ORE.defaultBlockState());
        var config = new OreConfiguration(java.util.List.of(target), 9, 0);
        boolean inside = OrePlacementContext.run(level, () -> OreFeature.canPlaceOre(Blocks.NETHERRACK.defaultBlockState(),
                ignored -> Blocks.NETHERRACK.defaultBlockState(), RandomSource.create(1), config, target, new BlockPos(0, 94, 0).mutable()));
        boolean boundary = OrePlacementContext.run(level, () -> OreFeature.canPlaceOre(Blocks.NETHERRACK.defaultBlockState(),
                ignored -> Blocks.NETHERRACK.defaultBlockState(), RandomSource.create(1), config, target, new BlockPos(0, 95, 0).mutable()));
        helper.assertTrue(inside, "Netherrack gold must generate under warped forest");
        helper.assertFalse(boundary, "Direct ore section writes must reject netherrack gold with soul sand valley immediately above");
        helper.assertTrue(OrePlacementContext.level() == null, "Ore context must be released after the attempt");
        helper.succeed();
    }

    @SuppressWarnings("unchecked")
    private static WorldGenLevel layered(ServerLevel backing, Map<BlockPos, BlockState> blocks) {
        var biomes = backing.registryAccess().registryOrThrow(Registries.BIOME);
        return (WorldGenLevel) Proxy.newProxyInstance(VerticalCanopyAndOreTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getLevel")) return backing;
                    if (method.getName().equals("getBiome")) {
                        BlockPos pos = (BlockPos) args[0];
                        String name = pos.getX() >= 16 ? "warped_forest" : pos.getY() >= 96 ? "soul_sand_valley"
                                : pos.getY() >= 64 ? "warped_forest" : pos.getY() >= 32 ? "crimson_forest" : "basalt_deltas";
                        return biomes.getHolderOrThrow(ResourceKey.create(Registries.BIOME, AOEMod.makeID(name)));
                    }
                    if (method.getName().equals("getBlockState")) return blocks.getOrDefault(args[0], Blocks.AIR.defaultBlockState());
                    if (method.getName().equals("getFluidState")) return blocks.getOrDefault(args[0], Blocks.AIR.defaultBlockState()).getFluidState();
                    if (method.getName().equals("getBlockEntity")) return null;
                    if (method.getName().equals("setBlock")) {
                        blocks.put(((BlockPos) args[0]).immutable(), (BlockState) args[1]);
                        return true;
                    }
                    if (method.getName().equals("ensureCanWrite")) return true;
                    if (method.getName().equals("isOutsideBuildHeight")) {
                        int y = args[0] instanceof BlockPos pos ? pos.getY() : (int) args[0];
                        return y < 0 || y >= 128;
                    }
                    if (method.getName().equals("isStateAtPosition")) return ((Predicate<BlockState>) args[1])
                            .test(blocks.getOrDefault(args[0], Blocks.AIR.defaultBlockState()));
                    try { return method.invoke(backing, args); }
                    catch (InvocationTargetException failure) { throw failure.getCause(); }
                });
    }
}
