package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.DocumentedNaturalSpawns;
import net.amurdza.examplemod.worldgen.DocumentedFauna;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DocumentedEcologyTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void netherFloraReachesLowestFloorBelowExtraLedges(GameTestHelper helper) {
        var nether = helper.getLevel().getServer().getLevel(net.amurdza.examplemod.util.GameplayDimensions.NETHER);
        var basalt = nether.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME)
                .getHolderOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,
                        new ResourceLocation("aoemod:basalt_deltas")));
        Map<BlockPos, BlockState> placed = new HashMap<>();
        java.util.function.Function<BlockPos, BlockState> stateAt = pos -> placed.getOrDefault(pos,
                java.util.Set.of(20, 40, 60, 80, 100, 120).contains(pos.getY())
                        ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
        var world = (net.minecraft.world.level.WorldGenLevel) Proxy.newProxyInstance(
                DocumentedEcologyTests.class.getClassLoader(), new Class<?>[]{net.minecraft.world.level.WorldGenLevel.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getLevel" -> nether;
                    case "getMaxBuildHeight" -> 128;
                    case "getMinBuildHeight" -> 0;
                    case "getBiome" -> basalt;
                    case "getBlockState" -> stateAt.apply((BlockPos) args[0]);
                    case "getFluidState" -> stateAt.apply((BlockPos) args[0]).getFluidState();
                    case "isEmptyBlock" -> stateAt.apply((BlockPos) args[0]).isAir();
                    case "setBlock" -> {
                        placed.put(((BlockPos) args[0]).immutable(), (BlockState) args[1]);
                        yield true;
                    }
                    default -> method.invoke(nether, args);
                });
        new net.amurdza.examplemod.worldgen.DocumentedFloraFeature().place(
                new net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<>(java.util.Optional.empty(), world,
                        nether.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(48152), BlockPos.ZERO,
                        net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE));
        helper.assertTrue(placed.entrySet().stream().anyMatch(entry -> entry.getKey().getY() == 21
                        && net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(entry.getValue().getBlock())
                        .getNamespace().equals("mynethersdelight")),
                "Extra upper ledges must not prevent powder cane on the lowest Nether floor");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void powderCaneAcceptsBothBasaltDeltaSubstrates(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        for (var substrate : new net.minecraft.world.level.block.Block[]{Blocks.BASALT, Blocks.BLACKSTONE}) {
            helper.getLevel().setBlock(pos.below(), substrate.defaultBlockState(), 3);
            for (String id : new String[]{"powdery_cane", "powdery_cannon", "powdery_chubby_sapling"}) {
                var block = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new ResourceLocation("mynethersdelight", id));
                helper.assertTrue(block != null && block != Blocks.AIR, "Missing powder cane stage " + id);
                helper.assertTrue(block.defaultBlockState().canSurvive(helper.getLevel(), pos),
                        "Powder cane must survive on " + substrate + ": " + id);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void cornUsesConfiguredGrowthInsteadOfItsPrivateRandomGate(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(net.amurdza.examplemod.util.GameplayDimensions.TROPICAL);
        var anchor = helper.absolutePos(new BlockPos(1, 2, 1));
        var pos = new BlockPos(anchor.getX(), 220, anchor.getZ() + 512);
        var corn = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new ResourceLocation("hearthandharvest:corn_stalk"));
        var soil = vectorwing.farmersdelight.common.registry.ModBlocks.RICH_SOIL_FARMLAND.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.MOISTURE, 7);
        Map<BlockPos, BlockState> previous = new HashMap<>();
        for (int y = -1; y < 3; y++) previous.put(pos.above(y), level.getBlockState(pos.above(y)));
        level.setBlock(pos.below(), soil, 18);
        for (int y = 0; y < 3; y++) level.setBlock(pos.above(y), Blocks.AIR.defaultBlockState(), 18);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(
                alabaster.hearthandharvest.common.block.CornStalkBlock.hasSufficientLight(level, pos),
                "Native corn requires propagated fixture lighting")).thenExecute(() -> {
            var map = net.amurdza.examplemod.config.BlockConfig.BLOCK_GROWTH_CHANCE_BY_DIMENSION
                    .computeIfAbsent(level.dimension(), ignored -> new HashMap<>());
            Float original = map.get(corn);
            try {
                var state = corn.defaultBlockState();
                // Reset hydration after waiting: dry soil halves the configured rate and makes this assertion probabilistic.
                level.setBlock(pos.below(), soil, 18);
                level.setBlock(pos, state, 18);
                helper.assertTrue(state.canSurvive(level, pos) && level.getBlockState(pos.below()).isFertile(level, pos.below()),
                        "The fixture must support cultivated corn with fully hydrated farmland");
                map.put(corn, 0F);
                state.randomTick(level, pos, fixedGateRandom(true));
                helper.assertTrue(level.getBlockState(pos).getValue(alabaster.hearthandharvest.common.block.CornStalkBlock.AGE) == 0,
                        "A zero configured rate must block even a successful native corn growth roll");
                map.put(corn, 1F);
                state.randomTick(level, pos, fixedGateRandom(false));
                helper.assertTrue(level.getBlockState(pos).getValue(alabaster.hearthandharvest.common.block.CornStalkBlock.AGE) == 1,
                        "A configured rate of one must allow growth even when corn's private random roll fails");
            } finally {
                if (original == null) map.remove(corn); else map.put(corn, original);
                previous.forEach((position, saved) -> level.setBlock(position, saved, 18));
            }
        }).thenSucceed();
    }

    private static net.minecraft.util.RandomSource fixedGateRandom(boolean pass) {
        return (net.minecraft.util.RandomSource) Proxy.newProxyInstance(DocumentedEcologyTests.class.getClassLoader(),
                new Class<?>[]{net.minecraft.util.RandomSource.class}, (proxy, method, args) -> {
                    if (method.getName().equals("nextInt") && args != null && args.length == 1) {
                        return pass ? 0 : Math.max(1, (int) args[0] - 1);
                    }
                    throw new UnsupportedOperationException("Unexpected corn RNG call: " + method.getName());
                });
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void cultivatedCornRemainsSeedOnlyEvenOnSuitableTropicalFarmland(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(net.amurdza.examplemod.util.GameplayDimensions.TROPICAL);
        var anchor = helper.absolutePos(new BlockPos(1, 2, 1));
        var pos = new BlockPos(anchor.getX(), 220, anchor.getZ());
        var farmland = vectorwing.farmersdelight.common.registry.ModBlocks.RICH_SOIL_FARMLAND.get();
        level.setBlock(pos.below(), farmland.defaultBlockState(), 18);
        for (int i = 0; i < 3; i++) {
            level.setBlock(pos.above(i), Blocks.AIR.defaultBlockState(), 18);
        }
        helper.assertTrue(level.getBlockState(pos.below()).is(farmland),
                "The fixture must use permitted tropical farmland");
        helper.assertFalse(net.amurdza.examplemod.worldgen.DocumentedFloraFeature.placePlant(level, pos,
                        "hearthandharvest:corn_stalk", level.random),
                "Natural plant generation must decline cultivated corn even on suitable farmland");
        for (int i = 0; i < 3; i++) helper.assertTrue(level.isEmptyBlock(pos.above(i)),
                "Declining wild corn must leave every stalk section empty");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void fishRequireTheirDocumentedVerticalSpace(GameTestHelper helper) {
        Map<BlockPos, BlockState> states = new HashMap<>();
        LevelReader water = fluidColumn(helper, states);
        BlockPos pos = new BlockPos(0, 10, 0);
        var small = species(DocumentedFauna.Habitat.SMALL_WATER, DocumentedFauna.Depth.ANY);
        var large = species(DocumentedFauna.Habitat.LARGE_WATER, DocumentedFauna.Depth.ANY);
        var bottom = species(DocumentedFauna.Habitat.BOTTOM_WATER, DocumentedFauna.Depth.ANY);
        for (int y = -1; y <= 1; y++) states.put(pos.offset(0, y, 0), Blocks.WATER.defaultBlockState());
        helper.assertTrue(DocumentedFauna.hasSpace(water, pos, small), "A small fish needs three vertically connected water blocks");
        helper.assertTrue(DocumentedFauna.hasSpace(water, pos, large), "Swimming animals use three water blocks, with real body fit checked at PositionCheck");
        states.put(pos.above(2), Blocks.WATER.defaultBlockState());
        states.put(pos.below(2), Blocks.WATER.defaultBlockState());
        helper.assertTrue(DocumentedFauna.hasSpace(water, pos, large), "A large fish should fit five water blocks");
        helper.assertFalse(DocumentedFauna.hasSpace(water, pos, bottom), "Bottom fish require solid ground beneath the connected water column");
        states.put(pos.below(), Blocks.STONE.defaultBlockState());
        helper.assertTrue(DocumentedFauna.hasSpace(water, pos, bottom), "Bottom fish need solid ground and two water blocks overhead");
        helper.assertFalse(DocumentedFauna.hasSpace(water, pos, small), "A swimming small fish cannot replace a bottom fish on solid ground");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void snowyDepthAndSpawnReasonAreSeparate(GameTestHelper helper) {
        Map<BlockPos, BlockState> states = new HashMap<>();
        LevelReader water = fluidColumn(helper, states);
        BlockPos pos = new BlockPos(0, 10, 0);
        for (int y = -2; y <= 23; y++) states.put(pos.offset(0, y, 0), Blocks.WATER.defaultBlockState());
        var shallow = species(DocumentedFauna.Habitat.SMALL_WATER, DocumentedFauna.Depth.SHALLOW);
        var deep = species(DocumentedFauna.Habitat.SMALL_WATER, DocumentedFauna.Depth.DEEP);
        helper.assertTrue(DocumentedFauna.hasSpace(water, pos, shallow), "23 water blocks overhead must be shallow");
        helper.assertFalse(DocumentedFauna.hasSpace(water, pos, deep), "Deep species must not spawn above the depth boundary");
        states.put(pos.above(24), Blocks.WATER.defaultBlockState());
        helper.assertTrue(DocumentedFauna.hasSpace(water, pos, deep), "24 water blocks overhead must admit deep species");
        helper.assertFalse(DocumentedFauna.hasSpace(water, pos, shallow), "Shallow species must not spawn below the depth boundary");
        helper.assertTrue(DocumentedNaturalSpawns.natural(MobSpawnType.NATURAL), "Natural spawning must use ecology rules");
        helper.assertFalse(DocumentedNaturalSpawns.natural(MobSpawnType.SPAWN_EGG), "Spawn eggs must keep their original behavior");
        helper.assertFalse(DocumentedNaturalSpawns.natural(MobSpawnType.BREEDING), "Breeding must keep its original behavior");
        helper.assertFalse(DocumentedNaturalSpawns.natural(MobSpawnType.COMMAND), "Commands must keep their original behavior");
        helper.succeed();
    }

    private static DocumentedFauna.Species species(DocumentedFauna.Habitat habitat, DocumentedFauna.Depth depth) {
        return new DocumentedFauna.Species(new ResourceLocation("minecraft:cod"), habitat, depth);
    }

    private static LevelReader fluidColumn(GameTestHelper helper, Map<BlockPos, BlockState> states) {
        return (LevelReader) Proxy.newProxyInstance(LevelReader.class.getClassLoader(), new Class[]{LevelReader.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getBlockState")) return states.getOrDefault(args[0], Blocks.AIR.defaultBlockState());
                    if (method.getName().equals("getFluidState")) return states.getOrDefault(args[0], Blocks.AIR.defaultBlockState()).getFluidState();
                    return method.invoke(helper.getLevel(), args);
                });
    }
}
