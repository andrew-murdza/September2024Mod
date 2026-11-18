package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.DiskConfiguration;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlainsSurfaceTests {
    private PlainsSurfaceTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void bothLowlandBiomesUseVanillaSandGravelAndClayDisks(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess();
        for (String name : new String[]{"plains", "savanna"}) {
            var biome = registry.registryOrThrow(Registries.BIOME).get(new ResourceLocation("aoemod", name));
            Set<String> features = new HashSet<>();
            biome.getGenerationSettings().features().get(GenerationStep.Decoration.UNDERGROUND_ORES.ordinal())
                    .forEach(holder -> {
                        // Other mods append direct inline features without a registry key.
                        // Inspect registered sediment features without assuming every entry is named.
                        ResourceLocation id = holder.unwrapKey().map(ResourceKey::location).orElseGet(() ->
                                registry.registryOrThrow(Registries.PLACED_FEATURE).getKey(holder.value()));
                        if (id != null) features.add(id.toString());
                    });
            for (String material : new String[]{"sand", "gravel", "clay"}) {
                String featureId = name.equals("plains")
                        ? "aoemod:plains/disk_" + material : "minecraft:disk_" + material;
                helper.assertTrue(features.contains(featureId), name + " must schedule " + material + " disks");
                if (name.equals("plains")) helper.assertFalse(features.contains("minecraft:disk_" + material),
                        "The vanilla " + material + " pass must not duplicate its tuned Plains replacement");
            }
            if (name.equals("plains")) helper.assertTrue(features.contains("aoemod:dimension_blobs/plains/gravel")
                            && features.contains("aoemod:blobs/gravel"),
                    "Tuning water gravel disks must preserve both underground gravel-blob passes");
        }
        for (String material : new String[]{"sand", "gravel", "clay"}) {
            var placed = registry.registryOrThrow(Registries.PLACED_FEATURE).get(new ResourceLocation("aoemod:plains/disk_" + material));
            helper.assertTrue(placed.feature().value() == registry.registryOrThrow(Registries.CONFIGURED_FEATURE)
                    .get(new ResourceLocation("minecraft:disk_" + material)), "Tuned " + material + " retains native disk geometry and targets");
            var encoded = net.minecraft.world.level.levelgen.placement.PlacedFeature.DIRECT_CODEC.encodeStart(
                    net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, registry), placed)
                    .getOrThrow(false, message -> { }).getAsJsonObject().getAsJsonArray("placement").get(0).getAsJsonObject();
            if (material.equals("gravel")) {
                var counts = encoded.getAsJsonObject("count").getAsJsonArray("distribution");
                int weight = 0, attempts = 0;
                for (var entry : counts) {
                    int entryWeight = entry.getAsJsonObject().get("weight").getAsInt();
                    weight += entryWeight;
                    attempts += entryWeight * entry.getAsJsonObject().get("data").getAsInt();
                }
                helper.assertTrue(encoded.get("type").getAsString().equals("minecraft:count")
                                && attempts / (double) weight == .75,
                        "Water gravel must average 0.75 attempts per chunk, 50% above the previous 0.5");
            }
            else helper.assertTrue(encoded.get("type").getAsString().equals("minecraft:count")
                            && encoded.get("count").getAsInt() == (material.equals("clay") ? 2 : 1),
                    "Plains must double clay to two attempts and retain one-third sand at one attempt");
        }
        for (String material : new String[]{"sand", "gravel", "clay"}) {
            var feature = registry.registryOrThrow(Registries.CONFIGURED_FEATURE).get(new ResourceLocation("minecraft:disk_" + material));
            helper.assertTrue(feature.feature() == Feature.DISK, "River sediment must use a native disk feature");
            var config = (DiskConfiguration) feature.config();
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
                for (int y = 57; y <= 61; y++) blocks.put(new BlockPos(x, y, z), Blocks.DIRT.defaultBlockState());
                blocks.put(new BlockPos(x, 62, z), Blocks.WATER.defaultBlockState());
                blocks.put(new BlockPos(x, 63, z), Blocks.WATER.defaultBlockState());
            }
            WorldGenLevel world = fixture(helper, blocks);
            feature.place(world, world.getLevel().getChunkSource().getGenerator(), RandomSource.create(12345), new BlockPos(0, 61, 0));
            var expected = switch (material) { case "sand" -> Blocks.SAND; case "gravel" -> Blocks.GRAVEL; default -> Blocks.CLAY; };
            int replaced = 0;
            for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
                BlockPos pos = new BlockPos(x, 61, z);
                if (world.getBlockState(pos).is(expected)) {
                    replaced++;
                    helper.assertTrue(x * x + z * z <= config.radius().getMaxValue() * config.radius().getMaxValue(),
                            "Sediment escaped its native disk radius");
                }
                helper.assertTrue(world.getBlockState(pos.above()).is(Blocks.WATER)
                                && world.getBlockState(pos.below(4)).is(Blocks.DIRT),
                        "Sediment must preserve water and the deeper soil bed");
            }
            helper.assertTrue(replaced >= 13 && replaced < 169, "Expected a bounded sediment patch on a mostly dirt bed");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void generatedPlainsHasGrassyBanksAndDirtRiverBeds(GameTestHelper helper) {
        auditShore(helper, GameplayDimensions.PLAINS);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void nativeSedimentDisksPreserveDryBanksAndExistingCrops(GameTestHelper helper) {
        for (var dimension : java.util.List.of(GameplayDimensions.PLAINS, GameplayDimensions.SAVANNA)) {
            for (String material : new String[]{"sand", "gravel", "clay"}) {
                Map<BlockPos, BlockState> blocks = new HashMap<>();
                for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
                    for (int y = 58; y <= 62; y++) blocks.put(new BlockPos(x, y, z), Blocks.DIRT.defaultBlockState());
                    blocks.put(new BlockPos(x, 63, z), (x >= 0 ? Blocks.GRASS_BLOCK : Blocks.DIRT).defaultBlockState());
                    if (x < 0) blocks.put(new BlockPos(x, 64, z), Blocks.WATER.defaultBlockState());
                }
                var cabbage = vectorwing.farmersdelight.common.registry.ModBlocks.WILD_CABBAGES.get().defaultBlockState();
                BlockPos crop = new BlockPos(0, 64, 0);
                blocks.put(crop, cabbage);
                WorldGenLevel world = fixture(helper, blocks, dimension);
                var feature = helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                        .get(new ResourceLocation("minecraft:disk_" + material));
                feature.place(world, world.getLevel().getChunkSource().getGenerator(), RandomSource.create(12345), new BlockPos(0, 63, 0));
                helper.assertTrue(world.getBlockState(crop).equals(cabbage)
                                && world.getBlockState(crop.below()).is(Blocks.GRASS_BLOCK),
                        "Late neighboring " + material + " disk removed an existing crop's grassy bank in " + dimension.location());
                for (int x = 0; x <= 8; x++) for (int z = -8; z <= 8; z++) {
                    helper.assertTrue(world.getBlockState(new BlockPos(x, 63, z)).is(Blocks.GRASS_BLOCK),
                            "Native " + material + " disk replaced a dry grass shoreline");
                }
                var expected = switch (material) { case "sand" -> Blocks.SAND; case "gravel" -> Blocks.GRAVEL; default -> Blocks.CLAY; };
                helper.assertTrue(blocks.entrySet().stream().anyMatch(entry -> entry.getKey().getX() < 0
                                && entry.getKey().getY() == 63 && entry.getValue().is(expected)),
                        "Protecting grass shores must preserve underwater " + material + " disks");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void generatedSavannaHasGrassyBanksAndMudRiverBeds(GameTestHelper helper) {
        auditShore(helper, GameplayDimensions.SAVANNA);
        helper.succeed();
    }

    private static void auditShore(GameTestHelper helper, ResourceKey<Level> dimension) {
        var level = helper.getLevel().getServer().getLevel(dimension);
        var generator = level.getChunkSource().getGenerator();
        var random = level.getChunkSource().randomState();
        var density = random.router().initialDensityWithoutJaggedness();
        int seaLevel = generator.getSeaLevel();
        var wetBed = dimension.equals(GameplayDimensions.SAVANNA) ? Blocks.MUD : Blocks.DIRT;
        int originX = 3_300_000, originZ = -3_400_000;
        BlockPos shore = null;
        search: for (int z = originZ - 2048; z <= originZ + 2048; z += 32) {
            for (int x = originX - 2048; x <= originX + 2048; x += 16) {
                if (Math.abs(density.compute(new DensityFunction.SinglePointContext(x, seaLevel, z))) > 1.5) continue;
                int floor = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, random) - 1;
                if (floor < seaLevel - 5 || floor >= seaLevel - 1) continue;
                if (!generator.getBaseColumn(x, z, level, random).getBlock(floor + 1).is(Blocks.WATER)) continue;
                for (int[] offset : new int[][]{{24, 0}, {-24, 0}, {0, 24}, {0, -24}}) {
                    if (generator.getBaseHeight(x + offset[0], z + offset[1], Heightmap.Types.OCEAN_FLOOR_WG, level, random) >= seaLevel) {
                        shore = new BlockPos(x, floor, z);
                        break search;
                    }
                }
            }
        }
        helper.assertTrue(shore != null, "Did not locate fresh lowland water beside dry land in " + dimension.location());
        int wet = 0, dry = 0;
        for (int cx = (shore.getX() >> 4) - 2; cx <= (shore.getX() >> 4) + 2; cx++) {
            for (int cz = (shore.getZ() >> 4) - 2; cz <= (shore.getZ() >> 4) + 2; cz++) {
                // SURFACE isolates base materials from later native sediment disks.
                ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.SURFACE, true);
                for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
                    int bed = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, dx, dz);
                    BlockPos pos = new BlockPos(cx * 16 + dx, bed, cz * 16 + dz);
                    boolean water = chunk.getFluidState(pos.above()).is(FluidTags.WATER);
                    helper.assertTrue(chunk.getBlockState(pos).is(water ? wetBed : Blocks.GRASS_BLOCK),
                            "River base must be " + wetBed + " underwater and grass on dry banks at " + pos + " in " + dimension.location());
                    if (water) wet++; else dry++;
                    for (int y = level.getMinBuildHeight() + 5; y < bed - 5; y++) {
                        var below = chunk.getBlockState(new BlockPos(pos.getX(), y, pos.getZ()));
                        helper.assertFalse(below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.DIRT) || below.is(Blocks.ROOTED_DIRT),
                                "Density was mistaken for height and soil leaked onto a cave floor at " + pos.getX() + "," + y + "," + pos.getZ());
                    }
                }
            }
        }
        helper.assertTrue(wet > 0 && dry > 0, "Generated shore audit must include submerged beds and grassy dry banks");
        AOEMod.LOGGER.info("{} fresh shore {}: {} wet {} columns, {} dry grass columns", dimension.location(), shore, wet, wetBed, dry);
    }

    private static WorldGenLevel fixture(GameTestHelper helper, Map<BlockPos, BlockState> blocks) {
        return fixture(helper, blocks, GameplayDimensions.PLAINS);
    }

    private static WorldGenLevel fixture(GameTestHelper helper, Map<BlockPos, BlockState> blocks, ResourceKey<Level> dimension) {
        var level = helper.getLevel().getServer().getLevel(dimension);
        var chunk = new ProtoChunk(new ChunkPos(0, 0), UpgradeData.EMPTY, level,
                level.registryAccess().registryOrThrow(Registries.BIOME), null);
        return (WorldGenLevel) Proxy.newProxyInstance(PlainsSurfaceTests.class.getClassLoader(), new Class<?>[]{WorldGenLevel.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getLevel": return level;
                        case "getBlockState": return blocks.getOrDefault((BlockPos) args[0], Blocks.AIR.defaultBlockState());
                        case "getFluidState": return blocks.getOrDefault((BlockPos) args[0], Blocks.AIR.defaultBlockState()).getFluidState();
                        case "setBlock": blocks.put(((BlockPos) args[0]).immutable(), (BlockState) args[1]); return true;
                        case "getChunk": return chunk;
                    }
                    try { return method.invoke(level, args); }
                    catch (InvocationTargetException exception) { throw exception.getCause(); }
                });
    }
}
