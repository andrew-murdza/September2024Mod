package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.PlainsFlowers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlainsFlowerTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void flowerChunksAreUniformAndForestOpportunitiesAreExactlyDoubled(GameTestHelper helper) {
        int patches = 0, forestPatches = 0, changedSeeds = 0;
        for (int chunkX = -64; chunkX < 64; chunkX++) for (int chunkZ = -64; chunkZ < 64; chunkZ++) {
            var planned = PlainsFlowers.flowerPatches(50881L, chunkX, chunkZ);
            helper.assertTrue(planned.equals(PlainsFlowers.flowerPatches(50881L, chunkX, chunkZ)),
                    "Flower patch positions must be reproducible");
            if (!planned.equals(PlainsFlowers.flowerPatches(-49835L, chunkX, chunkZ))) changedSeeds++;
            patches += planned.size();
            forestPatches += PlainsFlowers.flowerPatches(50881L, chunkX, chunkZ, true).size();
            for (var patch : planned) {
                helper.assertTrue(Math.floorDiv(patch.x(), 16) == chunkX && Math.floorDiv(patch.x() + 5, 16) == chunkX
                                && Math.floorDiv(patch.z(), 16) == chunkZ && Math.floorDiv(patch.z() + 5, 16) == chunkZ,
                        "A flower footprint crossed its owner chunk, including negative coordinates");
            }
        }
        for (int rx = -16; rx < 16; rx++) for (int rz = -16; rz < 16; rz++) {
            int ordinary = 0, forest = 0;
            for (int dx = 0; dx < 4; dx++) for (int dz = 0; dz < 4; dz++) {
                ordinary += PlainsFlowers.flowerPatches(50881L, rx * 4 + dx, rz * 4 + dz).size();
                forest += PlainsFlowers.flowerPatches(50881L, rx * 4 + dx, rz * 4 + dz, true).size();
            }
            helper.assertTrue(ordinary == 1 && forest == 2, "Every four-square region must have one ordinary flower chunk and two forest flower chunks");
        }
        helper.assertTrue(patches == 1024 && forestPatches == 2048, "Forest patches must be exactly twice as common without noise-biased empty regions");
        helper.assertTrue(changedSeeds > 600, "Flower positions should vary between world seeds");
        AOEMod.LOGGER.info("Plains flower audit: {} ordinary / {} forest patches across 16,384 uniformly stratified chunks", patches, forestPatches);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void flowerPatchesKeepVanillaSplitWithEqualTulipReplacementsAndLilies(GameTestHelper helper) {
        Map<String, Integer> totals = new HashMap<>();
        int homogeneousPatches = 0;
        int samples = 4096;
        for (int sample = 0; sample < samples; sample++) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = fixture(helper, blocks);
            int x = (sample % 64 - 32) * 64 + 2, z = (sample / 64 - 32) * 64 + 2;
            blocks.put(new BlockPos(x, 71, z), Blocks.GRASS.defaultBlockState());
            blocks.put(new BlockPos(x + 1, 71, z), Blocks.TALL_GRASS.defaultBlockState());
            blocks.put(new BlockPos(x + 1, 72, z), Blocks.TALL_GRASS.defaultBlockState()
                    .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
            blocks.put(new BlockPos(x + 2, 71, z), Blocks.PUMPKIN.defaultBlockState());
            Map<BlockPos, BlockState> before = Map.copyOf(blocks);
            var patch = new PlainsFlowers.FlowerPatch(x, z, 50881L + sample * 7919L);
            helper.assertTrue(PlainsFlowers.placePatch(world, patch) == 5, "Flowers must retain the existing five-root density");
            before.forEach((pos, state) -> helper.assertTrue(world.getBlockState(pos).equals(state),
                    "Flowers replaced existing grass or crops at " + pos));
            int roots = 0;
            Set<String> mixture = new HashSet<>();
            for (var entry : blocks.entrySet()) {
                if (entry.getKey().getY() != 71 || !PlainsFlowers.FLOWERS.contains(id(entry.getValue()))) continue;
                String species = id(entry.getValue());
                mixture.add(species);
                totals.merge(species, 1, Integer::sum);
                roots++;
                helper.assertTrue(entry.getKey().getX() >= x && entry.getKey().getX() < x + 6
                        && entry.getKey().getZ() >= z && entry.getKey().getZ() < z + 6, "Flower escaped its six-square patch");
            }
            helper.assertTrue(roots == 5, "Every successful patch must contain five roots: " + mixture);
            if (mixture.size() == 1) homogeneousPatches++;
            Map<BlockPos, BlockState> generated = Map.copyOf(blocks);
            blocks.clear();
            blocks.putAll(before);
            PlainsFlowers.placePatch(world, patch);
            helper.assertTrue(blocks.equals(generated), "The same terrain and seed must reproduce flower species and positions");
        }
        helper.assertTrue(totals.keySet().equals(Set.copyOf(PlainsFlowers.FLOWERS)),
                "Actual patches must contain only vanilla Plains' non-tulip flowers plus lilies: " + totals);
        int dandelions = totals.get("minecraft:dandelion");
        helper.assertTrue(dandelions > samples * 5 * .55 && dandelions < samples * 5 * .7,
                "Dandelions retain vanilla's two-thirds chance outside former tulip areas: " + totals);
        double companionMean = (samples * 5 - dandelions) / 5.0;
        for (String name : PlainsFlowers.FLOWERS) {
            if (name.equals("minecraft:dandelion")) continue;
            helper.assertTrue(totals.get(name) > companionMean * .85 && totals.get(name) < companionMean * 1.15,
                    "Lilies and each tulip replacement must have equal combined weight: " + totals);
        }
        helper.assertTrue(homogeneousPatches > 0,
                "Independent vanilla-weighted draws must allow homogeneous patches without forced companions");
        AOEMod.LOGGER.info("Plains flower placement: {}, {} homogeneous patches", totals, homogeneousPatches);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void blockedFlowerFootprintsLeaveNoStrays(GameTestHelper helper) {
        for (int seed = 0; seed < 32; seed++) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = fixture(helper, blocks);
            for (int x = 2; x < 8; x++) for (int z = 2; z < 8; z++) {
                blocks.put(new BlockPos(x, 71, z), Blocks.STONE.defaultBlockState());
                // A roomy cave below cannot supply the missing surface flowers.
                blocks.put(new BlockPos(x, 51, z), Blocks.AIR.defaultBlockState());
                blocks.put(new BlockPos(x, 52, z), Blocks.AIR.defaultBlockState());
            }
            for (int x = 2; x < 6; x++) blocks.remove(new BlockPos(x, 71, 2));
            var before = Map.copyOf(blocks);
            helper.assertTrue(PlainsFlowers.placePatch(world, new PlainsFlowers.FlowerPatch(2, 2, seed)) == 0,
                    "Incomplete flower patch must be rejected");
            helper.assertTrue(blocks.equals(before), "A failed flower patch must preserve all original blocks");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void freshPlainsFlowersKeepFiveRootsOnDrySurface(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        Set<ChunkPos> dryChunks = dryFlowerChunks(level);
        helper.assertTrue(dryChunks.size() == 24,
                "The seeded terrain search must find 24 dry flower owner chunks before generating chunks: " + dryChunks.size());
        Set<ChunkPos> sampledChunks = new LinkedHashSet<>(dryChunks);
        // Include unselected neighboring chunks to catch legacy scattered flowers,
        // not just the roots in our terrain-screened planned footprints.
        for (ChunkPos chunk : dryChunks) sampledChunks.add(new ChunkPos(chunk.x + 1, chunk.z));
        int flowers = 0, patches = 0;
        Set<String> species = new HashSet<>();
        for (ChunkPos chunk : sampledChunks) level.getChunk(chunk.x, chunk.z);
        for (ChunkPos chunk : sampledChunks) {
            int cx = chunk.x, cz = chunk.z;
            var forests = net.amurdza.examplemod.worldgen.structure.MansionForestFloor.forChunk(level, chunk);
            boolean forest = forests.stream().anyMatch(value -> value.contains(cx * 16 + 8, cz * 16 + 8));
            var planned = PlainsFlowers.flowerPatches(level.getSeed(), cx, cz, forest);
            Map<PlainsFlowers.FlowerPatch, Integer> counts = new HashMap<>();
            for (int x = cx * 16; x < cx * 16 + 16; x++) for (int z = cz * 16; z < cz * 16 + 16; z++) {
                int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                for (int y = surface - 2; y <= surface + 1; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    String name = id(state);
                    helper.assertFalse(name.contains("tulip"), "A legacy Plains tulip generated at " + pos);
                    helper.assertFalse(name.equals("minecraft:sunflower"), "An extra sunflower entered the vanilla Plains palette at " + pos);
                    if (!PlainsFlowers.FLOWERS.contains(name)) continue;
                    if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                            && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) continue;
                    helper.assertTrue(level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK) && y == surface,
                            "Flower generated away from the dry surface at " + pos);
                    var patch = planned.stream().filter(p -> pos.getX() >= p.x() && pos.getX() < p.x() + 6
                            && pos.getZ() >= p.z() && pos.getZ() < p.z() + 6).findFirst();
                    helper.assertTrue(patch.isPresent(), "Scattered or legacy flower at " + pos + ": " + name);
                    counts.merge(patch.orElseThrow(), 1, Integer::sum);
                    species.add(name);
                    flowers++;
                }
            }
            for (int count : counts.values()) helper.assertTrue(count == 5,
                    "Freshly generated flower patch must have five roots, got " + count);
            patches += counts.size();
        }
        helper.assertTrue(patches > 5, "Fresh Plains sample must contain flower patches");
        helper.assertTrue(species.size() >= 5 && species.contains("minecraft:dandelion"),
                "Fresh Plains must visibly mix a broad flower palette: " + species);
        AOEMod.LOGGER.info("Fresh Plains flowers: {} roots in {} patches, no tulips or sunflowers, species={}", flowers, patches, species);
        helper.succeed();
    }

    private static Set<ChunkPos> dryFlowerChunks(ServerLevel level) {
        var generator = level.getChunkSource().getGenerator();
        var random = level.getChunkSource().randomState();
        int seaLevel = generator.getSeaLevel();
        Set<ChunkPos> result = new LinkedHashSet<>();
        // At one-third frequency a 15-square region averages fewer than twelve
        // planned patches even before ocean/terrain rejection. Accumulate dry
        // owner chunks across regions without inspecting decorated blocks.
        for (int candidate = 0; candidate < 256; candidate++) {
            int minX = 6401 * 15 + candidate % 8 * 64;
            int minZ = -6407 * 15 + candidate / 8 * 64;
            Map<Long, Integer> heights = new HashMap<>();
            for (int cx = minX; cx < minX + 15; cx++) for (int cz = minZ; cz < minZ + 15; cz++) {
                for (var patch : PlainsFlowers.flowerPatches(level.getSeed(), cx, cz)) {
                    int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
                    for (int[] offset : new int[][]{{0, 0}, {5, 0}, {0, 5}, {5, 5}, {2, 2}}) {
                        int x = patch.x() + offset[0], z = patch.z() + offset[1];
                        long key = ((long) x << 32) ^ (z & 0xffffffffL);
                        int height = heights.computeIfAbsent(key, ignored -> generator.getBaseHeight(x, z,
                                Heightmap.Types.OCEAN_FLOOR_WG, level, random));
                        low = Math.min(low, height);
                        high = Math.max(high, height);
                    }
                    if (low > seaLevel + 1 && high - low <= 6) {
                        result.add(new ChunkPos(cx, cz));
                        if (result.size() == 24) return result;
                    }
                }
            }
        }
        return result;
    }

    private static String id(BlockState state) { return ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString(); }

    private static WorldGenLevel fixture(GameTestHelper helper, Map<BlockPos, BlockState> blocks) {
        var plains = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        var biome = plains.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod:plains")));
        java.util.function.Function<BlockPos, BlockState> stateAt = pos -> blocks.getOrDefault(pos,
                pos.getY() <= 70 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState());
        return (WorldGenLevel) Proxy.newProxyInstance(PlainsFlowerTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getLevel": return plains;
                        case "getBiome": return biome;
                        case "getHeight": return args == null || args.length == 0 ? plains.getHeight() : 71;
                        case "getBlockState": return stateAt.apply((BlockPos) args[0]);
                        case "getBlockEntity": return null;
                        case "getFluidState": return stateAt.apply((BlockPos) args[0]).getFluidState();
                        case "isStateAtPosition":
                            return ((java.util.function.Predicate<BlockState>) args[1]).test(stateAt.apply((BlockPos) args[0]));
                        case "isFluidAtPosition":
                            return ((java.util.function.Predicate<net.minecraft.world.level.material.FluidState>) args[1])
                                    .test(stateAt.apply((BlockPos) args[0]).getFluidState());
                        case "isEmptyBlock": return stateAt.apply((BlockPos) args[0]).isAir();
                        case "getRawBrightness", "getMaxLocalRawBrightness": return 15;
                        case "canSeeSky": return true;
                        case "ensureCanWrite": return true;
                        case "setBlock":
                            blocks.put(((BlockPos) args[0]).immutable(), (BlockState) args[1]);
                            return true;
                        default:
                            try { return method.invoke(plains, args); }
                            catch (InvocationTargetException failure) { throw failure.getCause(); }
                    }
                });
    }
}
