package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.AquaticPlantPlacement;
import net.amurdza.examplemod.worldgen.DocumentedFloraFeature;
import net.amurdza.examplemod.worldgen.PlainsSurfaceWater;
import net.amurdza.examplemod.worldgen.PlainsWaterProof;
import net.amurdza.examplemod.event_handlers.PlainsWaterProofEvents;
import net.amurdza.examplemod.worldgen.SnowyLakeVegetation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AquaticCaveRevisionTests {
    private static final Set<String> DEEP = Set.of("alexscaves:ping_pong_sponge", "betterend:charnia_light_blue", "betterend:charnia_cyan",
            "alexscaves:bone_worms");

    @GameTest(template = "plant_gameplay_empty")
    public static void drainingDisconnectedWaterPreservesWaterloggedStructures(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        BlockPos water = new BlockPos(5, 20, 8), stair = water.east(), chest = water.east(2), plant = water.east(3);
        BlockState dryStair = Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)
                .setValue(BlockStateProperties.HALF, Half.TOP);
        BlockState dryChest = Blocks.CHEST.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST);
        blocks.put(water, Blocks.WATER.defaultBlockState());
        blocks.put(stair, dryStair.setValue(BlockStateProperties.WATERLOGGED, true));
        blocks.put(chest, dryChest.setValue(BlockStateProperties.WATERLOGGED, true));
        blocks.put(plant, Blocks.SEAGRASS.defaultBlockState());
        WorldGenLevel world = drainWorld(helper, blocks, ChunkStatus.CARVERS);
        helper.assertTrue(PlainsSurfaceWater.drainDisconnected(world, 0, 0) == 4,
                "The sealed component must drain its water, plant, stairs, and chest");
        helper.assertTrue(world.getBlockState(water).isAir() && world.getBlockState(plant).isAir(),
                "A disconnected pool and its unsupported aquatic plant must become air");
        helper.assertTrue(world.getBlockState(stair).equals(dryStair) && world.getBlockState(chest).equals(dryChest),
                "Drainage must preserve structural blocks and every property except waterlogging");
        helper.assertTrue(PlainsSurfaceWater.drainDisconnected(world, 0, 0) == 0,
                "Repeated drainage must leave the dried structures alone");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void onlyCompletedNeighborGeometryCanProveWaterConnections(GameTestHelper helper) {
        for (ChunkStatus status : new ChunkStatus[]{ChunkStatus.CARVERS, ChunkStatus.FEATURES}) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            for (int x = 14; x <= 19; x++) blocks.put(new BlockPos(x, 20, 8), Blocks.WATER.defaultBlockState());
            for (int y = 21; y < 63; y++) blocks.put(new BlockPos(19, y, 8), Blocks.WATER.defaultBlockState());
            blocks.put(new BlockPos(19, 63, 8), Blocks.AIR.defaultBlockState());
            WorldGenLevel world = drainWorld(helper, blocks, status);
            int drained = PlainsSurfaceWater.drainDisconnected(world, 0, 0);
            BlockPos bridge = new BlockPos(15, 20, 8);
            if (status == ChunkStatus.FEATURES) {
                helper.assertTrue(drained == 0 && world.getBlockState(bridge).is(Blocks.WATER),
                        "A surface path through a completed neighbor must preserve the cave water");
                helper.assertTrue(PlainsWaterProof.contains(world.getChunk(0, 0), bridge),
                        "Actual drainage must persist a successful cross-chunk proof");
            } else {
                helper.assertTrue(drained == 2 && world.getBlockState(bridge).isAir(),
                        "An unfinished neighbor must not certify a path that later structures can seal");
                helper.assertFalse(PlainsWaterProof.contains(world.getChunk(0, 0), bridge),
                        "Unfinished neighbor geometry must not leave a persisted proof");
            }
            helper.assertTrue(world.getBlockState(new BlockPos(16, 20, 8)).is(Blocks.WATER),
                    "The drainage pass may only change its own chunk");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void connectedBridgeProofSurvivesPromotionAndChunkReload(GameTestHelper helper) {
        var level = helper.getLevel();
        var biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        var chunkPos = new net.minecraft.world.level.ChunkPos(-1, 0);
        var proto = new net.minecraft.world.level.chunk.ProtoChunk(chunkPos,
                net.minecraft.world.level.chunk.UpgradeData.EMPTY, level, biomes, null);
        BlockPos bridge = new BlockPos(-1, 20, 0);
        PlainsWaterProof.mark(proto, bridge);
        var promoted = new net.minecraft.world.level.chunk.LevelChunk(level, proto, null);
        helper.assertTrue(PlainsWaterProof.contains(promoted, bridge),
                "ProtoChunk promotion must preserve reservations for a still-undecorated neighboring path");
        var nbt = new net.minecraft.nbt.CompoundTag();
        PlainsWaterProofEvents.save(new net.minecraftforge.event.level.ChunkDataEvent.Save(promoted, level, nbt));
        helper.assertTrue(nbt.getLongArray(PlainsWaterProof.NBT_KEY).length <= 256,
                "Proof metadata is at most two KiB per chunk");
        var reloaded = new net.minecraft.world.level.chunk.ProtoChunk(chunkPos,
                net.minecraft.world.level.chunk.UpgradeData.EMPTY, level, biomes, null);
        PlainsWaterProofEvents.load(new net.minecraftforge.event.level.ChunkDataEvent.Load(reloaded, nbt,
                net.minecraft.world.level.chunk.ChunkStatus.ChunkType.PROTOCHUNK));
        helper.assertTrue(PlainsWaterProof.contains(reloaded, bridge)
                        && !PlainsWaterProof.contains(reloaded, bridge.west()),
                "Reload must restore the exact proven water cells without retaining neighboring sealed water");
        var wrapper = new net.minecraft.world.level.chunk.ImposterProtoChunk(promoted, false);
        helper.assertTrue(PlainsWaterProof.contains(wrapper, bridge), "Generation wrappers must see their real chunk's proof");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void onlyWaterPathsToTheSurfaceQualifyAcrossChunkSeams(GameTestHelper helper) {
        Set<BlockPos> water = new HashSet<>();
        // The tunnel crosses both negative and positive chunk seams and ends in a lake shaft.
        for (int x = -20; x <= 20; x++) water.add(new BlockPos(x, 20, 0));
        for (int y = 20; y <= 62; y++) water.add(new BlockPos(20, y, 0));
        BlockPos source = new BlockPos(20, 62, 0);
        helper.assertTrue(PlainsSurfaceWater.reachesSurface(new BlockPos(-20, 20, 0), water::contains, source::equals),
                "A bent tunnel crossing chunk boundaries must retain its surface-connected water");
        water.remove(new BlockPos(0, 20, 0));
        helper.assertFalse(PlainsSurfaceWater.reachesSurface(new BlockPos(-20, 20, 0), water::contains, source::equals),
                "A solid one-block seal must keep the remote cave dry");
        helper.assertTrue(PlainsSurfaceWater.reachesSurface(new BlockPos(1, 20, 0), water::contains, source::equals),
                "The connected half stays wet after the opposite half is sealed");
        water.add(new BlockPos(0, 20, 1));
        helper.assertFalse(PlainsSurfaceWater.reachesSurface(new BlockPos(-1, 20, 0), water::contains, source::equals),
                "Diagonal contact alone is not a water connection");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 200)
    public static void snowyDeepAndRoofedWaterRetainsTheSparseOriginalPlants(GameTestHelper helper) {
        for (boolean roof : new boolean[]{false, true}) {
            int depth = roof ? 5 : 32;
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = waterWorld(helper, blocks, depth, roof, GameplayDimensions.SNOWY);
            BlockPos root = new BlockPos(8, 63 - depth, 8);
            helper.assertTrue(SnowyLakeVegetation.usesDeepPalette(world, root),
                    "Deep open beds and shallow roofed aquifers both use the deep-water palette");
            var generator = world.getLevel().getChunkSource().getGenerator();
            new DocumentedFloraFeature().place(new FeaturePlaceContext<>(Optional.empty(), world, generator,
                    RandomSource.create(1731), BlockPos.ZERO, NoneFeatureConfiguration.INSTANCE));
            int plants = 0;
            for (var entry : blocks.entrySet()) {
                if (entry.getKey().getY() < root.getY() || entry.getKey().getY() >= 63) continue;
                String id = ForgeRegistries.BLOCKS.getKey(entry.getValue().getBlock()).toString();
                helper.assertTrue(DEEP.contains(id), "Deep beds must not receive shallow Savanna plants: " + id);
                plants++;
            }
            helper.assertTrue(plants > 0 && plants <= 40, "Keep the historical 40 sparse attempts, not a 256-column carpet: " + plants);
            if (roof) {
                for (int y = root.getY() - 2; y < root.getY(); y++) {
                    helper.assertTrue(world.getBlockState(new BlockPos(8, y, 8)).is(Blocks.MUD),
                            "Every roofed aquifer bed requires two mud layers");
                }
            }
        }
        var snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        BlockPos center = new BlockPos(7480, 151, 7448);
        var chunk = snowy.getChunkAt(center);
        var ocean = snowy.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                ResourceKey.create(Registries.BIOME, AOEMod.makeID("cold_ocean")));
        chunk.fillBiomesFromNoise((x, y, z, sampler) -> ocean, snowy.getChunkSource().randomState().sampler());
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-7, -1, -7), center.offset(7, 24, 7))) {
            chunk.setBlockState(pos, pos.getY() < center.getY() ? Blocks.MUD.defaultBlockState()
                    : Blocks.WATER.defaultBlockState(), false);
        }
        ItemStack bonemeal = new ItemStack(Items.BONE_MEAL, 2);
        helper.assertTrue(BoneMealItem.growWaterPlant(bonemeal, snowy, center, Direction.UP) && bonemeal.getCount() == 1,
                "Applying bonemeal at a deep bed must use one item");
        int grown = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-7, 0, -7), center.offset(7, 0, 7))) {
            BlockState state = chunk.getBlockState(pos);
            if (state.is(Blocks.WATER)) continue;
            var id = ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
            helper.assertTrue(DEEP.contains(id), "Deep bonemeal grew a plant outside the natural palette: " + id);
            grown++;
        }
        helper.assertTrue(grown > 0, "Deep bonemeal must populate the bed");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void buriedAquiferThinLedgesGainTwoMudLayers(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel world = waterWorld(helper, blocks, 5, true, GameplayDimensions.SNOWY);
        BlockPos water = new BlockPos(8, 58, 8);
        blocks.put(water.below(2), Blocks.AIR.defaultBlockState());
        BlockPos protectedBed = water.east();
        blocks.put(protectedBed.below(2), Blocks.BEDROCK.defaultBlockState());
        SnowyLakeVegetation.prepareBuriedBeds(world, 0, 0);
        helper.assertTrue(world.getBlockState(water.below()).is(Blocks.MUD)
                        && world.getBlockState(water.below(2)).is(Blocks.MUD),
                "A thin cave ledge must gain its second mud block even when another cavity lies beneath it");
        helper.assertTrue(world.getBlockState(water).is(Blocks.WATER), "Bed thickening cannot replace the aquifer water");
        helper.assertTrue(world.getBlockState(protectedBed.below()).is(Blocks.MUD)
                        && world.getBlockState(protectedBed.below(2)).is(Blocks.BEDROCK),
                "The second mud layer must preserve bedrock");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void plainsBuriedWaterCannotReceiveWaterPlants(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel roofed = waterWorld(helper, blocks, 5, true, GameplayDimensions.PLAINS);
        BlockPos root = new BlockPos(8, 58, 8);
        helper.assertFalse(AquaticPlantPlacement.hasSkyExposure(roofed, root), "The fixture has an opaque cave ceiling");
        helper.assertFalse(DocumentedFloraFeature.placePlant(roofed, root, "minecraft:seagrass", RandomSource.create(1)),
                "Unlit Plains water must reject plants even through direct placement entry points");
        helper.assertTrue(blocks.isEmpty(), "Rejecting a buried aquatic plant must not modify its water");
        helper.succeed();
    }

    /** Two bounded chunks with real chunk statuses and no path to the backing server's terrain. */
    private static WorldGenLevel drainWorld(GameTestHelper helper, Map<BlockPos, BlockState> blocks,
                                            ChunkStatus neighborStatus) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        var biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        Map<Integer, ChunkAccess> chunks = new HashMap<>();
        for (int x = 0; x <= 1; x++) {
            ProtoChunk chunk = new ProtoChunk(new net.minecraft.world.level.ChunkPos(x, 0),
                    net.minecraft.world.level.chunk.UpgradeData.EMPTY, level, biomes, null);
            chunk.setStatus(x == 0 ? ChunkStatus.CARVERS : neighborStatus);
            chunks.put(x, chunk);
        }
        java.util.function.Function<BlockPos, BlockState> state = pos -> blocks.getOrDefault(pos,
                pos.getY() <= 63 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
        return (WorldGenLevel) Proxy.newProxyInstance(AquaticCaveRevisionTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "hasChunk": return (int) args[0] >= 0 && (int) args[0] <= 1 && (int) args[1] == 0;
                        case "getChunk": {
                            int x = args[0] instanceof BlockPos pos ? Math.floorDiv(pos.getX(), 16) : (int) args[0];
                            int z = args[0] instanceof BlockPos pos ? Math.floorDiv(pos.getZ(), 16) : (int) args[1];
                            ChunkAccess chunk = z == 0 ? chunks.get(x) : null;
                            ChunkStatus required = args.length >= 3 ? (ChunkStatus) args[2] : ChunkStatus.EMPTY;
                            if (chunk != null && chunk.getStatus().isOrAfter(required)) return chunk;
                            if (args.length == 4 && !(boolean) args[3]) return null;
                            throw new AssertionError("Drainage requested unavailable terrain at " + x + ", " + z);
                        }
                        case "getHeight": return args == null || args.length == 0 ? level.getHeight() : 64;
                        case "getBlockState": return state.apply((BlockPos) args[0]);
                        case "getFluidState": return state.apply((BlockPos) args[0]).getFluidState();
                        case "setBlock": blocks.put(((BlockPos) args[0]).immutable(), (BlockState) args[1]); return true;
                    }
                    try { return method.invoke(level, args); }
                    catch (InvocationTargetException failure) { throw failure.getCause(); }
                });
    }

    private static WorldGenLevel waterWorld(GameTestHelper helper, Map<BlockPos, BlockState> blocks, int depth,
                                            boolean roof, ResourceKey<net.minecraft.world.level.Level> dimension) {
        var level = helper.getLevel().getServer().getLevel(dimension);
        int floor = 62 - depth;
        String biome = dimension.equals(GameplayDimensions.SNOWY) ? "mountain_lake" : "plains";
        var holder = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", biome)));
        java.util.function.Function<BlockPos, BlockState> stateAt = pos -> blocks.getOrDefault(pos,
                pos.getY() <= floor || roof && pos.getY() == 63 ? Blocks.STONE.defaultBlockState()
                        : pos.getY() < 63 ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState());
        return (WorldGenLevel) Proxy.newProxyInstance(AquaticCaveRevisionTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getBiome": return holder;
                        case "getSeed": return 1731L;
                        case "getHeight": return args == null || args.length == 0 ? level.getHeight() : 64;
                        case "getHeightmapPos": return new BlockPos(((BlockPos) args[1]).getX(), 64, ((BlockPos) args[1]).getZ());
                        case "getBlockState": return stateAt.apply((BlockPos) args[0]);
                        case "getFluidState": return stateAt.apply((BlockPos) args[0]).getFluidState();
                        case "isStateAtPosition": return ((java.util.function.Predicate<BlockState>) args[1]).test(stateAt.apply((BlockPos) args[0]));
                        case "isFluidAtPosition": return ((java.util.function.Predicate<net.minecraft.world.level.material.FluidState>) args[1]).test(stateAt.apply((BlockPos) args[0]).getFluidState());
                        case "isEmptyBlock": return stateAt.apply((BlockPos) args[0]).isAir();
                        case "setBlock": blocks.put(((BlockPos) args[0]).immutable(), (BlockState) args[1]); return true;
                    }
                    try { return method.invoke(level, args); }
                    catch (InvocationTargetException failure) { throw failure.getCause(); }
                });
    }
}
