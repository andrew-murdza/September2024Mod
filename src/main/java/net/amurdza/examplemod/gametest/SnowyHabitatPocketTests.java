package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DimensionBiomeSource;
import net.amurdza.examplemod.worldgen.SnowyCavePockets;
import net.amurdza.examplemod.worldgen.SnowyTerrainProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SnowyHabitatPocketTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void displayedHabitatsStayInOneColumnExceptMushroomStratum(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var registry = level.registryAccess().registryOrThrow(Registries.BIOME);
        var forest = registry.getHolderOrThrow(ResourceKey.create(Registries.BIOME, AOEMod.makeID("snowy_forest")));
        var dappled = registry.getHolderOrThrow(ResourceKey.create(Registries.BIOME, AOEMod.makeID("dappled_forest")));
        var cave = registry.getHolderOrThrow(ResourceKey.create(Registries.BIOME, AOEMod.makeID("mushroom_caves")));
        BiomeManager.NoiseBiomeSource checkerboard = (x, y, z) -> y >= -8 && y < 0 ? cave
                : ((x + z) & 1) == 0 ? forest : dappled;
        for (long seed : new long[]{0, 12345, -8152788754161584091L}) {
            var manager = new BiomeManager(level, seed).withDifferentSource(checkerboard).withDifferentSource(checkerboard);
            for (int x = -12; x <= 12; x++) for (int z = -12; z <= 12; z++) {
                var surface = manager.getBiome(new BlockPos(x, 64, z));
                for (int y : new int[]{-96, -64, -33, -32, -31, -1, 0, 1, 62, 63, 64, 65, 70, 128, 319}) {
                    helper.assertTrue(manager.getBiome(new BlockPos(x, y, z)).equals(y >= -32 && y < 0 ? cave : surface),
                            "Biome zoom must not change a column's habitat with Y, except its exact mushroom stratum");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void snowyForestBeginsAtBlockSeventyOneAndDeepColumnsMatchSurface(GameTestHelper helper) {
        double low = .3214, high = .85;
        for (int i = 0; i < 50; i++) {
            double middle = (low + high) / 2;
            if (SnowyTerrainProfile.height(middle) <= 70.5) low = middle; else high = middle;
        }
        helper.assertTrue(DimensionBiomeSource.snowySurfaceIndex(low - .00001) == 3
                        && DimensionBiomeSource.snowySurfaceIndex(high + .00001) == 0,
                "The forest boundary must track a solid top at Y71, not the former fixed weirdness .4");
        helper.assertTrue(DimensionBiomeSource.snowySurfaceIndex(.3214) == 3,
                "The exact dry shoreline must remain dappled forest");
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var source = level.getChunkSource().getGenerator().getBiomeSource();
        var sampler = level.getChunkSource().randomState().sampler();
        for (int x = -512; x <= 512; x += 16) for (int z = -512; z <= 512; z += 16) {
            var surface = source.getNoiseBiome(x, 16, z, sampler);
            for (int y : new int[]{-24, -16, -9, 0, 32, 79}) helper.assertTrue(
                    source.getNoiseBiome(x, y, z, sampler).equals(surface),
                    "The actual seeded biome source must keep its habitat below as well as above mushroom caves");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void tinyPocketsAreRemovedAcrossChunkEdgesWithoutChangingLargerCaves(GameTestHelper helper) {
        var tiny = new PocketFixture(helper, false);
        tiny.room(2, 20, 0, 7, 4, 4); // Nineteen supported floor tiles across a chunk boundary.
        int first = SnowyCavePockets.remove(tiny.world, 0, 0);
        helper.assertTrue(first == 14 * 8 && tiny.blocks.entrySet().stream()
                        .filter(e -> e.getKey().getX() >= 16).allMatch(e -> e.getValue().isAir()),
                "Cleanup may only fill its own chunk, even for a cross-boundary pocket");
        int second = SnowyCavePockets.remove(tiny.world, 16, 0);
        helper.assertTrue(first + second == 19 * 8 && tiny.blocks.values().stream().noneMatch(BlockState::isAir),
                "Both owners must remove the complete nineteen-floor-tile pocket");

        // Two pockets straddle the 400-column threshold. The larger one has a
        // narrow extension into a neighboring chunk which must remain completely intact.
        for (int footprint : new int[]{399, 400, 401}) {
            var room = new PocketFixture(helper, false);
            room.room(1, 25, 0, 11, 1, 15);
            room.room(1, footprint - 375, 0, 11, 16, 16);

            var original = Map.copyOf(room.blocks);
            for (int x : new int[]{0, 16}) for (int z : new int[]{0, 16}) SnowyCavePockets.remove(room.world, x, z);
            helper.assertTrue(footprint < 400 ? room.blocks.values().stream().noneMatch(BlockState::isAir)
                            : original.equals(room.blocks),
                    "Only caves strictly below 400 distinct connected X/Z columns may change");
        }
        var stacked = new PocketFixture(helper, false);
        stacked.room(2, 12, 0, 2, 4, 4);
        stacked.room(2, 12, 5, 7, 4, 4);
        stacked.room(2, 2, 3, 4, 4, 4);
        helper.assertTrue(SnowyCavePockets.remove(stacked.world, 0, 0) == stacked.blocks.size(),
                "Several connected stories above the same eleven X/Z positions still count as eleven columns");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void authoredAndUnavailableCavitiesAreNeverPartiallyFilled(GameTestHelper helper) {
        var authored = new PocketFixture(helper, false);
        authored.room(2, 3, 0, 7, 2, 2);
        authored.blocks.put(new BlockPos(3, 4, 2), Blocks.CAVE_AIR.defaultBlockState());
        var original = Map.copyOf(authored.blocks);
        helper.assertTrue(SnowyCavePockets.remove(authored.world, 0, 0) == 0 && authored.blocks.equals(original),
                "A room connected to authored cave air must be preserved in full");
        var unavailable = new PocketFixture(helper, true);
        unavailable.room(15, 16, 0, 7, 2, 2);
        original = Map.copyOf(unavailable.blocks);
        helper.assertTrue(SnowyCavePockets.remove(unavailable.world, 0, 0) == 0 && unavailable.blocks.equals(original),
                "An unavailable neighboring noise chunk must defer cleanup without any partial fill");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1800)
    public static void generatedColumnBoundaryMatchesGroundAndPersistsToClient(GameTestHelper helper) {
        for (var dimension : java.util.List.of(GameplayDimensions.SNOWY, GameplayDimensions.DESERT)) {
            var level = helper.getLevel().getServer().getLevel(dimension);
            var random = level.getChunkSource().randomState();
            var generator = level.getChunkSource().getGenerator();
            int sea = generator.getSeaLevel(), x = 310_080;
            boolean snowy = dimension.equals(GameplayDimensions.SNOWY);
            int threshold = snowy ? 71 : sea - 1;
            net.minecraft.world.level.ChunkPos boundary = null;
            for (int z = 410_000; z < 412_000; z += 16) {
                int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
                for (int dz = 0; dz < 16; dz++) {
                    int floor = snowy ? net.amurdza.examplemod.worldgen.SnowyOceanFloor.floor(random, x, z + dz)
                            : (int) Math.ceil(random.router().initialDensityWithoutJaggedness().compute(
                                    new net.minecraft.world.level.levelgen.DensityFunction.SinglePointContext(x, 0, z + dz)) + .5) - 1;
                    low = Math.min(low, floor); high = Math.max(high, floor);
                }
                if (low < threshold && high >= threshold) { boundary = new net.minecraft.world.level.ChunkPos(x >> 4, z >> 4); break; }
            }
            helper.assertTrue(boundary != null, "Find mixed surface terrain in " + dimension.location());
            var chunk = level.getChunk(boundary.x, boundary.z, net.minecraft.world.level.chunk.ChunkStatus.SURFACE, true);
            var region = new net.minecraft.server.level.WorldGenRegion(level, java.util.List.of(chunk),
                    net.minecraft.world.level.chunk.ChunkStatus.SURFACE, 0);
            var nbt = new net.minecraft.nbt.CompoundTag();
            net.amurdza.examplemod.worldgen.SurfaceHabitatColumns.write(chunk, nbt);
            helper.assertTrue(nbt.getByteArray(net.amurdza.examplemod.worldgen.SurfaceHabitatColumns.TAG).length == 256,
                    "A mixed surface chunk must serialize one exact habitat per block column");
            var restored = new net.minecraft.world.level.chunk.LevelChunk(level, boundary);
            net.amurdza.examplemod.worldgen.SurfaceHabitatColumns.read(restored, nbt);
            var packet = new net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData(restored);
            var receiver = new net.minecraft.world.level.chunk.LevelChunk(level, boundary);
            receiver.replaceWithPacketData(packet.getReadBuffer(), packet.getHeightmaps(),
                    packet.getBlockEntitiesTagsConsumer(boundary.x, boundary.z));
            helper.assertTrue(java.util.Arrays.equals(net.amurdza.examplemod.worldgen.SurfaceHabitatColumns.data(receiver).aoemod$getHabitats(),
                            net.amurdza.examplemod.worldgen.SurfaceHabitatColumns.data(chunk).aoemod$getHabitats()),
                    "Chunk save/load and the complete network packet must preserve all 256 surface habitats");
            int low = 0, high = 0;
            for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
                int bx = boundary.getMinBlockX() + dx, bz = boundary.getMinBlockZ() + dz;
                int floor = chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, dx, dz);
                boolean wet = chunk.getFluidState(new BlockPos(bx, sea - 1, bz)).is(net.minecraft.tags.FluidTags.WATER);
                String expected = snowy ? floor >= 71 ? "snowy_forest" : "dappled_forest" : wet ? "desert_lake" : "desert";
                if (floor < threshold) low++; else high++;
                for (int y : new int[]{0, 28, 63, 71, 100}) {
                    helper.assertTrue(region.getBiome(new BlockPos(bx, y, bz)).is(AOEMod.makeID(expected)),
                            "All aboveground heights must match this column's generated ground: " + bx + "," + bz + " " + expected);
                }
                if (!snowy && floor < threshold) helper.assertTrue(wet, "No shoreline air below the desert lake waterline");
            }
            helper.assertTrue(low > 0 && high > 0, "Exercise real terrain on both sides of the block-height boundary");
        }
        helper.succeed();
    }

    private static final class PocketFixture {
        final Map<BlockPos, BlockState> blocks = new HashMap<>();
        final WorldGenLevel world;

        PocketFixture(GameTestHelper helper, boolean missingEast) {
            var available = helper.getLevel().getChunkAt(helper.absolutePos(BlockPos.ZERO));
            world = (WorldGenLevel) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{WorldGenLevel.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getMinBuildHeight" -> -16;
                        case "getMaxBuildHeight" -> 48;
                        case "getHeight" -> 32;
                        case "getBlockState" -> blocks.getOrDefault(args[0], Blocks.STONE.defaultBlockState());
                        case "getChunk" -> missingEast && (Integer) args[0] > 0 ? null : available;
                        case "setBlock" -> { blocks.put(((BlockPos) args[0]).immutable(), (BlockState) args[1]); yield true; }
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        void room(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
            for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++)
                for (int y = minY; y <= maxY; y++) blocks.put(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
        }
    }
}
