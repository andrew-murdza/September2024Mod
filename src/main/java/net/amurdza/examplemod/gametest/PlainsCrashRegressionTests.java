package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.GrowthPlacementBuffer;
import net.amurdza.examplemod.worldgen.PlainsSurfaceWater;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/** Exercises the last Plains region recorded before the November 2 session ended abruptly. */
@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlainsCrashRegressionTests {
    // The current save may have been recreated after the abrupt session.
    private static final long CURRENT_WORLD_SEED = 6896527134938031266L;
    private static final int CENTER_CHUNK_X = 0;
    private static final int CENTER_CHUNK_Z = -3;

    private PlainsCrashRegressionTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void generatedBeeNestPersistsOccupantsWithoutLiveWorldCallbacks(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = new BlockPos(7, 80, 7);
        var chunk = new ProtoChunk(new ChunkPos(pos), UpgradeData.EMPTY, level,
                level.registryAccess().registryOrThrow(Registries.BIOME), null);
        BlockState[] state = {Blocks.AIR.defaultBlockState()};
        BeehiveBlockEntity[] placed = {null};
        WorldGenLevel world = (WorldGenLevel) Proxy.newProxyInstance(PlainsCrashRegressionTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getLevel": return level;
                        case "dimensionType": return level.dimensionType();
                        case "getBlockTicks": return level.getBlockTicks();
                        case "getFluidTicks": return level.getFluidTicks();
                        case "ensureCanWrite": return true;
                        case "isOutsideBuildHeight": return false;
                        case "getBlockState": return state[0];
                        case "getBlockEntity": return placed[0];
                        case "getChunk": {
                            helper.assertTrue(args.length == 1 && pos.equals(args[0]),
                                    "Bee-nest commit must only access its existing generation chunk");
                            return chunk;
                        }
                        case "setBlock": {
                            state[0] = (BlockState) args[1];
                            placed[0] = new BeehiveBlockEntity(pos, state[0]) {
                                @Override
                                public void setChanged() {
                                    throw new AssertionError("World generation invoked a live beehive callback that can load chunks");
                                }
                            };
                            placed[0].setLevel(level);
                            return true;
                        }
                        default: throw new AssertionError("Unexpected bee-nest generation access: " + method.getName());
                    }
                });
        var outer = new GrowthPlacementBuffer(world);
        var inner = new GrowthPlacementBuffer(outer);
        inner.setBlock(pos, Blocks.BEE_NEST.defaultBlockState(), 19);
        var staged = (BeehiveBlockEntity) inner.getBlockEntity(pos);
        CompoundTag bee = new CompoundTag();
        bee.putString("id", "minecraft:bee");
        staged.storeBee(bee, 123, false);
        staged.storeBee(bee.copy(), 456, true);
        CompoundTag expected = staged.saveWithFullMetadata();
        chunk.setUnsaved(false);
        helper.assertTrue(inner.commit(), "Inner bee-nest transaction failed");
        helper.assertTrue(state[0].isAir() && placed[0] == null && !chunk.isUnsaved(),
                "A nested bee-nest commit must remain staged");
        helper.assertTrue(outer.commit(), "Worldgen bee-nest transaction failed");
        helper.assertTrue(placed[0].getOccupantCount() == 2
                        && expected.equals(placed[0].saveWithFullMetadata()),
                "Worldgen commit lost bee occupant data");
        helper.assertTrue(chunk.isUnsaved(), "Worldgen bee occupant data must be marked for saving");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void liveBeeNestCommitRetainsChangedCallback(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockState state = Blocks.BEE_NEST.defaultBlockState();
        level.setBlockAndUpdate(pos, state);
        int[] callbacks = {0};
        var hive = new BeehiveBlockEntity(pos, state) {
            @Override
            public void setChanged() {
                callbacks[0]++;
                super.setChanged();
            }
        };
        level.setBlockEntity(hive);
        var buffer = new GrowthPlacementBuffer(level);
        buffer.setBlock(pos, state, 19);
        var staged = (BeehiveBlockEntity) buffer.getBlockEntity(pos);
        CompoundTag bee = new CompoundTag();
        bee.putString("id", "minecraft:bee");
        staged.storeBee(bee, 123, false);
        helper.assertTrue(buffer.commit(), "Live bee-nest transaction failed");
        helper.assertTrue(callbacks[0] == 1 && hive.getOccupantCount() == 1,
                "Live tree growth must retain the bee-nest changed callback and occupant data");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void currentWorldSeedOriginRegionSamplesTerrainAndLakeBiomes(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        var random = RandomState.create(generator.generatorSettings().value(),
                level.registryAccess().lookupOrThrow(Registries.NOISE), CURRENT_WORLD_SEED);
        int sampled = 0;
        for (int chunkX = CENTER_CHUNK_X - 2; chunkX <= CENTER_CHUNK_X + 2; chunkX++) {
            for (int chunkZ = CENTER_CHUNK_Z - 2; chunkZ <= CENTER_CHUNK_Z + 2; chunkZ++) {
                for (int dx = 0; dx < 16; dx += 4) for (int dz = 0; dz < 16; dz += 4) {
                    int x = chunkX * 16 + dx, z = chunkZ * 16 + dz;
                    var column = generator.getBaseColumn(x, z, level, random);
                    for (int y : new int[]{-48, 0, 32, 62, 96}) {
                        helper.assertTrue(column.getBlock(y) != null, "Missing sampled terrain at " + x + "," + y + "," + z);
                        helper.assertTrue(Double.isFinite(random.router().finalDensity().compute(
                                        new DensityFunction.SinglePointContext(x, y, z))),
                                "Invalid Plains density at " + x + "," + y + "," + z);
                        generator.getBiomeSource().getNoiseBiome(x >> 2, y >> 2, z >> 2, random.sampler());
                    }
                    sampled++;
                }
            }
        }
        AOEMod.LOGGER.info("Plains current-save seed {}: sampled {} columns near the last logged Plains visit at 0,96,-38",
                CURRENT_WORLD_SEED, sampled);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void originRegionCompletesDecorationAndChunkPostProcessing(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        // This uses the test server's own seed and checks the full generation pipeline;
        // the crashed world's exact seed and final player position are not available.
        AOEMod.LOGGER.info("Plains origin-region full generation starts with test seed {}", level.getSeed());
        int count = 0;
        for (int x = CENTER_CHUNK_X - 4; x <= CENTER_CHUNK_X + 4; x++) {
            for (int z = CENTER_CHUNK_Z - 4; z <= CENTER_CHUNK_Z + 4; z++) {
                final ChunkPos position = new ChunkPos(x, z);
                helper.runAtTickTime(++count, () -> {
                    AOEMod.LOGGER.info("Plains origin-region loading full chunk {}", position);
                    var chunk = level.getChunk(position.x, position.z);
                    helper.assertTrue(chunk.getStatus().isOrAfter(ChunkStatus.FULL),
                            "Plains chunk did not finish decoration at " + position);
                    chunk.postProcessGeneration();
                    for (var entry : chunk.getBlockEntities().entrySet()) {
                        helper.assertTrue(chunk.getBlockState(entry.getKey()).hasBlockEntity(),
                                "Orphan block entity after Plains chunk generation at " + entry.getKey());
                    }
                });
            }
        }
        helper.runAtTickTime(count + 20, helper::succeed);
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void largeCoveredAquiferDrainsWithoutRequestingGeneration(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        Map<Long, ChunkAccess> chunks = new HashMap<>();
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
            var chunk = new ProtoChunk(new ChunkPos(x, z), UpgradeData.EMPTY, level,
                    level.registryAccess().registryOrThrow(Registries.BIOME), null);
            chunk.setStatus(x == 0 && z == 0 ? ChunkStatus.CARVERS : ChunkStatus.FEATURES);
            chunks.put(ChunkPos.asLong(x, z), chunk);
        }
        Map<BlockPos, BlockState> changes = new HashMap<>();
        long[] reads = new long[3];
        java.util.function.Function<BlockPos, BlockState> state = pos -> {
            if (pos.getX() < -64 || pos.getX() >= 80 || pos.getZ() < -64 || pos.getZ() >= 80) {
                throw new AssertionError("Drainage read outside the available region at " + pos);
            }
            return changes.getOrDefault(pos, pos.getY() >= 20 && pos.getY() < 40
                    ? Blocks.WATER.defaultBlockState() : Blocks.STONE.defaultBlockState());
        };
        WorldGenLevel world = (WorldGenLevel) Proxy.newProxyInstance(PlainsCrashRegressionTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getLevel": return level;
                        case "getMinBuildHeight": return level.getMinBuildHeight();
                        case "getHeight": return args == null || args.length == 0 ? level.getHeight() : 64;
                        case "hasChunk": return chunks.containsKey(ChunkPos.asLong((int) args[0], (int) args[1]));
                        case "getChunk": {
                            reads[0]++;
                            int x = args[0] instanceof BlockPos pos ? pos.getX() >> 4 : (int) args[0];
                            int z = args[0] instanceof BlockPos pos ? pos.getZ() >> 4 : (int) args[1];
                            var chunk = chunks.get(ChunkPos.asLong(x, z));
                            var required = args.length >= 3 ? (ChunkStatus) args[2] : ChunkStatus.EMPTY;
                            if (chunk != null && chunk.getStatus().isOrAfter(required)) return chunk;
                            if (args.length == 4 && !(boolean) args[3]) return null;
                            throw new AssertionError("Drainage attempted to load or generate unavailable chunk " + x + "," + z);
                        }
                        case "getBlockState": reads[1]++; return state.apply((BlockPos) args[0]);
                        case "getFluidState": reads[2]++; return state.apply((BlockPos) args[0]).getFluidState();
                        case "setBlock": {
                            BlockPos pos = ((BlockPos) args[0]).immutable();
                            helper.assertTrue(pos.getX() >= 0 && pos.getX() < 16 && pos.getZ() >= 0 && pos.getZ() < 16,
                                    "Drainage changed a neighboring chunk at " + pos);
                            changes.put(pos, (BlockState) args[1]);
                            return true;
                        }
                        default: throw new AssertionError("Unexpected world access during aquifer drainage: " + method.getName());
                    }
                });
        long start = System.nanoTime();
        int drained = PlainsSurfaceWater.drainDisconnected(world, 0, 0);
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
        helper.assertTrue(drained == 16 * 16 * 20 && changes.size() == drained,
                "A sealed 414720-cell aquifer must drain exactly the 5120 cells belonging to this chunk");
        AOEMod.LOGGER.info("Plains covered-aquifer stress: 414720 connected cells, {} ms, {} chunk reads, {} block reads, {} fluid reads",
                elapsedMillis, reads[0], reads[1], reads[2]);
        helper.succeed();
    }
}
