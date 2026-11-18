package net.amurdza.examplemod.gametest;

import com.mojang.serialization.JsonOps;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.BiomePlacementRestrictions;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.MushroomCaveFloors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.carver.CaveCarverConfiguration;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MushroomCaveRevisionTests {
    private MushroomCaveRevisionTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void mushgloomsAlwaysHaveTwoCompleteCapLayers(GameTestHelper helper) {
        var cap = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("twilightforest:huge_mushgloom"));
        var stem = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("twilightforest:huge_mushgloom_stem"));
        for (long seed = 0; seed < 32; seed++) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            var world = fixture(helper, blocks, -21);
            helper.assertTrue(place(world, seed), "Unobstructed mushgloom must grow for seed " + seed);
            var layers = new java.util.TreeMap<Integer, Integer>();
            blocks.forEach((pos, state) -> {
                if (state.is(cap)) layers.merge(pos.getY(), 1, Integer::sum);
            });
            helper.assertTrue(layers.size() == 2 && layers.lastKey() == layers.firstKey() + 1
                            && layers.values().stream().allMatch(count -> count == 9),
                    "Every mushgloom must have two full 3x3 cap layers for seed " + seed);
            for (int y = -20; y < layers.firstKey(); y++) helper.assertTrue(
                    world.getBlockState(new BlockPos(8, y, 8)).is(stem), "Stem must reach the full cap");
        }
        Map<BlockPos, BlockState> besideCap = new HashMap<>();
        besideCap.put(new BlockPos(10, -17, 10), Blocks.STONE.defaultBlockState());
        helper.assertTrue(place(fixture(helper, besideCap, -21), 11),
                "Stone outside the actual 3x3 cap must not reject a complete mushgloom");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void mushgloomStemAndEitherCapLayerRejectStoneAtomically(GameTestHelper helper) {
        for (BlockPos obstruction : new BlockPos[]{new BlockPos(8, -19, 8), new BlockPos(9, -17, 9)}) {
            for (long seed = 0; seed < 16; seed++) {
                Map<BlockPos, BlockState> blocks = new HashMap<>();
                blocks.put(obstruction, Blocks.STONE.defaultBlockState());
                var world = fixture(helper, blocks, -21);
                helper.assertFalse(place(world, seed), "Stone intersecting a stem or either cap layer must reject growth");
                helper.assertTrue(blocks.size() == 1 && blocks.get(obstruction).is(Blocks.STONE),
                        "A rejected mushgloom must leave no partial stem or cap");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void bottomBiomeFloorUsesAirHabitatAndMyceliumWinsOverOre(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        BlockPos floor = new BlockPos(8, -33, 8);
        blocks.put(floor, Blocks.COAL_ORE.defaultBlockState());
        blocks.put(floor.below(), Blocks.IRON_ORE.defaultBlockState());
        var world = fixture(helper, blocks, -33);
        helper.assertTrue(BiomePlacementRestrictions.isAllowed(world, floor, Blocks.MYCELIUM.defaultBlockState()),
                "Mycelium at Y-33 belongs to the mushroom cave air at Y-32");
        MushroomCaveFloors.prepare(world, 0, 0);
        helper.assertTrue(world.getBlockState(floor).is(Blocks.MYCELIUM),
                "Bottom-band cave soil must replace exposed ore with mycelium");
        helper.assertTrue(world.getBlockState(floor.below()).is(Blocks.IRON_ORE),
                "Underlying ore veins must survive soil restoration");
        var target = OreConfiguration.target(new BlockMatchTest(Blocks.MYCELIUM), Blocks.COAL_ORE.defaultBlockState());
        var config = new OreConfiguration(java.util.List.of(target), 8);
        helper.assertFalse(OreFeature.canPlaceOre(Blocks.MYCELIUM.defaultBlockState(), world::getBlockState,
                        RandomSource.create(3), config, target, floor.mutable()),
                "Even an ore rule explicitly targeting mycelium must preserve it");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void snowyDensityProfileKeepsLegacyCarversDisabled(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess();
        var functions = registry.registryOrThrow(Registries.DENSITY_FUNCTION);
        var ops = RegistryOps.create(JsonOps.INSTANCE, registry);
        for (String path : new String[]{"snowy/final_density", "snowy/caves/entrances"}) {
            String encoded = net.minecraft.world.level.levelgen.DensityFunction.DIRECT_CODEC.encodeStart(ops,
                    functions.get(new ResourceLocation("aoemod", path))).getOrThrow(false, message -> { }).toString();
            helper.assertFalse(encoded.contains("spaghetti") || encoded.contains("noodle"),
                    "The active cave/entrance graph must not include narrow spaghetti or noodle branches");
        }
        var carvers = registry.registryOrThrow(Registries.CONFIGURED_CARVER);
        for (String name : new String[]{"cave", "cave_extra_underground"}) {
            var widened = (CaveCarverConfiguration) carvers.get(new ResourceLocation("aoemod", "snowy_" + name)).config();
            var vanilla = (CaveCarverConfiguration) carvers.get(new ResourceLocation("minecraft", name)).config();
            double factor = Math.sqrt(1.3) * 2;
            helper.assertTrue(widened.probability == 0,
                    "Independent tunnel carvers must remain disabled because the density profile owns cave connections");
            helper.assertTrue(Math.abs(widened.horizontalRadiusMultiplier.getMinValue()
                            - vanilla.horizontalRadiusMultiplier.getMinValue() * factor) < 1e-6
                            && Math.abs(widened.verticalRadiusMultiplier.getMaxValue()
                            - vanilla.verticalRadiusMultiplier.getMaxValue() * factor) < 1e-6,
                    "Both tunnel radii must double relative to the previously expanded carvers");
        }
        helper.assertTrue(carvers.get(new ResourceLocation("aoemod", "snowy_canyon")).config().probability == 0,
                "Independent canyon carving must remain disabled to preserve the density profile's cave supports");
        helper.succeed();
    }

    private static boolean place(WorldGenLevel world, long seed) {
        var feature = world.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(new ResourceLocation("twilightforest:mushroom/big_mushgloom"));
        return feature.place(world, world.getLevel().getChunkSource().getGenerator(), RandomSource.create(seed),
                new BlockPos(8, -20, 8));
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void customMushroomCapsCannotConsumeLeaves(GameTestHelper helper) {
        for (String feature : new String[]{"twilightforest:mushroom/big_mushgloom", "aoemod:giant_mushrooms/huge_glow_shroom"}) {
            Map<BlockPos, BlockState> reference = new HashMap<>();
            var control = fixture(helper, reference, -21);
            var configured = control.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(new ResourceLocation(feature));
            helper.assertTrue(configured.place(control, control.getLevel().getChunkSource().getGenerator(),
                    RandomSource.create(11), new BlockPos(8, -20, 8)), "Reference custom mushroom must grow");
            Map<BlockPos, BlockState> blocked = new HashMap<>();
            // Cover all possible off-axis cap heights so a shorter retry cannot escape the obstacle.
            for (int y = -18; y < -4; y++) blocked.put(new BlockPos(9, y, 8), Blocks.OAK_LEAVES.defaultBlockState());
            var before = Map.copyOf(blocked);
            var world = fixture(helper, blocked, -21);
            helper.assertFalse(configured.place(world, world.getLevel().getChunkSource().getGenerator(),
                            RandomSource.create(11), new BlockPos(8, -20, 8)),
                    "Only red/brown cap blocks have permission to replace tree leaves: " + feature);
            helper.assertTrue(blocked.equals(before), "Rejected custom mushrooms must preserve every leaf atomically");
        }
        helper.succeed();
    }

    @SuppressWarnings("unchecked")
    private static WorldGenLevel fixture(GameTestHelper helper, Map<BlockPos, BlockState> blocks, int floorY) {
        ServerLevel level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var registry = level.registryAccess().registryOrThrow(Registries.BIOME);
        var mushroom = registry.getHolderOrThrow(ResourceKey.create(Registries.BIOME, AOEMod.makeID("mushroom_caves")));
        var forest = registry.getHolderOrThrow(ResourceKey.create(Registries.BIOME, AOEMod.makeID("snowy_forest")));
        java.util.function.Function<BlockPos, BlockState> state = pos -> blocks.getOrDefault(pos,
                pos.getY() < floorY ? Blocks.STONE.defaultBlockState()
                        : pos.getY() == floorY ? Blocks.MYCELIUM.defaultBlockState() : Blocks.AIR.defaultBlockState());
        return (WorldGenLevel) Proxy.newProxyInstance(MushroomCaveRevisionTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getLevel": return level;
                        case "getBiome": {
                            int y = ((BlockPos) args[0]).getY();
                            return y >= -32 && y < 0 ? mushroom : forest;
                        }
                        case "getBlockState": return state.apply((BlockPos) args[0]);
                        case "getFluidState": return state.apply((BlockPos) args[0]).getFluidState();
                        case "isStateAtPosition": return ((Predicate<BlockState>) args[1]).test(state.apply((BlockPos) args[0]));
                        case "isFluidAtPosition": return ((Predicate<net.minecraft.world.level.material.FluidState>) args[1])
                                .test(state.apply((BlockPos) args[0]).getFluidState());
                        case "isEmptyBlock": return state.apply((BlockPos) args[0]).isAir();
                        case "setBlock": blocks.put(((BlockPos) args[0]).immutable(), (BlockState) args[1]); return true;
                        default:
                            try { return method.invoke(level, args); }
                            catch (InvocationTargetException exception) { throw exception.getCause(); }
                    }
                });
    }
}
