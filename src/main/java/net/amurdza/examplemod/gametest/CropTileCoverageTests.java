package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.CropTiles;
import net.amurdza.examplemod.worldgen.CropTerrainHeights;
import net.amurdza.examplemod.worldgen.SavannaFlowerBands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CropTileCoverageTests {
    private static final long SEED = 50881;

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void dryTilesHaveCompleteDistinctPalettesAndSeededLayouts(GameTestHelper helper) {
        helper.assertTrue(CropTiles.TILE_SIZE == 288 && CropTiles.SAVANNA_TILE_SIZE == 288 && CropTiles.CELL_SIZE == 8
                        && CropTiles.PLANTS_PER_PATCH == 5 && CropTiles.plantsPerPatch("minecraft:pumpkin") == 2
                        && CropTiles.plantsPerPatch("farmersdelight:wild_carrots") == 5
                        && CropTiles.plantsPerPatch("minecraft:red_mushroom") == 10,
                "Crop coverage uses 288 tiles, adjacent crop chunks, two pumpkins, five-root crops and ten-root mushrooms");
        helper.assertTrue(Set.copyOf(CropTiles.PLAINS_SPECIES).equals(Set.of("farmersdelight:wild_carrots",
                        "farmersdelight:wild_cabbages", "minecraft:pumpkin")), "Wrong Plains wild-crop palette");
        helper.assertTrue(Set.copyOf(CropTiles.SAVANNA_SPECIES).equals(Set.of("farmersdelight:wild_carrots",
                        "farmersdelight:wild_cabbages", "minecraft:pumpkin", "minecraft:red_mushroom", "minecraft:brown_mushroom")),
                "Wrong Savanna wild-crop and mushroom palette");
        for (var palette : List.of(CropTiles.PLAINS_SPECIES, CropTiles.SAVANNA_SPECIES)) {
            for (long seed : new long[]{0, SEED, -991733}) for (int tx = -2; tx < 2; tx++) for (int tz = -2; tz < 2; tz++) {
                var patches = CropTiles.plan(seed, tx, tz, palette, (x, z) -> true);
                assertPalette(helper, patches, palette);
                helper.assertTrue(patches.equals(CropTiles.plan(seed, tx, tz, palette, (x, z) -> true)), "Plan is not reproducible");
                Set<Long> cells = new HashSet<>();
                for (var patch : patches) {
                    assertBounds(helper, patch, tx, tz, 288);
                    helper.assertTrue(cells.add(cell(patch.x(), patch.z())), "Two species claimed one eight-block cell");
                }
            }
            var baseline = CropTiles.plan(SEED, 0, 0, palette, (x, z) -> true);
            helper.assertTrue(!Set.copyOf(baseline).equals(Set.copyOf(CropTiles.plan(SEED + 1, 0, 0, palette, (x, z) -> true))),
                    "Changing seed must change crop locations");
            var adjacent = CropTiles.plan(SEED, 1, 0, palette, (x, z) -> true).stream()
                    .map(p -> new CropTiles.Patch(p.x() - (288), p.z(), p.species())).collect(Collectors.toSet());
            helper.assertTrue(!Set.copyOf(baseline).equals(adjacent), "Neighboring tiles must not repeat a translated layout");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void arbitraryOffset576PlainsSquaresContainEverySpeciesWhenLandIsSuitable(GameTestHelper helper) {
        for (var palette : List.of(CropTiles.PLAINS_SPECIES)) {
            List<CropTiles.Patch> all = new ArrayList<>();
            for (int tx = -4; tx <= 4; tx++) for (int tz = -4; tz <= 4; tz++)
                all.addAll(CropTiles.plan(SEED, tx, tz, palette, (x, z) -> true));
            // Both positive/negative corners and positions immediately around tile/chunk seams.
            for (int x : new int[]{-288, -287, -121, -120, -17, -1, 0, 1, 15, 119, 120, 121, 287, 288}) {
                for (int z : new int[]{-288, -287, -121, -120, -17, -1, 0, 1, 15, 119, 120, 121, 287, 288}) {
                    int fullTileX = Math.floorDiv(x + 287, 288), fullTileZ = Math.floorDiv(z + 287, 288);
                    helper.assertTrue(fullTileX * 288 >= x && (fullTileX + 1) * 288 <= x + 576
                                    && fullTileZ * 288 >= z && (fullTileZ + 1) * 288 <= z + 576,
                            "Every offset 576 square must contain a full aligned 288 tile");
                    for (String species : palette) {
                        long count = all.stream().filter(p -> p.species().equals(species) && p.x() - 2 >= x
                                && p.x() + 3 < x + 576 && p.z() - 2 >= z && p.z() + 3 < z + 576).count();
                        helper.assertTrue(count >= 3, "Offset 576 square lost a complete " + species + " patch at " + x + "," + z);
                        if (Math.floorMod(x, 288) == 0 && Math.floorMod(z, 288) == 0)
                            helper.assertTrue(count == 12, "Four aligned 288 tiles must contain exactly twelve patches per species");
                    }
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void smallDryIslandsKeepMushroomsButCannotFragmentCropClusters(GameTestHelper helper) {
        for (int origin : new int[]{-288, 0, 288}) {
            int tile = Math.floorDiv(origin, 288);
            var plains = CropTiles.plan(SEED, tile, tile, CropTiles.PLAINS_SPECIES,
                    (x, z) -> x >= origin && x < origin + 16 && z >= origin && z < origin + 16);
            helper.assertTrue(plains.isEmpty(), "A single dry chunk cannot fit a 3x3 crop cluster");
            var savanna = CropTiles.plan(SEED, tile, tile, CropTiles.SAVANNA_SPECIES,
                    (x, z) -> x >= origin && x < origin + 24 && z >= origin && z < origin + 16);
            helper.assertTrue(savanna.size() == 2 && savanna.stream().allMatch(p -> p.species().endsWith("_mushroom")),
                    "Small islands retain mushrooms without scattering the nine crop chunks");
        }
        int[] queries = {0};
        var wet = CropTiles.plan(SEED, -1, 1, CropTiles.SAVANNA_SPECIES, (x, z) -> { queries[0]++; return false; });
        helper.assertTrue(wet.isEmpty() && queries[0] > 0 && queries[0] <= 288 * 288,
                "All-water tile must omit crops and query each column at most once: " + queries[0]);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void signedChunkOwnershipAndReverseDecorationProduceIdenticalBlocks(GameTestHelper helper) {
        for (long seed : new long[]{SEED, -1234}) {
            Fixture fixture = new Fixture(helper, seed, false);
            Set<CropTiles.Patch> owned = new HashSet<>();
            for (int cx = -18; cx < 18; cx++) for (int cz = -18; cz < 18; cz++) {
                for (var patch : CropTiles.forChunk(seed, cx, cz, CropTiles.PLAINS_SPECIES, (x, z) -> true)) {
                    helper.assertTrue(Math.floorDiv(patch.x(), 16) == cx && Math.floorDiv(patch.z(), 16) == cz
                                    && owned.add(patch), "Chunk seam duplicated or reassigned a patch");
                    helper.assertTrue(CropTiles.place(fixture.world, patch) == CropTiles.plantsPerPatch(patch.species()),
                            "Dry owner chunk must place its complete species quota");
                }
            }
            helper.assertTrue(owned.size() == 4 * 9, "A 576 square spanning signed seams must own four full nine-chunk crop tiles");
            Map<BlockPos, BlockState> forward = Map.copyOf(fixture.blocks);
            fixture.blocks.clear();
            for (int cx = 17; cx >= -18; cx--) for (int cz = 17; cz >= -18; cz--) {
                var patches = CropTiles.forChunk(seed, cx, cz, CropTiles.PLAINS_SPECIES, (x, z) -> true);
                for (int i = patches.size() - 1; i >= 0; i--) CropTiles.place(fixture.world, patches.get(i));
            }
            helper.assertTrue(fixture.blocks.equals(forward), "Changing chunk/patch generation order changed crop states or locations");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void allSpeciesReplaceGrassAndRetriesRemainInsideTheirOwnCell(GameTestHelper helper) {
        int[] dryCell = savannaCellOutsideFlowers();
        for (String species : CropTiles.SAVANNA_SPECIES) {
            Fixture fixture = new Fixture(helper, SEED, true);
            fixture.fillGrass(dryCell[0], dryCell[1]);
            var before = Map.copyOf(fixture.blocks);
            helper.assertTrue(CropTiles.place(fixture.world, new CropTiles.Patch(dryCell[0] + 2, dryCell[1] + 2, species)) == CropTiles.plantsPerPatch(species),
                    "Grass-covered cell must produce a full " + species + " patch");
            assertRootsAndPreservedNeighbors(helper, fixture, before, dryCell[0], dryCell[1], species);
        }
        for (int seam : new int[]{-120, 120}) {
            Fixture fixture = new Fixture(helper, SEED, false);
            int min = seam - 8;
            fixture.fillGrass(min, min);
            for (int x = min; x < min + 6; x++) for (int z = min; z < min + 6; z++) {
                fixture.blocks.put(new BlockPos(x, 71, z), Blocks.OAK_LOG.defaultBlockState());
                fixture.blocks.put(new BlockPos(x, 72, z), Blocks.OAK_LOG.defaultBlockState());
            }
            var before = Map.copyOf(fixture.blocks);
            helper.assertTrue(CropTiles.place(fixture.world, new CropTiles.Patch(min + 2, min + 2, "minecraft:pumpkin")) == 2,
                    "A fully blocked first footprint must retry other anchors inside its own eight-block cell");
            assertRootsAndPreservedNeighbors(helper, fixture, before, min, min, "minecraft:pumpkin");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void failedPatchesRestoreGrassHalvesAndCannotBorrowNeighboringCells(GameTestHelper helper) {
        Fixture fixture = new Fixture(helper, SEED, false);
        for (int x : new int[]{0, 8}) for (int z : new int[]{0, 8}) fixture.fillGrass(x, z);
        var before = Map.copyOf(fixture.blocks);
        int[] attempted = {0};
        fixture.writes = (pos, state) -> {
            if (state.is(Blocks.PUMPKIN) && ++attempted[0] >= 2) {
                fixture.accept(pos, state); // Simulate an interceptor that mutates before rejecting.
                fixture.accept(pos.above(), Blocks.AIR.defaultBlockState());
                return false;
            }
            return fixture.accept(pos, state);
        };
        helper.assertTrue(CropTiles.place(fixture.world, new CropTiles.Patch(2, 2, "minecraft:pumpkin")) == 0
                        && attempted[0] >= 2, "Write failures must reject a partially committed two-pumpkin patch");
        helper.assertTrue(fixture.blocks.equals(before), "Failed patch deleted original grass halves or left partial crops");
        fixture.writes = fixture::accept;
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            if (x == 0 && z == 0) continue;
            fixture.blocks.put(new BlockPos(x, 71, z), Blocks.STONE.defaultBlockState());
        }
        before = Map.copyOf(fixture.blocks);
        helper.assertTrue(CropTiles.place(fixture.world, new CropTiles.Patch(2, 2, "minecraft:pumpkin")) == 0,
                "One suitable chunk column cannot borrow the neighboring chunk's empty ground");
        helper.assertTrue(fixture.blocks.equals(before), "Insufficient local sites must leave all blocks unchanged");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void blockedCropCellRetriesWithinOwnerChunkWithoutStealingOtherReservations(GameTestHelper helper) {
        Fixture fixture = new Fixture(helper, SEED, false);
        var pumpkin = new CropTiles.Patch(2, 2, "minecraft:pumpkin");
        var carrots = new CropTiles.Patch(10, 2, "farmersdelight:wild_carrots");
        var owners = List.of(pumpkin, carrots);
        for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) {
            fixture.blocks.put(new BlockPos(x, 71, z), Blocks.OAK_LOG.defaultBlockState());
            fixture.blocks.put(new BlockPos(x, 72, z), Blocks.OAK_LOG.defaultBlockState());
        }
        helper.assertTrue(CropTiles.place(fixture.world, pumpkin, owners) == 2,
                "A fully obstructed crop cell must retry another free cell in its assigned chunk");
        var roots = fixture.blocks.entrySet().stream().filter(e -> e.getValue().is(Blocks.PUMPKIN))
                .map(Map.Entry::getKey).toList();
        helper.assertTrue(roots.size() == 2 && roots.stream().allMatch(p -> p.getX() >= 0 && p.getX() < 16
                        && p.getZ() >= 8 && p.getZ() < 16),
                "The retry must remain inside its chunk and leave the other crop's reserved cell free");
        helper.assertTrue(CropTiles.place(fixture.world, carrots, owners) == 5,
                "Relocating a crop must preserve the second species reservation");
        Map<BlockPos, BlockState> first = Map.copyOf(fixture.blocks);
        helper.assertTrue(CropTiles.place(fixture.world, pumpkin, owners) == 0
                        && CropTiles.place(fixture.world, carrots, owners) == 0 && fixture.blocks.equals(first),
                "Repeated decoration must not create a second patch after a fallback moved the first");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void blockedMushroomCellStillPlacesExactlyTenWithoutCrossingChunks(GameTestHelper helper) {
        var bands = new SavannaFlowerBands(SEED);
        int minX = 0, minZ = 0;
        boolean found = false;
        search: for (int x = -256; x < 256; x += 16) for (int z = -256; z < 256; z += 16) {
            boolean clear = true;
            for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) clear &= bands.colour(x + dx, z + dz) < 0;
            if (clear) { minX = x; minZ = z; found = true; break search; }
        }
        helper.assertTrue(found, "Mushroom obstruction fixture requires a flower-free chunk");
        Fixture fixture = new Fixture(helper, SEED, true);
        var red = new CropTiles.Patch(minX + 2, minZ + 2, "minecraft:red_mushroom");
        var brown = new CropTiles.Patch(minX + 10, minZ + 2, "minecraft:brown_mushroom");
        var owners = List.of(red, brown);
        for (int x = minX; x < minX + 8; x++) for (int z = minZ; z < minZ + 8; z++) {
            fixture.blocks.put(new BlockPos(x, 71, z), Blocks.OAK_LOG.defaultBlockState());
        }
        helper.assertTrue(CropTiles.place(fixture.world, red, owners) == 10
                        && CropTiles.place(fixture.world, brown, owners) == 10,
                "Blocked mushroom cells must retain both ten-root quotas using free cells in the owner chunk");
        helper.assertTrue(fixture.blocks.entrySet().stream().filter(e -> e.getValue().is(Blocks.RED_MUSHROOM)
                        || e.getValue().is(Blocks.BROWN_MUSHROOM)).allMatch(e ->
                        Math.floorDiv(e.getKey().getX(), 16) == Math.floorDiv(red.x(), 16)
                                && Math.floorDiv(e.getKey().getZ(), 16) == Math.floorDiv(red.z(), 16)),
                "Mushroom retries must not create additional clusters in another chunk");
        Map<BlockPos, BlockState> first = Map.copyOf(fixture.blocks);
        helper.assertTrue(CropTiles.place(fixture.world, red, owners) == 0 && fixture.blocks.equals(first),
                "A moved mushroom patch must not be duplicated");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void freshSavannaDecorationCompletesEverySpeciesAndPreservesFlowerBands(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        int tileX = 8501, tileZ = -8513;
        List<CropTiles.Patch> patches = List.of();
        Set<ChunkPos> chunks = Set.of();
        var bands = new SavannaFlowerBands(level.getSeed());
        boolean exercisesFlowers = false;
        // Select by undecorated terrain only; do not hide failed generation by
        // looking for a tile whose final crops already happen to pass the audit.
        for (int attempt = 0; attempt < 8; attempt++) {
            tileX = 8501 + attempt;
            patches = CropTiles.plan(level, tileX, tileZ, true);
            if (patches.size() != 11) continue;
            chunks = ownerChunksAndNeighbors(patches);
            exercisesFlowers = hasDryFlowerSites(level, chunks, bands);
            if (exercisesFlowers) break;
        }
        assertPalette(helper, patches, CropTiles.SAVANNA_SPECIES);
        helper.assertTrue(exercisesFlowers, "The bounded fresh sample must contain dry reserved flower sites");
        Map<Long, CropTiles.Patch> owners = new HashMap<>();
        for (var patch : patches) {
            owners.put(cell(patch.x(), patch.z()), patch);
        }
        helper.assertTrue(chunks.size() <= 99, "Runtime audit must remain bounded to eleven owner chunks and their neighbors");
        for (ChunkPos chunk : chunks) level.getChunk(chunk.x, chunk.z);

        Set<String> forbidden = Set.of("farmersdelight:wild_potatoes", "farmersdelight:wild_beetroots", "vintagedelight:wild_oats");
        Map<Block, String> names = new HashMap<>();
        Map<Long, Set<BlockPos>> roots = new HashMap<>();
        int flowers = 0;
        for (ChunkPos chunk : chunks) {
            for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
                for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                    int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    for (int y = level.getMinBuildHeight() + 1; y <= surface + 1; y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = level.getBlockState(pos);
                        if (state.isAir()) continue;
                        String id = names.computeIfAbsent(state.getBlock(), b -> ForgeRegistries.BLOCKS.getKey(b).toString());
                        helper.assertTrue(!forbidden.contains(id), "A seed-only species still generated wild in Savanna: " + id + " at " + pos);
                        if (SavannaFlowerBands.TULIPS.contains(id)) {
                            helper.assertTrue(bands.hasFlower(x, z) && id.equals(bands.tulip(x, z)),
                                    "Generated tulip escaped its reserved stripe/color at " + pos);
                            flowers++;
                        }
                        if (!CropTiles.SAVANNA_SPECIES.contains(id) || y < surface - 1
                                || !level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)) continue;
                        helper.assertTrue(bands.colour(x, z) < 0, "A crop occupied a reserved flower band at " + pos);
                        int ownerChunkX = Math.floorDiv(x, 16), ownerChunkZ = Math.floorDiv(z, 16);
                        var owner = owners.values().stream().filter(p -> p.species().equals(id)
                                && Math.floorDiv(p.x(), 16) == ownerChunkX && Math.floorDiv(p.z(), 16) == ownerChunkZ)
                                .findFirst().orElse(null);
                        if (owner == null) continue; // Other tiles may decorate the neighboring audit chunks.
                        helper.assertTrue(owner.species().equals(id), "A reserved eight-block cell contains the wrong crop at " + pos);
                        roots.computeIfAbsent(cell(owner.x(), owner.z()), ignored -> new HashSet<>()).add(pos);
                    }
                }
            }
        }
        for (var entry : owners.entrySet()) {
            Set<BlockPos> planted = roots.getOrDefault(entry.getKey(), Set.of());
            helper.assertTrue(planted.size() == CropTiles.plantsPerPatch(entry.getValue().species()), "Fresh Savanna lost a complete patch: " + entry.getValue() + ", actual=" + planted);
            helper.assertTrue(planted.stream().mapToInt(BlockPos::getX).max().orElseThrow()
                            - planted.stream().mapToInt(BlockPos::getX).min().orElseThrow() < 6
                            && planted.stream().mapToInt(BlockPos::getZ).max().orElseThrow()
                            - planted.stream().mapToInt(BlockPos::getZ).min().orElseThrow() < 6,
                    "Generated crop roots escaped one compact six-square footprint");
        }
        helper.assertTrue(flowers > 0, "The fresh audit must exercise living flower bands as well as crop-free bands");
        AOEMod.LOGGER.info("Fresh Savanna 288 tile ({},{}): eleven full species patches, {} tulips in {} owner/neighbor chunks",
                tileX, tileZ, flowers, chunks.size());
        helper.succeed();
    }

    private static Set<ChunkPos> ownerChunksAndNeighbors(List<CropTiles.Patch> patches) {
        Set<ChunkPos> chunks = new LinkedHashSet<>();
        for (var patch : patches) {
            int cx = Math.floorDiv(patch.x(), 16), cz = Math.floorDiv(patch.z(), 16);
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) chunks.add(new ChunkPos(cx + dx, cz + dz));
        }
        return chunks;
    }

    private static boolean hasDryFlowerSites(net.minecraft.server.level.ServerLevel level, Set<ChunkPos> chunks,
                                            SavannaFlowerBands bands) {
        var source = level.getChunkSource();
        var generator = source.getGenerator();
        var heights = new CropTerrainHeights(level, (NoiseBasedChunkGenerator) generator, source.randomState());
        int sites = 0;
        for (ChunkPos chunk : chunks) for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                if (bands.hasFlower(x, z) && heights.height(x, z) >= generator.getSeaLevel() && ++sites == 12) return true;
            }
        }
        return false;
    }

    private static void assertPalette(GameTestHelper helper, List<CropTiles.Patch> patches, List<String> palette) {
        helper.assertTrue(patches.size() == (palette.equals(CropTiles.SAVANNA_SPECIES) ? 11 : 9)
                        && patches.stream().map(CropTiles.Patch::species).collect(Collectors.toSet()).equals(Set.copyOf(palette)),
                "Tile must have its complete clustered crop and mushroom quotas: " + patches);
        for (String species : palette) {
            long expected = palette.equals(CropTiles.SAVANNA_SPECIES) ? species.endsWith("_mushroom") ? 1 : 3 : 3;
            helper.assertTrue(patches.stream().filter(p -> p.species().equals(species)).count() == expected,
                    "Wrong tile quota for " + species);

        }
    }

    private static void assertBounds(GameTestHelper helper, CropTiles.Patch patch, int tx, int tz, int size) {
        helper.assertTrue(patch.x() - 2 >= tx * size && patch.x() + 3 < (tx + 1) * size
                        && patch.z() - 2 >= tz * size && patch.z() + 3 < (tz + 1) * size,
                "Patch escaped its signed tile");
        helper.assertTrue(cell(patch.x() - 2, patch.z() - 2) == cell(patch.x() + 3, patch.z() + 3),
                "A six-square footprint must fit inside one independent eight-square cell");
    }

    private static long cell(int x, int z) { return ChunkPos.asLong(Math.floorDiv(x, 8), Math.floorDiv(z, 8)); }

    private static int[] savannaCellOutsideFlowers() {
        var bands = new SavannaFlowerBands(SEED);
        for (int x = -128; x < 128; x += 8) for (int z = -128; z < 128; z += 8) {
            boolean clear = true;
            for (int dx = 0; dx < 8; dx++) for (int dz = 0; dz < 8; dz++) clear &= bands.colour(x + dx, z + dz) < 0;
            if (clear) return new int[]{x, z};
        }
        throw new IllegalStateException("No suitable Savanna test cell");
    }

    private static void assertRootsAndPreservedNeighbors(GameTestHelper helper, Fixture fixture,
            Map<BlockPos, BlockState> before, int minX, int minZ, String species) {
        Set<BlockPos> roots = fixture.blocks.entrySet().stream().filter(e -> e.getKey().getY() == 71
                        && ForgeRegistries.BLOCKS.getKey(e.getValue().getBlock()).toString().equals(species))
                .map(Map.Entry::getKey).collect(Collectors.toSet());
        helper.assertTrue(roots.size() == CropTiles.plantsPerPatch(species) && roots.stream().allMatch(p -> p.getX() >= minX && p.getX() < minX + 8
                        && p.getZ() >= minZ && p.getZ() < minZ + 8), "Crop retry leaked outside its own cell");
        helper.assertTrue(roots.stream().mapToInt(BlockPos::getX).max().orElseThrow() - roots.stream().mapToInt(BlockPos::getX).min().orElseThrow() < 6
                        && roots.stream().mapToInt(BlockPos::getZ).max().orElseThrow() - roots.stream().mapToInt(BlockPos::getZ).min().orElseThrow() < 6,
                "A complete patch must retain a compact six-square footprint");
        for (BlockPos root : roots) helper.assertTrue(fixture.state(root.above()).isAir(), "Crop replacement left an orphan tall-grass half");
        before.forEach((pos, state) -> {
            if (!roots.contains(pos) && !roots.contains(pos.below()))
                helper.assertTrue(fixture.state(pos).equals(state), "An unrelated plant or protected obstacle changed at " + pos);
        });
    }

    /** Detached block/light storage exercises real survival and transaction code without changing a server world. */
    private static final class Fixture {
        final Map<BlockPos, BlockState> blocks = new HashMap<>();
        final WorldGenLevel world;
        BiPredicate<BlockPos, BlockState> writes = this::accept;

        Fixture(GameTestHelper helper, long seed, boolean savanna) {
            var level = helper.getLevel().getServer().getLevel(savanna ? GameplayDimensions.SAVANNA : GameplayDimensions.PLAINS);
            var biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
            var biome = biomes.getHolderOrThrow(ResourceKey.create(Registries.BIOME,
                    new ResourceLocation(savanna ? "aoemod:savanna" : "aoemod:plains")));
            Map<Long, ProtoChunk> chunks = new HashMap<>();
            java.util.function.BiFunction<Integer, Integer, ProtoChunk> chunkAt = (x, z) -> chunks.computeIfAbsent(ChunkPos.asLong(x, z), ignored ->
                    new ProtoChunk(new ChunkPos(x, z), UpgradeData.EMPTY, level, biomes, null) {
                        @Override public BlockState getBlockState(BlockPos pos) { return state(pos); }
                        @Override public BlockState setBlockState(BlockPos pos, BlockState next, boolean moving) {
                            BlockState previous = state(pos); accept(pos, next); return previous;
                        }
                    });
            var light = new LevelLightEngine(new LightChunkGetter() {
                @Override public LightChunk getChunkForLighting(int x, int z) { return chunkAt.apply(x, z); }
                @Override public BlockGetter getLevel() { return level; }
            }, false, false);
            world = (WorldGenLevel) Proxy.newProxyInstance(CropTileCoverageTests.class.getClassLoader(),
                    new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "getLevel": return level;
                            case "getSeed": return seed;
                            case "getBiome": return biome;
                            case "getHeight": return args == null || args.length == 0 ? level.getHeight() : 71;
                            case "getBlockState": return state((BlockPos) args[0]);
                            case "getBlockEntity": return null;
                            case "getFluidState": return state((BlockPos) args[0]).getFluidState();
                            case "isStateAtPosition": return ((java.util.function.Predicate<BlockState>) args[1]).test(state((BlockPos) args[0]));
                            case "isFluidAtPosition": return ((java.util.function.Predicate<net.minecraft.world.level.material.FluidState>) args[1]).test(state((BlockPos) args[0]).getFluidState());
                            case "isEmptyBlock": return state((BlockPos) args[0]).isAir();
                            case "getRawBrightness", "getMaxLocalRawBrightness": return 15;
                            case "canSeeSky", "ensureCanWrite": return true;
                            case "setBlock": return writes.test(((BlockPos) args[0]).immutable(), (BlockState) args[1]);
                            case "getLightEngine": return light;
                            case "getChunk": {
                                if (args[0] instanceof BlockPos pos) return chunkAt.apply(Math.floorDiv(pos.getX(), 16), Math.floorDiv(pos.getZ(), 16));
                                return chunkAt.apply((Integer) args[0], (Integer) args[1]);
                            }
                            default:
                                try { return method.invoke(level, args); }
                                catch (InvocationTargetException failure) { throw failure.getCause(); }
                        }
                    });
        }

        BlockState state(BlockPos pos) { return blocks.getOrDefault(pos, (pos.getY() < 71 ? Blocks.GRASS_BLOCK : Blocks.AIR).defaultBlockState()); }
        boolean accept(BlockPos pos, BlockState state) { blocks.put(pos.immutable(), state); return true; }

        void fillGrass(int minX, int minZ) {
            for (int x = minX; x < minX + 8; x++) for (int z = minZ; z < minZ + 8; z++) {
                boolean tall = (x + z) % 2 == 0;
                blocks.put(new BlockPos(x, 71, z), (tall ? Blocks.TALL_GRASS : Blocks.GRASS).defaultBlockState());
                blocks.put(new BlockPos(x, 72, z), tall ? Blocks.TALL_GRASS.defaultBlockState()
                        .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER) : Blocks.AIR.defaultBlockState());
            }
        }
    }
}
