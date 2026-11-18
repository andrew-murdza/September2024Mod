package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.CropTiles;
import net.amurdza.examplemod.worldgen.DocumentedFloraFeature;
import net.amurdza.examplemod.worldgen.PlainsVegetation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlainsVegetationTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void threeAdjacentCropChunksPerSpeciesInEach288Tile(GameTestHelper helper) {
        for (long seed : new long[]{50881, -49835}) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = fixture(helper, blocks, seed);
            for (int tileX : new int[]{-2, -1, 0, 1}) for (int tileZ : new int[]{-2, -1, 0, 1}) {
                var patches = PlainsVegetation.cropPatches(seed, tileX, tileZ);
                helper.assertTrue(patches.size() == 9 && patches.equals(PlainsVegetation.cropPatches(seed, tileX, tileZ)),
                        "Every 288 tile needs nine reproducible crop patches");
                Map<String, Integer> species = new HashMap<>();
                for (var patch : patches) {
                    assertInsideTileAndChunk(helper, patch, tileX, tileZ);
                    species.merge(patch.species(), 1, Integer::sum);
                }
                helper.assertTrue(species.size() == 3 && species.values().stream().allMatch(n -> n == 3),
                        "Every 288 tile needs exactly three patches of each species: " + species);
            }
            // Exercise a 1152-square area containing sixteen complete tiles
            // across both positive and negative chunk-aligned tile boundaries.
            for (int cx = -36; cx < 36; cx++) for (int cz = -36; cz < 36; cz++) {
                for (var patch : PlainsVegetation.cropPatchesForChunk(seed, cx, cz, (x, z) -> true)) {
                    helper.assertTrue(Math.floorDiv(patch.x(), 16) == cx && Math.floorDiv(patch.z(), 16) == cz,
                            "Chunk helper returned another chunk's patch");
                    helper.assertTrue(PlainsVegetation.placeCropPatchWithRetries(world, patch) == CropTiles.plantsPerPatch(patch.species()),
                            "An unobstructed reserved patch must contain its complete species quota");
                }
            }
            Map<Tile, Map<String, Integer>> counts = rootCountsByTile(blocks);
            helper.assertTrue(counts.size() == 16, "Expected all 16 tiles across the +/-288 seams: " + counts.keySet());
            counts.forEach((tile, species) -> helper.assertTrue(species.size() == 3
                            && species.getOrDefault("minecraft:pumpkin", 0) == 6
                            && species.getOrDefault("farmersdelight:wild_carrots", 0) == 15
                            && species.getOrDefault("farmersdelight:wild_cabbages", 0) == 15,
                    "Every tile must contain six pumpkins and fifteen roots of each wild crop: " + tile + " " + species));
            Map<BlockPos, BlockState> forward = Map.copyOf(blocks);
            blocks.clear();
            for (int cx = 35; cx >= -36; cx--) for (int cz = 35; cz >= -36; cz--) {
                var patches = PlainsVegetation.cropPatchesForChunk(seed, cx, cz, (x, z) -> true);
                for (int i = patches.size() - 1; i >= 0; i--) PlainsVegetation.placeCropPatchWithRetries(world, patches.get(i));
            }
            helper.assertTrue(blocks.equals(forward), "Chunk and shared-tile generation order changed crop positions or states");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void legacyGrassCountIsRetainedWithCompactTallPatches(GameTestHelper helper) {
        int oldTall = 0, newTall = 0, shortGrass = 0;
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        var grass = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(new ResourceLocation("aoemod:plains/floor/forest_floor"));
        for (int sample = 0; sample < 512; sample++) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = fixture(helper, blocks, sample);
            RandomSource baseline = RandomSource.create(81337L + sample * 97L);
            // Execute the unchanged original 30% floor selector and its actual
            // 74:6 grass provider, then the original 40-column grass opportunities.
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                grass.place(world, level.getChunkSource().getGenerator(), baseline, new BlockPos(x, 71, z));
            }
            for (int attempt = 0; attempt < 40; attempt++) {
                BlockPos pos = new BlockPos(baseline.nextInt(16), 71, baseline.nextInt(16));
                if (!world.isEmptyBlock(pos) || baseline.nextInt(3) == 0) continue;
                int species = baseline.nextInt(16);
                if (species < 2) DocumentedFloraFeature.placePlant(world, pos,
                        species == 0 ? "minecraft:grass" : "minecraft:tall_grass", baseline);
            }
            Map<BlockPos, BlockState> before = Map.copyOf(blocks);
            for (var entry : before.entrySet()) {
                if (entry.getKey().getY() != 71) continue;
                if (entry.getValue().is(Blocks.TALL_GRASS)) oldTall++;
                if (entry.getValue().is(Blocks.GRASS)) shortGrass++;
            }
            int retained = PlainsVegetation.relocateTallGrass(world, 0, 0, RandomSource.create(5197L + sample));
            int[] patchCounts = new int[4];
            int roots = 0;
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                BlockPos pos = new BlockPos(x, 71, z);
                BlockState old = before.getOrDefault(pos, Blocks.AIR.defaultBlockState());
                BlockState now = world.getBlockState(pos);
                helper.assertTrue(old.is(Blocks.GRASS) == now.is(Blocks.GRASS),
                        "Regrouping tall grass must keep every short-grass root unchanged");
                if (!now.is(Blocks.TALL_GRASS)) continue;
                helper.assertTrue(x % 8 >= 1 && x % 8 <= 6 && z % 8 >= 1 && z % 8 <= 6,
                        "Tall grass escaped its 6x6 patch footprint");
                helper.assertTrue(world.getBlockState(pos.above()).is(Blocks.TALL_GRASS), "Tall grass requires both halves");
                patchCounts[(x / 8) * 2 + z / 8]++; roots++;
            }
            for (int size : patchCounts) helper.assertTrue(size == 0 || size >= 4 && size <= 6,
                    "Tall grass patches must contain 4-6 roots, found " + size);
            helper.assertTrue(roots == retained, "Retained count must match actual grass roots");
            newTall += roots;
        }
        helper.assertTrue(newTall > oldTall * 0.97 && newTall < oldTall * 1.03,
                "Tall-grass regrouping must preserve old density; before=" + oldTall + ", after=" + newTall);
        helper.assertTrue(shortGrass > 30000, "Legacy short-grass coverage is missing");
        AOEMod.LOGGER.info("Plains grass relocation, 512 dry chunks: legacy tall {}, clustered tall {}, unchanged short {}",
                oldTall, newTall, shortGrass);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void pumpkinsNeverUseCaveFloorsOrLeaveIncompletePatches(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel world = fixture(helper, blocks, 50881);
        var patch = new PlainsVegetation.CropPatch(5, 5, "minecraft:pumpkin");
        helper.assertTrue(PlainsVegetation.placeCropPatch(world, patch, RandomSource.create(38)) == 2,
                "Pumpkin patches must contain exactly two pumpkins");
        blocks.clear();
        for (int x = 3; x <= 8; x++) for (int z = 3; z <= 8; z++) {
            blocks.put(new BlockPos(x, 71, z), Blocks.STONE.defaultBlockState());
            blocks.put(new BlockPos(x, 51, z), Blocks.AIR.defaultBlockState());
            blocks.put(new BlockPos(x, 52, z), Blocks.AIR.defaultBlockState());
        }
        blocks.remove(new BlockPos(3, 71, 3));
        helper.assertTrue(PlainsVegetation.placeCropPatch(world, patch, RandomSource.create(38)) == 0,
                "One surface slot and a roomy cave must not create a partial pumpkin patch");
        helper.assertFalse(blocks.values().stream().anyMatch(state -> state.is(Blocks.PUMPKIN)),
                "No surface singleton or underground pumpkin may remain after rejection");
        helper.assertTrue(PlainsVegetation.replacesScatteredPlant("minecraft:pumpkin")
                && !PlainsVegetation.replacesScatteredPlant("minecraft:grass")
                && !PlainsVegetation.replacesScatteredPlant("minecraft:tall_grass"),
                "Only replaced scattered species should be suppressed, preserving the legacy grass budget");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void freshlyDecoratedPlainsRetainsPatchesAndSeparatesCavePlants(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        int regionX = 6103, regionZ = -6107;
        Map<PatchOwner, PlainsVegetation.CropPatch> planned = new HashMap<>();
        Map<PatchOwner, Integer> cropCounts = new HashMap<>();
        Map<PatchOwner, java.util.IntSummaryStatistics[]> bounds = new HashMap<>();
        Map<BlockPos, Integer> tallPatchCounts = new HashMap<>();
        // Natural lowlands can contain water. Select a bounded sample using only
        // undecorated terrain; never choose based on final successful crop counts.
        // Complete crop footprints alone do not guarantee a useful dry-land
        // sample for checking the stochastic grass population.
        var generator = level.getChunkSource().getGenerator();
        var randomState = level.getChunkSource().randomState();
        for (int attempt = 0; attempt < 32; attempt++) {
            regionX = 6103 + attempt % 8;
            regionZ = -6107 + attempt / 8;
            planned.clear();
            int drySamples = 0;
            for (int dx = 8; dx < 288; dx += 16) for (int dz = 8; dz < 288; dz += 16) {
                if (generator.getBaseHeight(regionX * 288 + dx, regionZ * 288 + dz,
                        net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, level, randomState)
                        >= generator.getSeaLevel()) drySamples++;
            }
            if (drySamples < 16) continue;
            candidateTiles:
            for (int tx = regionX; tx < regionX + 1; tx++) {
                for (int tz = regionZ; tz < regionZ + 1; tz++) {
                    var tilePlan = PlainsVegetation.cropPatches(level, tx, tz);
                    if (tilePlan.size() != 9) break candidateTiles;
                    for (var patch : tilePlan) {
                        assertInsideTileAndChunk(helper, patch, tx, tz);
                        helper.assertTrue(planned.put(owner(patch.x(), patch.z()), patch) == null,
                                "Two patches claimed the same eight-block cell");
                    }
                }
            }
            if (planned.size() == 9) break;
        }
        helper.assertTrue(planned.size() == 9, "The bounded Plains sample must provide three chunks per species in one dry 288 tile");
        var chunkXs = planned.values().stream().mapToInt(patch -> Math.floorDiv(patch.x(), 16)).summaryStatistics();
        var chunkZs = planned.values().stream().mapToInt(patch -> Math.floorDiv(patch.z(), 16)).summaryStatistics();
        long distinctChunks = planned.values().stream().map(patch -> net.minecraft.world.level.ChunkPos.asLong(
                Math.floorDiv(patch.x(), 16), Math.floorDiv(patch.z(), 16))).distinct().count();
        helper.assertTrue(distinctChunks == 9 && chunkXs.getMax() - chunkXs.getMin() == 2
                        && chunkZs.getMax() - chunkZs.getMin() == 2,
                "Fresh Plains crops must occupy every chunk of one adjacent 3x3 cluster");
        int minX = regionX * 288, minZ = regionZ * 288;
        // Request full chunks before auditing: neighboring decorations must have
        // finished too, so a later tree pass cannot hide accidental extra plants.
        for (int cx = Math.floorDiv(minX, 16); cx < Math.floorDiv(minX, 16) + 18; cx++) {
            for (int cz = Math.floorDiv(minZ, 16); cz < Math.floorDiv(minZ, 16) + 18; cz++) level.getChunk(cx, cz);
        }
        int shortGrass = 0, tallGrass = 0, dryGrassColumns = 0, crops = 0, caveMushrooms = 0, caveLichen = 0;
        for (int x = minX; x < minX + 288; x++) for (int z = minZ; z < minZ + 288; z++) {
            int surface = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (level.getBlockState(new BlockPos(x, surface - 1, z)).is(Blocks.GRASS_BLOCK)) dryGrassColumns++;
            for (int y = level.getMinBuildHeight() + 1; y <= surface + 1; y++) {
                BlockPos pos = new BlockPos(x, y, z);
                BlockState state = level.getBlockState(pos);
                if (state.isAir()) continue;
                if (state.is(Blocks.RED_MUSHROOM) || state.is(Blocks.BROWN_MUSHROOM)) {
                    helper.assertTrue(y < surface, "A mushroom generated on the Plains surface at " + pos);
                    caveMushrooms++;
                }
                if (state.is(Blocks.GLOW_LICHEN)) {
                    helper.assertTrue(y < surface && !level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK),
                            "Glow lichen generated on the surface/grass floor at " + pos);
                    caveLichen++;
                }
                if (state.is(Blocks.GRASS) && y == surface) shortGrass++;
                if (state.is(Blocks.TALL_GRASS) && level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)) {
                    helper.assertTrue(y == surface, "Tall grass escaped the surface relocation pass at " + pos);
                    int localX = Math.floorMod(x, 16), localZ = Math.floorMod(z, 16);
                    helper.assertTrue(localX % 8 >= 1 && localX % 8 <= 6 && localZ % 8 >= 1 && localZ % 8 <= 6,
                            "Generated tall grass lies outside its six-square patch at " + pos);
                    helper.assertTrue(level.getBlockState(pos.above()).is(Blocks.TALL_GRASS),
                            "Generated tall grass lost its upper half at " + pos);
                    BlockPos patch = new BlockPos(Math.floorDiv(x, 16), (localX / 8) * 2 + localZ / 8, Math.floorDiv(z, 16));
                    tallPatchCounts.merge(patch, 1, Integer::sum); tallGrass++;
                }
                if (!isCrop(state)) continue;
                if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)
                        && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)
                        == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER) continue;
                helper.assertTrue(y >= surface - 1 && level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK),
                        "A wild crop or pumpkin generated underground at " + pos);
                String id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
                int ownerChunkX = Math.floorDiv(x, 16), ownerChunkZ = Math.floorDiv(z, 16);
                var matching = planned.entrySet().stream().filter(e -> e.getValue().species().equals(id)
                                && Math.floorDiv(e.getValue().x(), 16) == ownerChunkX
                                && Math.floorDiv(e.getValue().z(), 16) == ownerChunkZ).findFirst().orElse(null);
                PatchOwner chunk = matching == null ? owner(x, z) : matching.getKey();
                var patch = matching == null ? null : matching.getValue();
                helper.assertTrue(patch != null && patch.species().equals(id),
                        "A crop escaped its species-owned eight-block cell at " + pos);
                cropCounts.merge(chunk, 1, Integer::sum); crops++;
                var area = bounds.computeIfAbsent(chunk, ignored -> new java.util.IntSummaryStatistics[]{
                        new java.util.IntSummaryStatistics(), new java.util.IntSummaryStatistics()});
                area[0].accept(x); area[1].accept(z);
            }
        }
        cropCounts.forEach((chunk, count) -> helper.assertTrue(count == CropTiles.plantsPerPatch(planned.get(chunk).species()),
                "A generated crop patch must contain its complete species quota or be absent, got " + count));
        bounds.forEach((chunk, area) -> helper.assertTrue(area[0].getMax() - area[0].getMin() < 6
                        && area[1].getMax() - area[1].getMin() < 6, "Retried crop patch escaped its six-square footprint"));
        tallPatchCounts.forEach((patch, count) -> helper.assertTrue(count >= 4 && count <= 6,
                "A generated tall-grass patch must contain 4-6 roots, got " + count + " at " + patch));
        String grassSample = "dry grass columns=" + dryGrassColumns + ", short=" + shortGrass + ", tall=" + tallGrass;
        helper.assertTrue(dryGrassColumns >= 512, "The terrain-selected grass sample is too small: " + grassSample);
        // The legacy dry-floor lottery averages roughly 28% short and 2% tall.
        // Allow terrain/flowers/cluster obstruction while still detecting a lost
        // generation pass. Water columns cannot contribute to this denominator.
        helper.assertTrue(shortGrass > dryGrassColumns * 0.15 && tallGrass > dryGrassColumns * 0.005,
                "Fresh Plains must retain both grass populations per dry land area: " + grassSample);
        helper.assertTrue(planned.size() == 9 && cropCounts.size() == 9 && crops == 36,
                "One fresh 288 tile must contain six pumpkins, fifteen carrots and fifteen cabbages; planned="
                        + planned.size() + ", complete=" + cropCounts.size() + ", roots=" + crops);
        Map<Tile, Map<String, Integer>> speciesCounts = new HashMap<>();
        cropCounts.forEach((chunk, count) -> speciesCounts.computeIfAbsent(new Tile(chunk.tileX(), chunk.tileZ()),
                ignored -> new HashMap<>()).merge(planned.get(chunk).species(), 1, Integer::sum));
        helper.assertTrue(speciesCounts.size() == 1, "The sample must cover one 288 tile");
        speciesCounts.forEach((tile, species) -> helper.assertTrue(species.size() == 3
                        && species.values().stream().allMatch(n -> n == 3),
                "Each 288 tile must have three completed patches per crop: " + tile + " " + species));
        AOEMod.LOGGER.info("Fresh Plains one 288 tile at region ({},{}): dry grass columns {}, short {}, tall {}, tall patches {}, crop patches {} of 9, cave mushrooms {}, cave lichen {}",
                regionX, regionZ, dryGrassColumns, shortGrass, tallGrass, tallPatchCounts.size(), cropCounts.size(), caveMushrooms, caveLichen);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void cropTilesVaryBySeedAndCoordinatesAndRejectWetSites(GameTestHelper helper) {
        var baseline = PlainsVegetation.cropPatches(1234, 0, 0);
        helper.assertFalse(baseline.equals(PlainsVegetation.cropPatches(4321, 0, 0)),
                "Changing the world seed must move crop patches");
        var neighbor = PlainsVegetation.cropPatches(1234, 1, 0);
        var translated = neighbor.stream().map(p -> new PlainsVegetation.CropPatch(p.x() - 288, p.z(), p.species())).toList();
        helper.assertFalse(baseline.equals(translated), "Adjacent 288 tiles must not repeat an identical local pattern");
        for (int tx = -3; tx <= 3; tx++) for (int tz = -3; tz <= 3; tz++) {
            final int originX = tx * 288;
            var patches = PlainsVegetation.cropPatches(6194, tx, tz, (x, z) -> x >= originX + 40);
            Map<String, Integer> counts = new HashMap<>();
            var cells = new java.util.HashSet<Long>();
            for (var patch : patches) {
                assertInsideTileAndChunk(helper, patch, tx, tz);
                helper.assertTrue(patch.x() >= originX + 40, "Wet columns must not consume a species quota");
                helper.assertTrue(cells.add(net.minecraft.world.level.ChunkPos.asLong(
                                Math.floorDiv(patch.x(), 8), Math.floorDiv(patch.z(), 8))),
                        "A tile's patches must have distinct eight-block owner cells");
                counts.merge(patch.species(), 1, Integer::sum);
            }
            helper.assertTrue(counts.size() == 3 && counts.values().stream().allMatch(n -> n == 3),
                    "Partly wet tiles need three adjacent complete opportunities for each species");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void allWaterTileDoesNotBorrowItsNeighborsCropQuota(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel world = fixture(helper, blocks, 98172);
        java.util.function.BiPredicate<Integer, Integer> dry = (x, z) -> x >= 288 && x < 576 && z >= 48 && z < 288;
        helper.assertTrue(PlainsVegetation.cropPatches(98172, 0, 0, dry).isEmpty(),
                "An all-water tile must not plant submerged crops");
        var neighbor = PlainsVegetation.cropPatches(98172, 1, 0, dry);
        helper.assertTrue(neighbor.size() == 9, "Adjacent partly wet tile still needs its own nine crop chunks");
        for (int cx = 0; cx < 36; cx++) for (int cz = 0; cz < 18; cz++) {
            for (var patch : PlainsVegetation.cropPatchesForChunk(98172, cx, cz, dry)) {
                helper.assertTrue(PlainsVegetation.placeCropPatchWithRetries(world, patch) == CropTiles.plantsPerPatch(patch.species()),
                        "The dry neighboring tile lost a crop patch");
            }
        }
        var counts = rootCountsByTile(blocks);
        helper.assertTrue(counts.size() == 1 && counts.containsKey(new Tile(1, 0)),
                "A water tile must neither receive crops nor shift its quota into the neighboring tile: " + counts);
        helper.assertTrue(counts.get(new Tile(1, 0)).size() == 3
                        && counts.get(new Tile(1, 0)).getOrDefault("minecraft:pumpkin", 0) == 6
                        && counts.get(new Tile(1, 0)).getOrDefault("farmersdelight:wild_carrots", 0) == 15
                        && counts.get(new Tile(1, 0)).getOrDefault("farmersdelight:wild_cabbages", 0) == 15,
                "The good neighboring tile must contain exactly 36 roots, with two pumpkins per patch");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void aSingleDryBoundaryChunkCannotSplitAnAdjacentCropCluster(GameTestHelper helper) {
        for (int seam : new int[]{-288, 288}) {
            int chunkX = Math.floorDiv(seam, 16), chunkZ = Math.floorDiv(seam, 16);
            var patches = PlainsVegetation.cropPatchesForChunk(81293, chunkX, chunkZ,
                    (x, z) -> Math.floorDiv(x, 16) == chunkX && Math.floorDiv(z, 16) == chunkZ);
            helper.assertTrue(patches.isEmpty(),
                    "A lone dry chunk cannot borrow neighboring tile ground to form a 2x2 crop cluster");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void boundaryRetriesStayInsideTheirEightWideTileIntersection(GameTestHelper helper) {
        for (int seam : new int[]{-288, 288}) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = fixture(helper, blocks, 91235);
            var patch = new PlainsVegetation.CropPatch(seam - 6, seam - 6, "minecraft:pumpkin");
            for (int x = seam - 8; x <= seam - 3; x++) for (int z = seam - 8; z <= seam - 3; z++) {
                blocks.put(new BlockPos(x, 71, z), Blocks.STONE.defaultBlockState());
            }
            helper.assertTrue(PlainsVegetation.placeCropPatchWithRetries(world, patch) == 2,
                    "A blocked boundary anchor must retry within its eight-wide tile intersection");
            var roots = blocks.entrySet().stream().filter(e -> e.getValue().is(Blocks.PUMPKIN))
                    .map(Map.Entry::getKey).toList();
            helper.assertTrue(roots.size() == 2 && roots.stream().allMatch(pos -> pos.getX() >= seam - 8
                            && pos.getX() < seam && pos.getZ() >= seam - 8 && pos.getZ() < seam),
                    "A retried crop patch escaped into a neighboring 288 tile at seam " + seam);
            helper.assertTrue(roots.stream().mapToInt(BlockPos::getX).max().orElseThrow()
                            - roots.stream().mapToInt(BlockPos::getX).min().orElseThrow() < 6
                            && roots.stream().mapToInt(BlockPos::getZ).max().orElseThrow()
                            - roots.stream().mapToInt(BlockPos::getZ).min().orElseThrow() < 6,
                    "Retried boundary patch must stay within a six-square footprint");
            blocks.clear();
            for (int x = seam - 16; x < seam; x++) for (int z = seam - 16; z < seam; z++) {
                blocks.put(new BlockPos(x, 71, z), Blocks.STONE.defaultBlockState());
            }
            blocks.remove(new BlockPos(seam - 8, 71, seam - 8));
            helper.assertTrue(PlainsVegetation.placeCropPatchWithRetries(world, patch) == 0,
                    "One local slot must fail even when the neighboring tile's chunk is entirely empty");
            helper.assertFalse(blocks.values().stream().anyMatch(state -> state.is(Blocks.PUMPKIN)),
                    "Failed boundary retry left partial crops or borrowed its neighbor's ground");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void obstructedCropAnchorRetriesWithinItsOwnChunk(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel world = fixture(helper, blocks, 91235);
        var patch = new PlainsVegetation.CropPatch(5, 5, "minecraft:pumpkin");
        for (int x = 3; x <= 8; x++) for (int z = 3; z <= 8; z++)
            blocks.put(new BlockPos(x, 71, z), Blocks.STONE.defaultBlockState());
        helper.assertTrue(PlainsVegetation.placeCropPatchWithRetries(world, patch) == 2,
                "A blocked first footprint must relocate to a complete dry patch in the same chunk");
        var plants = blocks.entrySet().stream().filter(e -> e.getValue().is(Blocks.PUMPKIN)).map(Map.Entry::getKey).toList();
        helper.assertTrue(plants.size() == 2 && plants.stream().allMatch(p -> p.getX() >= 0 && p.getX() < 16 && p.getZ() >= 0 && p.getZ() < 16),
                "Retries must preserve exactly two pumpkins and stay inside the owning chunk");
        int minX = plants.stream().mapToInt(BlockPos::getX).min().orElseThrow();
        int maxX = plants.stream().mapToInt(BlockPos::getX).max().orElseThrow();
        int minZ = plants.stream().mapToInt(BlockPos::getZ).min().orElseThrow();
        int maxZ = plants.stream().mapToInt(BlockPos::getZ).max().orElseThrow();
        helper.assertTrue(maxX - minX < 6 && maxZ - minZ < 6, "Retried crops must remain a six-square patch");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void naturalTerrainPlansKeepValidCompleteFootprints(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        var generator = level.getChunkSource().getGenerator();
        var randomState = level.getChunkSource().randomState();
        for (int x : new int[]{-1, 0}) for (int z : new int[]{-1, 0}) {
            var patches = PlainsVegetation.cropPatches(level, x, z);
            var species = new java.util.HashSet<String>();
            var owners = new java.util.HashSet<PatchOwner>();
            for (var patch : patches) {
                assertInsideTileAndChunk(helper, patch, x, z);
                species.add(patch.species());
                helper.assertTrue(owners.add(owner(patch.x(), patch.z())),
                        "A natural tile must not repeat reserved cells");
                int dry = 0;
                for (int dx = -2; dx <= 3; dx++) for (int dz = -2; dz <= 3; dz++) {
                    if (generator.getBaseHeight(patch.x() + dx, patch.z() + dz,
                            net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, level, randomState)
                            >= generator.getSeaLevel()) dry++;
                }
                helper.assertTrue(dry >= CropTiles.plantsPerPatch(patch.species()),
                        "Natural planned footprint lacks enough exposed columns for " + patch.species());
            }
            helper.assertTrue(patches.isEmpty() || patches.size() == 9, "Natural tiles must preserve the complete nine-chunk cluster");
        }
        helper.succeed();
    }

    private static void assertInsideTileAndChunk(GameTestHelper helper, PlainsVegetation.CropPatch patch, int tileX, int tileZ) {
        helper.assertTrue(patch.x() - 2 >= tileX * 288 && patch.x() + 3 < (tileX + 1) * 288
                        && patch.z() - 2 >= tileZ * 288 && patch.z() + 3 < (tileZ + 1) * 288,
                "Six-square footprint escaped its 288 tile, including negative coordinates: " + patch);
        helper.assertTrue(Math.floorDiv(patch.x() - 2, 8) == Math.floorDiv(patch.x() + 3, 8)
                        && Math.floorDiv(patch.z() - 2, 8) == Math.floorDiv(patch.z() + 3, 8),
                "Six-square footprint escaped its eight-block owner cell: " + patch);
    }

    private static Map<Tile, Map<String, Integer>> rootCountsByTile(Map<BlockPos, BlockState> blocks) {
        Map<Tile, Map<String, Integer>> counts = new HashMap<>();
        blocks.forEach((pos, state) -> {
            if (pos.getY() == 71 && isCrop(state)) counts.computeIfAbsent(
                    new Tile(Math.floorDiv(pos.getX(), 288), Math.floorDiv(pos.getZ(), 288)), ignored -> new HashMap<>())
                    .merge(net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString(), 1, Integer::sum);
        });
        return counts;
    }

    private static PatchOwner owner(int x, int z) {
        return new PatchOwner(Math.floorDiv(x, 288), Math.floorDiv(z, 288), Math.floorDiv(x, 8), Math.floorDiv(z, 8));
    }

    private record Tile(int x, int z) { }
    private record PatchOwner(int tileX, int tileZ, int cellX, int cellZ) { }

    private static boolean isCrop(BlockState state) {
        String id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
        return id.startsWith("farmersdelight:wild_") || state.is(Blocks.PUMPKIN);
    }

    private static WorldGenLevel fixture(GameTestHelper helper, Map<BlockPos, BlockState> blocks, long seed) {
        var plains = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        var biome = plains.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod:plains")));
        java.util.function.Function<BlockPos, BlockState> stateAt = pos -> blocks.getOrDefault(pos,
                pos.getY() <= 70 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState());
        return (WorldGenLevel) Proxy.newProxyInstance(PlainsVegetationTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getLevel": return plains;
                        case "getSeed": return seed;
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
