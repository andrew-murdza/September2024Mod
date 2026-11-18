package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.MountainRidges;
import net.amurdza.examplemod.worldgen.MountainLakeOffset;
import net.amurdza.examplemod.worldgen.MushroomCaveFloors;
import net.amurdza.examplemod.worldgen.SnowyLakeCaves;
import net.amurdza.examplemod.worldgen.SnowyTerrainProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SnowyVanillaTerrainTests {
    private SnowyVanillaTerrainTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void carverExposedMushroomFloorsReplaceSurfaceOresAndPreserveAquifers(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                net.minecraft.resources.ResourceKey.create(Registries.BIOME, AOEMod.makeID("mushroom_caves")));
        var forest = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                net.minecraft.resources.ResourceKey.create(Registries.BIOME, AOEMod.makeID("snowy_forest")));
        var blocks = new java.util.HashMap<BlockPos, BlockState>();
        java.util.function.Function<BlockPos, BlockState> state = pos -> blocks.getOrDefault(pos,
                pos.getY() <= -11 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
        BlockPos floor = new BlockPos(8, -11, 8);
        blocks.put(floor.east().above(), Blocks.WATER.defaultBlockState());
        blocks.put(floor.east(2), Blocks.COAL_ORE.defaultBlockState());
        blocks.put(floor.east(3), Blocks.MUD.defaultBlockState());
        blocks.put(new BlockPos(14, 0, 8), Blocks.STONE.defaultBlockState());
        WorldGenLevel world = (WorldGenLevel) java.lang.reflect.Proxy.newProxyInstance(
                SnowyVanillaTerrainTests.class.getClassLoader(), new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "getLevel" -> level;
                        case "getBiome" -> ((BlockPos) args[0]).getX() == 13 ? forest : biome;
                        case "getBlockState" -> state.apply((BlockPos) args[0]);
                        case "getFluidState" -> state.apply((BlockPos) args[0]).getFluidState();
                        case "isEmptyBlock" -> state.apply((BlockPos) args[0]).isAir();
                        case "setBlock" -> { blocks.put(((BlockPos) args[0]).immutable(), (BlockState) args[1]); yield true; }
                        default -> method.invoke(level, args);
                    };
                });
        MushroomCaveFloors.prepare(world, 0, 0);
        helper.assertTrue(state.apply(floor).is(Blocks.MYCELIUM) && state.apply(floor.below()).is(Blocks.DIRT)
                        && state.apply(floor.below(2)).is(Blocks.DIRT) && state.apply(floor.below(3)).is(Blocks.STONE),
                "Dry stone exposed after surface rules needs the historical mycelium and dirt profile");
        helper.assertTrue(state.apply(floor.east()).is(Blocks.STONE) && state.apply(floor.east().above()).is(Blocks.WATER)
                        && state.apply(floor.east(2)).is(Blocks.MYCELIUM) && state.apply(floor.east(3)).is(Blocks.MUD),
                "Restoring mushroom soil replaces exposed ore while preserving aquifers and muddy beds");
        helper.assertTrue(state.apply(new BlockPos(16, -11, 8)).is(Blocks.STONE),
                "The soil preparation pass must remain in its owning chunk");
        helper.assertTrue(state.apply(new BlockPos(13, -11, 8)).is(Blocks.STONE)
                        && state.apply(new BlockPos(14, 0, 8)).is(Blocks.STONE),
                "The pass must stay inside the mushroom biome and its underground band");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void generatedLakeBanksFollowTheCurrentContourAndExactWaterBoundary(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        int repairedBedHoles = 0;
        for (long seed : new long[]{0, 12345}) {
            var random = RandomState.create(generator.generatorSettings().value(), level.registryAccess().lookupOrThrow(Registries.NOISE), seed);
            int bankColumns = 0, deepColumns = 0, crossings = 0;
            int minimumDepth = Integer.MAX_VALUE, maximumDepth = Integer.MIN_VALUE;
            for (int x : new int[]{-2048, 0, 2048}) {
                boolean previousLake = false;
                for (int z = -1920; z < 0; z += 4) {
                    double ridge = Math.abs(random.router().ridges().compute(new DensityFunction.SinglePointContext(x, 0, z)));
                    boolean lake = ridge < MountainLakeOffset.LAKE_WEIRDNESS;
                    if (z != -1920 && lake != previousLake) crossings++;
                    previousLake = lake;
                    if (!lake) continue;
                    double height = SnowyTerrainProfile.height(ridge);
                    boolean bank = height > SnowyTerrainProfile.SHORE_HEIGHT - MountainLakeOffset.MAX_LAKE_DEPTH + 4 && height < 60;
                    boolean deep = height == SnowyTerrainProfile.SHORE_HEIGHT - MountainLakeOffset.MAX_LAKE_DEPTH;
                    if (!(bank || deep && deepColumns < 24)) continue;
                    // getBaseColumn stops before fillFromNoise's completion
                    // hook. Include that production pass in the bank audit.
                    var column = generator.getBaseColumn(x, z, level, random);
                    int firstAir = (int) Math.ceil(height + .5);
                    if (column.getBlock(firstAir - 1).isAir()) repairedBedHoles++;
                    finishSampledColumn(level, random, x, z, column);
                    helper.assertTrue(column.getBlock(firstAir - 1).isSolid() && column.getBlock(firstAir).is(Blocks.WATER),
                            "The seeded bank must meet water at its exact contour height: seed=" + seed + ", column=" + x + "," + z
                                    + ", height=" + height + ", bed=" + column.getBlock(firstAir - 1) + ", water=" + column.getBlock(firstAir));
                    helper.assertTrue(column.getBlock(62).is(Blocks.WATER) && column.getBlock(63).isAir(),
                            "Water must end exactly below Snowy's sea level, including on its newly widened banks");
                    for (int y = firstAir - 4; y < firstAir; y++) helper.assertTrue(column.getBlock(y).isSolid(),
                            "No cheese chamber may enter the protected lake substrate at " + x + "," + y + "," + z);
                    int depth = generator.getSeaLevel() - firstAir;
                    minimumDepth = Math.min(minimumDepth, depth);
                    maximumDepth = Math.max(maximumDepth, depth);
                    if (bank) {
                        bankColumns++;
                        double expected = SnowyTerrainProfile.SHORE_HEIGHT - 500 * (MountainLakeOffset.LAKE_WEIRDNESS - ridge);
                        helper.assertTrue(Math.abs(height - expected) < 1e-9,
                                "The non-eased underwater bank must retain the current 500-block contour grade");
                    } else deepColumns++;
                }
            }
            helper.assertTrue(bankColumns >= 40 && deepColumns == 24 && crossings >= 9,
                    "Bank audit must span sloping water, deep bottoms and repeated boundaries: seed=" + seed
                            + ", banks=" + bankColumns + ", deep=" + deepColumns + ", crossings=" + crossings);
            helper.assertTrue(minimumDepth < 10 && maximumDepth == (int) MountainLakeOffset.MAX_LAKE_DEPTH,
                    "Cold Ocean needs shallow banks and the revised deepest water");
            AOEMod.LOGGER.info("Snowy exact bank audit: seed={} bankColumns={} deepColumns={} crossings={} waterDepth={}..{}",
                    seed, bankColumns, deepColumns, crossings, minimumDepth, maximumDepth);
        }
        helper.assertTrue(repairedBedHoles > 0,
                "This audit must include raw interpolation holes repaired by the real completion passes");
        var wet = new SnowyTerrainProfile(DensityFunctions.constant(MountainLakeOffset.LAKE_WEIRDNESS - 1e-6), DensityFunctions.constant(-1));
        var dry = new SnowyTerrainProfile(DensityFunctions.constant(MountainLakeOffset.LAKE_WEIRDNESS + 1e-6), DensityFunctions.constant(-1));
        var cave = new DensityFunction.SinglePointContext(-1, -16, -1);
        helper.assertTrue(wet.compute(cave) < 0 && dry.compute(cave) < 0,
                "Cheese caves continue beneath both sides of the shore while the bed remains protected");
        helper.succeed();
    }

    private static void finishSampledColumn(ServerLevel level, RandomState random, int x, int z,
                                            net.minecraft.world.level.NoiseColumn column) {
        var chunk = new net.minecraft.world.level.chunk.ProtoChunk(new net.minecraft.world.level.ChunkPos(x >> 4, z >> 4),
                net.minecraft.world.level.chunk.UpgradeData.EMPTY, level,
                level.registryAccess().registryOrThrow(Registries.BIOME), null);
        var pos = new BlockPos.MutableBlockPos();
        for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++)
            chunk.setBlockState(pos.set(x, y, z), column.getBlock(y), false);
        net.amurdza.examplemod.worldgen.SnowyOceanFloor.restoreCoverAndWater(chunk, random, Blocks.STONE.defaultBlockState());
        net.amurdza.examplemod.worldgen.SnowyCaveColumns.finish(chunk, random, Blocks.STONE.defaultBlockState());
        for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++)
            column.setBlock(y, chunk.getBlockState(pos.set(x, y, z)));
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void completeRidgeDomainRetainsSeededBendsAndEveryContourAtBothScales(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var settings = ((NoiseBasedChunkGenerator) level.getChunkSource().getGenerator()).generatorSettings().value();
        var noises = level.registryAccess().lookupOrThrow(Registries.NOISE);
        java.util.Set<Long> seedContours = new java.util.HashSet<>();
        for (long seed : new long[]{0, 12345, 98765}) {
            var random = RandomState.create(settings, noises, seed);
            var repeated = RandomState.create(settings, noises, seed);
            for (int stretch : new int[]{1, 2}) {
                var ridges = new MountainRidges(new DensityFunction.NoiseHolder(noises.getOrThrow(Noises.RIDGE),
                        random.getOrCreateNoise(Noises.RIDGE)), new DensityFunction.NoiseHolder(noises.getOrThrow(Noises.SHIFT),
                        random.getOrCreateNoise(Noises.SHIFT)), stretch);
                var repeat = new MountainRidges(new DensityFunction.NoiseHolder(noises.getOrThrow(Noises.RIDGE),
                        repeated.getOrCreateNoise(Noises.RIDGE)), new DensityFunction.NoiseHolder(noises.getOrThrow(Noises.SHIFT),
                        repeated.getOrCreateNoise(Noises.SHIFT)), stretch);
                for (int x : new int[]{-2048, 0, 2048}) {
                    double minimum = 1, maximum = -1, closestLake = 1;
                    for (int z = -960 * stretch; z < 0; z += 4) {
                        var point = new DensityFunction.SinglePointContext(x, 0, z);
                        double contour = ridges.compute(point);
                        helper.assertTrue(contour == repeat.compute(point), "Recreating a world seed must retain identical mountain bands");
                        helper.assertTrue(contour == ridges.compute(new DensityFunction.SinglePointContext(x, 128, z)),
                                "The shared biome and surface contour must be independent of query height");
                        minimum = Math.min(minimum, contour);
                        maximum = Math.max(maximum, contour);
                        closestLake = Math.min(closestLake, Math.abs(contour));
                    }
                    helper.assertTrue(minimum < -.89 && maximum > .89 && closestLake < .01,
                            "Weak noise peaks must not erase snowy summits or lake crossings at scale " + stretch);
                }
                seedContours.add(Double.doubleToLongBits(ridges.compute(new DensityFunction.SinglePointContext(137, 0, -293))));
            }
        }
        helper.assertTrue(seedContours.size() == 6, "World seeds and stretch must still change the positions of the complete bands");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void mushroomCavesUseOneChunkDecorationBudget(GameTestHelper helper) {
        var features = helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        var active = features.get(new ResourceLocation("aoemod", "mushroom_caves/floor/forest_floor_full"));
        helper.assertTrue(active.feature() instanceof net.amurdza.examplemod.worldgen.feature.features.MushroomCaveVegetationFeature,
                "The loaded cave feature must use one chunk lottery rather than repeated layered nine-mushroom patches");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void seededCheeseCavitiesRetainRockPillarsAndNoIndependentOutlets(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var settings = ((NoiseBasedChunkGenerator) level.getChunkSource().getGenerator()).generatorSettings().value();
        var registry = level.registryAccess().registryOrThrow(Registries.DENSITY_FUNCTION);
        var cheese = registry.get(new ResourceLocation("aoemod", "snowy/caves/cheese"));
        var terrain = registry.get(new ResourceLocation("aoemod", "snowy/terrain_density"));
        helper.assertTrue(cheese != null && terrain != null, "The cave audit requires the active cheese field and surface profile");
        long caveAir = 0, rock = 0, solidMushroom = 0, openMushroom = 0;
        for (long seed : new long[]{12345, 98765, -8152788754161584091L}) {
            var chambers = state(level, settings, cheese, seed);
            var actual = state(level, settings, settings.noiseRouter().finalDensity(), seed);
            var uncarved = state(level, settings, terrain, seed);
            SnowyTerrainProfile[] found = {null};
            actual.router().finalDensity().mapAll(function -> {
                if (function instanceof SnowyTerrainProfile profile && !profile.surfaceOnly()) found[0] = profile;
                return function;
            });
            helper.assertTrue(found[0] != null, "The loaded final density must use the current cheese-only terrain profile");
            helper.assertTrue(found[0].pillars().maxValue() < .03,
                    "Native sheet-like pillars must be absent before the round support postpass");
            for (int x = -1536; x <= 1536; x += 96) for (int z = -1536; z <= 1536; z += 96) {
                if (SnowyLakeCaves.lakeColumn(actual, x, z)) continue;
                for (int y = -60; y <= 100; y += 8) {
                    var point = new DensityFunction.SinglePointContext(x, y, z);
                    if (uncarved.router().finalDensity().compute(point) <= 0) continue;
                    double chamber = chambers.router().finalDensity().compute(point);
                    double density = actual.router().finalDensity().compute(point);
                    helper.assertTrue(Math.abs(chamber - found[0].caverns().compute(point)) < 1e-12,
                            "The active profile must retain the same seeded vanilla-scale cheese graph");
                    if (density < 0) {
                        caveAir++;
                        helper.assertTrue(chamber < 0,
                                "No separate outlet, spaghetti or expanded historical graph may open rock where cheese is solid");
                    } else rock++;
                    if (y >= -32 && y < 0) {
                        if (density < 0) openMushroom++; else solidMushroom++;
                    }
                }
            }
        }
        helper.assertTrue(caveAir > 100 && rock > caveAir,
                "Current cheese caves must retain substantial air and surrounding rock");
        helper.assertTrue(solidMushroom > openMushroom && openMushroom > 100,
                "The underground mushroom habitat must retain caves within rock instead of a forced air layer");
        AOEMod.LOGGER.info("Snowy active cheese audit: caveAir={} rock={} mushroomSolid={} mushroomOpen={}",
                caveAir, rock, solidMushroom, openMushroom);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void climateBoundsAndWidenedWeirdnessChooseSurfaceBiomes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        helper.assertTrue(generator.getSeaLevel() == 63, "Mountain lakes use vanilla sea level");
        for (long seed : new long[]{0, 12345, 98765}) {
            var random = state(level, generator.generatorSettings().value(),
                    generator.generatorSettings().value().noiseRouter().finalDensity(), seed);
            int lake = 0, dappled = 0, snowy = 0;
            for (int x = -4096; x <= 4096; x += 128) for (int z = -4096; z <= 4096; z += 128) {
                var point = new DensityFunction.SinglePointContext(x, 0, z);
                helper.assertTrue(random.router().continents().compute(point) >= 0.4,
                        "Snowy continentalness must not fall below0.4");
                helper.assertTrue(random.router().erosion().compute(point) <= -0.15,
                        "Snowy erosion must not exceed-0.15");
                double weirdness = Math.abs(random.router().ridges().compute(point));
                // Biome climate values quantize to 0.0001 in vanilla.
                long quantized = Math.abs(net.minecraft.world.level.biome.Climate.quantizeCoord((float) weirdness));
                String expected = quantized < net.amurdza.examplemod.worldgen.MountainLakeOffset.LAKE_QUANTIZED_WEIRDNESS ? "cold_ocean"
                        : net.amurdza.examplemod.worldgen.SnowyTerrainProfile.height(net.minecraft.world.level.biome.Climate.unquantizeCoord(quantized)) > 70.5
                        ? "snowy_forest" : "dappled_forest";
                var biome = generator.getBiomeSource().getNoiseBiome(x >> 2, 40, z >> 2, random.sampler());
                helper.assertTrue(biome.unwrapKey().orElseThrow().location().getPath().equals(expected),
                        "Surface habitats must follow absolute weirdness at " + point);
                if (expected.equals("cold_ocean")) lake++;
                else if (expected.equals("dappled_forest")) dappled++;
                else snowy++;
            }
            helper.assertTrue(lake > 0 && dappled > 0 && snowy > 0,
                    "Every sampled seed must contain all three surface habitats");
            System.out.println("SNOWY_HABITATS seed=" + seed + " lake=" + lake + " dappled=" + dappled + " snowy=" + snowy);
        }
        helper.succeed();
    }

    private static RandomState state(ServerLevel level, NoiseGeneratorSettings settings,
                                     DensityFunction density, long seed) {
        NoiseRouter r = settings.noiseRouter();
        NoiseRouter replaced = new NoiseRouter(r.barrierNoise(), r.fluidLevelFloodednessNoise(), r.fluidLevelSpreadNoise(),
                r.lavaNoise(), r.temperature(), r.vegetation(), r.continents(), r.erosion(), r.depth(), r.ridges(),
                r.initialDensityWithoutJaggedness(), density, r.veinToggle(), r.veinRidged(), r.veinGap());
        NoiseGeneratorSettings configured = new NoiseGeneratorSettings(settings.noiseSettings(), settings.defaultBlock(),
                settings.defaultFluid(), replaced, settings.surfaceRule(), settings.spawnTarget(), settings.seaLevel(),
                settings.disableMobGeneration(), settings.isAquifersEnabled(), settings.oreVeinsEnabled(), settings.useLegacyRandomSource());
        return RandomState.create(configured, level.registryAccess().lookupOrThrow(Registries.NOISE), seed);
    }
}
