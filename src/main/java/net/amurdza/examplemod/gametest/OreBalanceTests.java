package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.BiomePlacementRestrictions;
import net.amurdza.examplemod.util.GameplayDimensions;
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
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OreBalanceTests {
    private static final String TEMPLATE = "plant_gameplay_empty";

    @GameTest(template = TEMPLATE)
    public static void desertSulfurAcceptsStoneAndRejectsDeepslate(GameTestHelper helper) {
        ServerLevel desert = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        assertStoneOnly(helper, desert, "desert", "dimension_blobs/desert/sulfur");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void stoneOreBonusesCannotIncreaseDeepslateSupply(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        assertStoneOnly(helper, server.getLevel(GameplayDimensions.DESERT), "desert",
                "dimension_ores/stone_coal_17_50");
        assertStoneOnly(helper, server.getLevel(GameplayDimensions.SNOWY), "snowy_forest",
                "dimension_ores/snowy_emerald_stone_bonus");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void snowyGeodesGenerateTheirShellAndCrystals(GameTestHelper helper) {
        ServerLevel snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel level = solidHost(snowy, "snowy_forest", blocks);
        var placed = snowy.registryAccess().registryOrThrow(Registries.PLACED_FEATURE)
                .getHolderOrThrow(ResourceKey.create(Registries.PLACED_FEATURE,
                        AOEMod.makeID("snowy/amethyst_geode"))).value();
        var geode = placed.feature().value();
        helper.assertTrue(geode.place(level, snowy.getChunkSource().getGenerator(),
                        RandomSource.create(7), new BlockPos(0, -60, 0)),
                "The registered Snowy geode must generate inside solid host rock");
        helper.assertTrue(blocks.values().stream().anyMatch(s -> s.is(Blocks.SMOOTH_BASALT)),
                "The complete geode must retain its permitted smooth-basalt outer shell");
        helper.assertTrue(blocks.values().stream().anyMatch(s -> s.is(Blocks.CALCITE)),
                "The geode must retain its calcite middle shell");
        helper.assertTrue(blocks.values().stream().anyMatch(s -> s.is(Blocks.AMETHYST_BLOCK)),
                "The geode must generate amethyst inside the shell");
        helper.assertTrue(blocks.values().stream().anyMatch(s -> s.is(Blocks.BUDDING_AMETHYST)),
                "The geode must include budding amethyst for renewable crystals");
        helper.succeed();
    }

    private static void assertStoneOnly(GameTestHelper helper, ServerLevel backing, String biome, String feature) {
        WorldGenLevel level = solidHost(backing, biome, new HashMap<>());
        var configured = backing.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getHolderOrThrow(ResourceKey.create(Registries.CONFIGURED_FEATURE, AOEMod.makeID(feature))).value();
        var config = (OreConfiguration) configured.config();
        BlockPos.MutableBlockPos position = new BlockPos(0, 40, 0).mutable();
        boolean stone = OrePlacementContext.run(level, () -> config.targetStates.stream().anyMatch(target ->
                OreFeature.canPlaceOre(Blocks.STONE.defaultBlockState(), ignored -> Blocks.STONE.defaultBlockState(),
                        RandomSource.create(1), config, target, position)));
        boolean deepslate = OrePlacementContext.run(level, () -> config.targetStates.stream().anyMatch(target ->
                OreFeature.canPlaceOre(Blocks.DEEPSLATE.defaultBlockState(), ignored -> Blocks.DEEPSLATE.defaultBlockState(),
                        RandomSource.create(1), config, target, position)));
        helper.assertTrue(stone, feature + " must survive dimension permissions and replace stone");
        helper.assertFalse(deepslate, feature + " must not replace deepslate, including at a mixed-rock boundary");
        helper.assertTrue(OrePlacementContext.level() == null, "Ore context must be released after placement checks");
    }

    @SuppressWarnings("unchecked")
    private static WorldGenLevel solidHost(ServerLevel backing, String biome, Map<BlockPos, BlockState> blocks) {
        var biomeHolder = backing.registryAccess().registryOrThrow(Registries.BIOME)
                .getHolderOrThrow(ResourceKey.create(Registries.BIOME, new ResourceLocation(AOEMod.MOD_ID, biome)));
        return (WorldGenLevel) Proxy.newProxyInstance(OreBalanceTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getLevel")) return backing;
                    if (method.getName().equals("getBiome")) return biomeHolder;
                    if (method.getName().equals("getBlockState")) return blocks.getOrDefault(args[0], Blocks.STONE.defaultBlockState());
                    if (method.getName().equals("getFluidState")) return blocks.getOrDefault(args[0], Blocks.STONE.defaultBlockState()).getFluidState();
                    if (method.getName().equals("getBlockEntity")) return null;
                    if (method.getName().equals("setBlock")) {
                        BlockPos pos = ((BlockPos) args[0]).immutable();
                        BlockState state = (BlockState) args[1];
                        if (!BiomePlacementRestrictions.isAllowed((WorldGenLevel) proxy, pos, state)) return false;
                        blocks.put(pos, state);
                        return true;
                    }
                    if (method.getName().equals("ensureCanWrite")) return true;
                    if (method.getName().equals("isStateAtPosition")) return ((Predicate<BlockState>) args[1])
                            .test(blocks.getOrDefault(args[0], Blocks.STONE.defaultBlockState()));
                    try { return method.invoke(backing, args); }
                    catch (InvocationTargetException failure) { throw failure.getCause(); }
                });
    }
}
