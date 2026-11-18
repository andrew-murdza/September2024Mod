package net.amurdza.examplemod.gametest;

import com.mojang.serialization.JsonOps;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.BiomePlacementRestrictions;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.SnowyLakeVegetation;
import net.amurdza.examplemod.worldgen.SnowyPlantSnowlogging;
import net.amurdza.examplemod.worldgen.SnowyVegetation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import snownee.snow.CoreModule;
import snownee.snow.block.entity.SnowBlockEntity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SnowyEcologyRevisionTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void loadedPlantSourcesAndForestBudgetsPermitTheRequestedVegetation(GameTestHelper helper) {
        var snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        for (String biome : new String[]{"snowy_forest", "dappled_forest"}) {
            WorldGenLevel world = biomeWorld(snowy, biome);
            for (Block plant : new Block[]{Blocks.GRASS, Blocks.TALL_GRASS, Blocks.FERN, Blocks.LARGE_FERN}) {
                helper.assertTrue(BiomePlacementRestrictions.isAllowed(world, new BlockPos(0, 150, 0), plant.defaultBlockState()),
                        "Loaded dimension source tags must allow " + plant + " in " + biome);
            }
        }
        var registry = snowy.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        int nativeGrass = count(registry.get(new ResourceLocation("minecraft:patch_grass_forest")));
        helper.assertTrue(nativeGrass == 2 && SnowyVegetation.DAPPLED_GRASS_ATTEMPTS == nativeGrass,
                "Dappled forest must halve its previous four attempts to the native forest's two");
        var nativeDappled = snowy.registryAccess().registryOrThrow(Registries.BIOME).get(new ResourceLocation("minecraft:dappled_forest"));
        helper.assertTrue(nativeDappled != null && nativeDappled.getGenerationSettings().features().stream().flatMap(set -> set.stream())
                        .anyMatch(feature -> feature.unwrapKey().map(key -> key.location().toString().equals("minecraft:patch_grass_forest")).orElse(false)),
                "The installed native dappled biome must use the two-attempt forest-grass reference");
        var dappled = snowy.registryAccess().registryOrThrow(Registries.BIOME).get(AOEMod.makeID("dappled_forest"));
        helper.assertTrue(dappled.getGenerationSettings().features().stream().flatMap(set -> set.stream())
                        .anyMatch(feature -> feature.unwrapKey().map(key -> key.location().toString().contains("red_shrub")).orElse(false)),
                "Red shrubs must return to dappled forest generation");
        helper.assertTrue(count(registry.get(new ResourceLocation("aoemod:dappled/red_shrubs"))) == 4,
                "Returning red shrubs must preserve their prior four placement attempts");
        double treeAttempts = SnowySpruceRevisionTests.meanAttempts(registry.get(new ResourceLocation("aoemod:snowy/trees")));
        helper.assertTrue(treeAttempts == 1,
                "The shared pass must select one tree per eligible chunk: " + treeAttempts);
        for (String biome : new String[]{"snowy_forest", "dappled_forest"}) {
            var forest = snowy.registryAccess().registryOrThrow(Registries.BIOME).get(AOEMod.makeID(biome));
            helper.assertFalse(forest.getGenerationSettings().features().stream().flatMap(set -> set.stream())
                            .anyMatch(feature -> feature.unwrapKey().map(key -> key.location().toString())
                                    .filter(id -> id.equals("aoemod:snowy/grass") || id.equals("aoemod:snowy/large_ferns")
                                            || id.equals("aoemod:dappled/grass")).isPresent()),
                    "Legacy placed patches must not duplicate the actual forest-floor pass in " + biome);
        }
        helper.succeed();
    }

    private static int count(net.minecraft.world.level.levelgen.placement.PlacedFeature feature) {
        var count = (CountPlacement) feature.placement().stream().filter(CountPlacement.class::isInstance).findFirst()
                .orElseThrow(() -> new IllegalStateException("Loaded feature has no count modifier: " + feature.feature().unwrapKey()));
        var encoded = CountPlacement.CODEC.encodeStart(JsonOps.INSTANCE, count);
        return encoded.result().orElseThrow(() -> new IllegalStateException("Cannot read loaded feature count: "
                + encoded.error().map(error -> error.message()).orElse("missing value"))).getAsJsonObject().get("count").getAsInt();
    }

    private static com.google.gson.JsonElement encodePlacement(PlacementModifier modifier,
            com.mojang.serialization.DynamicOps<com.google.gson.JsonElement> ops) {
        var encoded = PlacementModifier.CODEC.encodeStart(ops, modifier);
        return encoded.result().orElseThrow(() -> new IllegalStateException("Cannot compare loaded shrub placement "
                + modifier.getClass().getSimpleName() + ": " + encoded.error().map(error -> error.message()).orElse("missing value")));
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void historicalGroveCoverageAndLakeMixtureRetainTheirMeasuredBudgets(GameTestHelper helper) {
        int grass = 0, fern = 0, large = 0;
        for (int x = -128; x < 128; x++) for (int z = -128; z < 128; z++) {
            var plant = SnowyVegetation.groveGroundCover(71521, x, z);
            helper.assertTrue(java.util.Objects.equals(plant, SnowyVegetation.groveGroundCover(71521, x, z)),
                    "Ground cover must be stable across revisits and chunk ordering");
            if (plant == null) continue;
            if (plant.is(Blocks.GRASS)) grass++;
            else if (plant.is(Blocks.FERN)) fern++;
            else if (plant.is(Blocks.LARGE_FERN)) large++;
            else helper.fail("Unexpected historical grove plant: " + plant);
        }
        int total = grass + fern + large;
        helper.assertTrue(total > 3600 && total < 4300 && grass > 1200 && grass < 1600
                        && fern > 2100 && fern < 2550 && large > 170 && large < 300,
                "Historical grove has 6% cover and 6:10:1 grass/fern/large-fern weights: " + grass + "/" + fern + "/" + large);
        for (int depth : new int[]{1, 8}) {
            Map<String, Integer> mixture = new HashMap<>();
            var random = RandomSource.create(98261);
            for (int i = 0; i < 64000; i++) mixture.merge(SnowyLakeVegetation.plant(random, depth), 1, Integer::sum);
            int sponges = mixture.getOrDefault("minecraft:wet_sponge", 0);
            int tall = mixture.getOrDefault("minecraft:tall_seagrass", 0);
            int seagrass = mixture.getOrDefault("minecraft:seagrass", 0) + tall;
            helper.assertTrue(seagrass > 58000 && mixture.getOrDefault("minecraft:kelp", 0) > 2100,
                    "Savanna-matching lake mixture must heavily favor seagrass and retain kelp: " + mixture);
            helper.assertTrue(depth == 1 ? sponges == 0 && tall == 0 : sponges > 120 && sponges < 300 && tall > 16000,
                    "Sponges are rare and tall seagrass needs sufficient depth: " + mixture);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void realSnowContainersPreservePlantsUpperHalvesAndSnowySoil(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var world = biomeWorld(level, "snowy_forest");
        BlockPos anchor = helper.absolutePos(new BlockPos(1, 0, 1));
        BlockPos root = new BlockPos(anchor.getX(), level.getMaxBuildHeight() - 8, anchor.getZ());
        for (Block plant : new Block[]{Blocks.GRASS, Blocks.FERN, Blocks.LARGE_FERN, Blocks.TALL_GRASS}) {
            level.setBlock(root.above(), Blocks.AIR.defaultBlockState(), 18);
            level.setBlock(root, Blocks.SNOW.defaultBlockState().setValue(BlockStateProperties.LAYERS, 2), 18);
            level.setBlock(root.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 18);
            helper.assertTrue(SnowyPlantSnowlogging.place(world, root, plant.defaultBlockState()),
                    "Every historical grove plant must generate inside real snow: " + plant);
            helper.assertTrue(level.getBlockState(root).is(CoreModule.TILE_BLOCK.get())
                            && level.getBlockState(root).getValue(BlockStateProperties.LAYERS) == 2,
                    "Snowlogging must retain existing snow depth");
            var container = (SnowBlockEntity) level.getBlockEntity(root);
            helper.assertTrue(container != null && container.getContainedState().is(plant), "The native container must hold the real plant");
            helper.assertTrue(level.getBlockState(root.below()).getValue(BlockStateProperties.SNOWY),
                    "Writes with worldgen flags must explicitly update the snowy grass-side texture");
            if (plant instanceof DoublePlantBlock) {
                var upper = level.getBlockState(root.above());
                helper.assertTrue(upper.is(plant) && upper.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER
                                && upper.canSurvive(world, root.above()),
                        "Large plants need a supported native upper half above their snowlogged lower half");
            }
            var restored = new SnowBlockEntity(root, level.getBlockState(root));
            restored.load(container.saveWithFullMetadata());
            helper.assertTrue(restored.getContainedState().equals(container.getContainedState()), "Snowlogged plants must persist through saving");
        }
        level.setBlock(root.above(), Blocks.AIR.defaultBlockState(), 18);
        level.setBlock(root, Blocks.AIR.defaultBlockState(), 18);
        level.setBlock(root.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 18);
        helper.assertTrue(SnowyPlantSnowlogging.coverSurface(world, root)
                        && level.getBlockState(root).is(Blocks.SNOW)
                        && level.getBlockState(root.below()).getValue(BlockStateProperties.SNOWY),
                "Bare forest-floor snow must also update the grass block beneath it");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void lakeDecorationWritesCompleteNativeAquaticsBelowIce(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        int surface = level.getChunkSource().getGenerator().getSeaLevel() - 1;
        for (int depth : new int[]{1, 8}) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            int floor = surface - depth;
            Function<BlockPos, BlockState> stateAt = pos -> blocks.getOrDefault(pos,
                    pos.getY() <= floor ? Blocks.SAND.defaultBlockState()
                            : pos.getY() <= surface ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState());
            var biome = level.registryAccess().registryOrThrow(Registries.BIOME)
                    .getHolderOrThrow(ResourceKey.create(Registries.BIOME, AOEMod.makeID("cold_ocean")));
            WorldGenLevel world = (WorldGenLevel) Proxy.newProxyInstance(SnowyEcologyRevisionTests.class.getClassLoader(),
                    new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "getBiome": return biome;
                            case "getBlockState": return stateAt.apply((BlockPos) args[0]);
                            case "getFluidState": return stateAt.apply((BlockPos) args[0]).getFluidState();
                            case "isStateAtPosition": return ((Predicate<BlockState>) args[1]).test(stateAt.apply((BlockPos) args[0]));
                            case "isFluidAtPosition": return ((Predicate<net.minecraft.world.level.material.FluidState>) args[1])
                                    .test(stateAt.apply((BlockPos) args[0]).getFluidState());
                            case "isEmptyBlock": return stateAt.apply((BlockPos) args[0]).isAir();
                            case "setBlock": blocks.put(((BlockPos) args[0]).immutable(), (BlockState) args[1]); return true;
                            default:
                                try { return method.invoke(level, args); }
                                catch (InvocationTargetException failure) { throw failure.getCause(); }
                        }
                    });
            // Include frozen columns without changing the submerged floor.
            if (depth > 1) for (int x = 0; x < 16; x++) blocks.put(new BlockPos(x, surface, 0), Blocks.ICE.defaultBlockState());
            int roots = 0, tall = 0, kelp = 0, sponges = 0;
            for (int chunk = 0; chunk < 16; chunk++) SnowyLakeVegetation.place(world, chunk * 16, 0);
            for (int x = 0; x < 256; x++) for (int z = 0; z < 16; z++) {
                BlockPos pos = new BlockPos(x, floor + 1, z);
                BlockState state = world.getBlockState(pos);
                if (state.is(Blocks.WATER)) continue;
                roots++;
                if (state.is(Blocks.TALL_SEAGRASS)) {
                    tall++;
                    helper.assertTrue(world.getBlockState(pos.above()).is(Blocks.TALL_SEAGRASS)
                                    && world.getBlockState(pos.above()).getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER,
                            "Generated tall seagrass requires its complete upper half");
                }
                if (state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT)) kelp++;
                if (state.is(Blocks.WET_SPONGE)) sponges++;
                else helper.assertTrue(state.getFluidState().is(FluidTags.WATER), "Aquatic plants must retain the existing water");
            }
            helper.assertTrue(roots == 4096 && kelp > 80 && (depth == 1 ? tall == 0 && sponges == 0 : tall > 800 && sponges < 40),
                    "Every exposed legal lakebed column gets a complete Savanna-weighted plant: roots=" + roots + ", tall=" + tall + ", kelp=" + kelp + ", sponge=" + sponges);
            for (var entry : blocks.entrySet()) helper.assertTrue(entry.getKey().getY() > floor && entry.getKey().getY() <= surface,
                    "Lake decoration cannot replace the floor or create water above its existing surface");
            if (depth > 1) for (int x = 0; x < 16; x++) helper.assertTrue(world.getBlockState(new BlockPos(x, surface, 0)).is(Blocks.ICE),
                    "Aquatic plants must preserve existing lake ice");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 2400)
    public static void freshlyDecoratedForestsContainGroundGrassSnowloggedPlantsAndBothBerries(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var generator = level.getChunkSource().getGenerator();
        var sampler = level.getChunkSource().randomState().sampler();
        int fresh = (int) ((System.currentTimeMillis() / 1000) % 10000) * 512;
        int originX = (4_300_000 + fresh) >> 4, originZ = -4_700_000 >> 4;
        int grass = 0, snowloggedGrass = 0;
        for (String target : new String[]{"dappled_forest", "minecraft:sweet_berry_bush", "aoemod:blue_berry_bush"}) {
            BlockPos chosen = null;
            String biome = target.equals("dappled_forest") ? target : "snowy_forest";
            var biomeKey = ResourceKey.create(Registries.BIOME, AOEMod.makeID(biome));
            if (!target.equals("dappled_forest")) {
                int tileX = Math.floorDiv(originX, 18), tileZ = Math.floorDiv(originZ, 18);
                for (int candidate = 0; candidate < 32 && chosen == null; candidate++) {
                    for (var patch : SnowyVegetation.berryTilePatches(level, tileX + candidate % 8, tileZ + candidate / 8)) {
                        if (patch.species().equals(target)) {
                            chosen = new BlockPos(Math.floorDiv(patch.x(), 16) * 16, 0, Math.floorDiv(patch.z(), 16) * 16);
                            break;
                        }
                    }
                }
            }
            search:
            for (int dz = 0; chosen == null && dz < 128 && target.equals("dappled_forest"); dz++) {
                candidate:
                for (int dx = 0; dx < 128; dx++) {
                int cx = originX + dx, cz = originZ + dz;
                for (int qx : new int[]{0, 7}) for (int qz : new int[]{0, 7}) {
                    if (!generator.getBiomeSource().getNoiseBiome(cx * 4 + qx, 40, cz * 4 + qz, sampler).is(biomeKey)) continue candidate;
                }
                chosen = new BlockPos(cx * 16, 0, cz * 16);
                break search;
                }
            }
            helper.assertTrue(chosen != null, "Must find fresh suitable forest terrain for " + target);
            int berryRoots = 0;
            for (int dx = 0; dx < 2; dx++) for (int dz = 0; dz < 2; dz++) {
                int cx = (chosen.getX() >> 4) + dx, cz = (chosen.getZ() >> 4) + dz;
                level.getChunk(cx, cz);
                for (int x = cx * 16; x < cx * 16 + 16; x++) for (int z = cz * 16; z < cz * 16 + 16; z++) {
                    BlockPos pos = SnowyVegetation.surfaceRoot(level, x, z);
                    if (pos == null) continue;
                    BlockState state = level.getBlockState(pos);
                    if (target.equals("dappled_forest") && GameplayDimensions.biomeIs(level, pos, "dappled_forest")
                            && (state.is(Blocks.GRASS) || state.is(Blocks.FERN))) grass++;
                    if (state.is(Blocks.SNOW) || state.is(CoreModule.TILE_BLOCK.get())) {
                        BlockState soil = level.getBlockState(pos.below());
                        if (soil.hasProperty(BlockStateProperties.SNOWY)) helper.assertTrue(soil.getValue(BlockStateProperties.SNOWY),
                                "Fresh worldgen snow must have snowy soil sides at " + pos);
                    }
                    if (!(level.getBlockEntity(pos) instanceof SnowBlockEntity snow)) continue;
                    var plant = snow.getContainedState();
                    if (plant.is(Blocks.GRASS) || plant.is(Blocks.FERN) || plant.is(Blocks.LARGE_FERN)) snowloggedGrass++;
                    if (dx == 0 && dz == 0 && net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(plant.getBlock()).toString().equals(target)) {
                        berryRoots++;
                        helper.assertTrue(plant.getValue(BlockStateProperties.AGE_3) == 3, "Natural berry patches must be mature and snowlogged");
                    }
                }
            }
            if (!target.equals("dappled_forest")) helper.assertTrue(berryRoots == 6,
                    "The seeded berry opportunity must produce six actual snowlogged bushes in fresh decorated terrain: " + target + "=" + berryRoots);
        }
        helper.assertTrue(grass > 0 && snowloggedGrass > 0,
                "Final generated forest chunks must visibly contain both dappled grass and snowy grove cover: " + grass + "/" + snowloggedGrass);
        System.out.println("SNOWY_ECOLOGY_FRESH dappledGrass=" + grass + " snowloggedGroundCover=" + snowloggedGrass + " berryRoots=12");
        helper.succeed();
    }

    private static WorldGenLevel biomeWorld(ServerLevel level, String name) {
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME)
                .getHolderOrThrow(ResourceKey.create(Registries.BIOME, AOEMod.makeID(name)));
        return (WorldGenLevel) Proxy.newProxyInstance(SnowyEcologyRevisionTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getBiome")) return biome;
                    try { return method.invoke(level, args); }
                    catch (InvocationTargetException failure) { throw failure.getCause(); }
                });
    }
}
