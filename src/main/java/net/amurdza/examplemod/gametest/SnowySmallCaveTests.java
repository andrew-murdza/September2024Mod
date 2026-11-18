package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SnowySmallCaveTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 800)
    public static void ceilingLiftPreservesFloorsOriginalAirAndTallColumns(GameTestHelper helper) {
        var level = snowy(helper); var random = level.getChunkSource().randomState();
        var supports = SnowyCaveColumns.supports(random);
        var chunk = solidChunk(level, new ChunkPos(-1, -1));
        java.util.List<BlockPos> columns = new java.util.ArrayList<>();
        for (int x = -16; x < 0; x++) for (int z = -16; z < 0; z++)
            if (supports.clearance(x, z) > SnowyCaveSupports.FLARE && columns.size() < 18) columns.add(new BlockPos(x, -50, z));
        helper.assertTrue(columns.size() == 18, "Height fixtures must be outside support masks");
        int[] expected = {0, 8, 8, 9, 9, 9, 10, 10, 11, 11, 11, 12, 12};
        for (int height = 0; height < expected.length; height++) for (int y = 0; y < height; y++)
            chunk.setBlockState(columns.get(height).above(y), Blocks.AIR.defaultBlockState(), false);
        for (int i = 13; i < 16; i++) {
            chunk.setBlockState(columns.get(i), Blocks.AIR.defaultBlockState(), false);
            chunk.setBlockState(columns.get(i).above(), (i == 13 ? Blocks.MUD : i == 14 ? Blocks.CLAY : Blocks.GLASS).defaultBlockState(), false);
        }
        // A thin ordinary-stone separator must not leave a short lower cavity
        // below the requested minimum. The tall upper ceiling stays untouched.
        BlockPos stacked = columns.get(16);
        chunk.setBlockState(stacked, Blocks.AIR.defaultBlockState(), false);
        for (int y = 2; y < 10; y++) chunk.setBlockState(stacked.above(y), Blocks.AIR.defaultBlockState(), false);
        BlockPos doubleShort = columns.get(17);
        for (int y : new int[]{0, 1, 3, 4}) chunk.setBlockState(doubleShort.above(y), Blocks.AIR.defaultBlockState(), false);
        SnowyCaveColumns.finish(chunk, random, Blocks.STONE.defaultBlockState());
        for (int height = 0; height < expected.length; height++) {
            BlockPos first = columns.get(height);
            helper.assertTrue(SnowyCaveColumns.raisedHeight(height) == expected[height], "The requested height function must remain exact");
            helper.assertTrue(chunk.getBlockState(first.below()).is(Blocks.STONE), "Raising a ceiling must never lower its floor");
            for (int y = 0; y < expected[height]; y++) helper.assertTrue(chunk.getBlockState(first.above(y)).isAir(),
                    "Original air and modest ceiling extensions must remain open at height=" + height);
            helper.assertTrue(chunk.getBlockState(first.above(expected[height])).is(Blocks.STONE), "Tall ceilings must remain unchanged");
        }
        for (int i = 13; i < 16; i++) helper.assertTrue(chunk.getBlockState(columns.get(i)).isAir()
                && !chunk.getBlockState(columns.get(i).above()).isAir(), "Mud, clay and structure material are hard ceiling stops");
        for (int y = 0; y < 13; y++) helper.assertTrue(chunk.getBlockState(stacked.above(y)).isAir(),
                "A short cave must open its thin stone separator into existing air");
        helper.assertTrue(chunk.getBlockState(stacked.below()).is(Blocks.STONE)
                        && chunk.getBlockState(stacked.above(13)).is(Blocks.STONE),
                "Merging into existing air must preserve the lower floor and apply the upper run height ramp");
        for (int y = 0; y < 7; y++) helper.assertTrue(chunk.getBlockState(doubleShort.above(y)).isAir(),
                "Both original short runs retain their own ceiling expansion after their separator is opened");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 800)
    public static void roundSupportsAreUncutAndFlareAtBothEnds(GameTestHelper helper) {
        var level = snowy(helper); var random = level.getChunkSource().randomState();
        var supports = SnowyCaveColumns.supports(random);
        BlockPos center = null;
        search: for (int x = -80; x < 80; x++) for (int z = -80; z < 80; z++)
            if (supports.clearance(x, z) < -1.5) { center = new BlockPos(x, -40, z); break search; }
        helper.assertTrue(center != null, "Seeded supports need circular cores");
        var chunk = solidChunk(level, new ChunkPos(center));
        int bottom = -50, top = -20, core = 0, ends = 0;
        for (int x = chunk.getPos().getMinBlockX(); x <= chunk.getPos().getMaxBlockX(); x++)
            for (int z = chunk.getPos().getMinBlockZ(); z <= chunk.getPos().getMaxBlockZ(); z++)
                for (int y = bottom; y <= top; y++) chunk.setBlockState(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), false);
        SnowyCaveColumns.finish(chunk, random, Blocks.STONE.defaultBlockState());
        for (int x = chunk.getPos().getMinBlockX(); x <= chunk.getPos().getMaxBlockX(); x++)
            for (int z = chunk.getPos().getMinBlockZ(); z <= chunk.getPos().getMaxBlockZ(); z++) {
                double distance = supports.clearance(x, z);
                for (int y = bottom; y <= top; y++) helper.assertTrue(chunk.getBlockState(new BlockPos(x, y, z)).isSolid()
                        == (distance <= SnowyCaveSupports.flare(y, bottom, top)), "Support voxels must match round cores and symmetric end flares");
                if (distance <= 0) core++;
                if (distance <= SnowyCaveSupports.FLARE) ends++;
            }
        helper.assertTrue(core > 0 && ends > core * 2, "Slim full-height cores need broader floor and ceiling attachments");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 2400)
    public static void actualOceanHasDryCavesIntactMudClayBedsAndNoShoreIce(GameTestHelper helper) {
        var level = snowy(helper); var random = level.getChunkSource().randomState();
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        ChunkPos selected = null;
        search: for (int z = -7_100_000; z < -7_098_000; z += 16) {
            int x = 7_200_000, floor = SnowyOceanFloor.floor(random, x + 8, z + 8);
            if (floor > 28) continue;
            var column = generator.getBaseColumn(x + 8, z + 8, level, random);
            for (int y = -70; y < floor - 8; y += 2) if (column.getBlock(y).isAir()) {
                selected = new ChunkPos(x >> 4, z >> 4); break search;
            }
        }
        helper.assertTrue(selected != null, "Find fresh ocean with native cheese caves beneath it");
        var chunk = level.getChunk(selected.x, selected.z, ChunkStatus.SURFACE, true);
        int caveAir = 0, wet = 0;
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            int x = selected.getMinBlockX() + dx, z = selected.getMinBlockZ() + dz;
            int floor = SnowyOceanFloor.floor(random, x, z);
            if (!SnowyOceanFloor.submerged(floor)) continue;
            wet++;
            for (int depth = 0; depth < 4; depth++) helper.assertTrue(chunk.getBlockState(new BlockPos(x, floor - depth, z))
                    .is(depth < 2 ? Blocks.MUD : Blocks.CLAY), "Seafloor must remain mud/mud/clay/clay without shoreline ice");
            for (int y = floor + 1; y < 63; y++) helper.assertTrue(chunk.getBlockState(new BlockPos(x, y, z)).is(Blocks.WATER),
                    "Every exposed submerged surface must meet water without a dry shoreline gap");
            for (int y = -80; y < floor - 4; y++) if (chunk.getBlockState(new BlockPos(x, y, z)).isAir()) caveAir++;
        }
        helper.assertTrue(wet > 128 && caveAir > 64, "The ocean must retain protected wet beds and substantial dry caves below");
        var profile = new SnowyTerrainProfile(DensityFunctions.constant(0), DensityFunctions.constant(-1), DensityFunctions.constant(100), false);
        helper.assertTrue(profile.compute(new DensityFunction.SinglePointContext(0, -32, 0)) < 0,
                "Old lake exclusion and native pillar sheets must no longer suppress the cheese field");
        AOEMod.LOGGER.info("Ocean cave audit chunk={} wet={} undergroundAir={}", selected, wet, caveAir);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void onlyNetherRetainsAmbientBrightness(GameTestHelper helper) {
        var types = helper.getLevel().registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
        for (String name : new String[]{"tropical", "savanna", "plains", "snowy", "desert", "nether"}) {
            var type = types.get(AOEMod.makeID(name + "_dimension"));
            helper.assertTrue(type != null && Math.abs(type.ambientLight() - (name.equals("nether") ? .1 : 0)) < 1e-7,
                    "Only Nether may retain ambient dimension light");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 3600)
    public static void generatedCavesPreserveEveryVoxelOfConnectedLargeComponents(GameTestHelper helper) {
        var level = snowy(helper);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        // This surveyed window on this seed contains two genuine cross-chunk
        // cheese caves. Both the independent reconstruction and native generation
        // use the same state, regardless of the seed selected by GameTestServer.
        var random = net.minecraft.world.level.levelgen.RandomState.create(generator.generatorSettings().value(),
                level.registryAccess().lookupOrThrow(Registries.NOISE), -4040078797334470895L);
        final int span = 32, min = level.getMinBuildHeight(), max = 111, area = span * span;
        final int baseX = 56_032, baseZ = -63_104;
        var original = new java.util.BitSet((max - min) * area);
        var generated = new java.util.BitSet((max - min) * area);
        var pos = new BlockPos.MutableBlockPos();
        long start = System.nanoTime();
        for (int ox = 0; ox < span; ox += 16) for (int oz = 0; oz < span; oz += 16) {
            var cp = new ChunkPos((baseX + ox) >> 4, (baseZ + oz) >> 4);
            var raw = new ProtoChunk(cp, UpgradeData.EMPTY, level, level.registryAccess().registryOrThrow(Registries.BIOME), null);
            var target = new ProtoChunk(cp, UpgradeData.EMPTY, level, level.registryAccess().registryOrThrow(Registries.BIOME), null);
            target.setStatus(ChunkStatus.BIOMES);
            var region = new net.minecraft.server.level.WorldGenRegion(level, java.util.List.of(target), ChunkStatus.NOISE, 0);
            var completed = generator.fillFromNoise(Runnable::run, net.minecraft.world.level.levelgen.blending.Blender.empty(),
                    random, level.structureManager().forWorldGenRegion(region), target).join();
            for (int x = ox; x < ox + 16; x++) for (int z = oz; z < oz + 16; z++) {
                var column = generator.getBaseColumn(baseX + x, baseZ + z, level, random);
                for (int y = min; y < max; y++) {
                    var state = column.getBlock(y);
                    if (!state.isAir()) raw.setBlockState(pos.set(baseX + x, y, baseZ + z), state, false);
                }
            }
            SnowyOceanFloor.restoreCoverAndWater(raw, random, Blocks.STONE.defaultBlockState());
            SnowyCaveColumns.finish(raw, random, Blocks.STONE.defaultBlockState());
            for (int x = ox; x < ox + 16; x++) for (int z = oz; z < oz + 16; z++) {
                int floor = SnowyOceanFloor.floor(random, baseX + x, baseZ + z);
                for (int y = min; y < Math.min(max, floor); y++) {
                    pos.set(baseX + x, y, baseZ + z);
                    int i = (y - min) * area + z * span + x;
                    if (raw.getBlockState(pos).isAir()) original.set(i);
                    if (completed.getBlockState(pos).isAir()) generated.set(i);
                }
            }
        }
        var extra = (java.util.BitSet) generated.clone();
        extra.andNot(original);
        helper.assertTrue(extra.isEmpty(), "Cleanup must not carve new air or change the original noise profile");
        var remaining = (java.util.BitSet) original.clone();
        int[] queue = new int[(max - min) * area];
        int large = 0, sealedSmall = 0, protectedVoxels = 0;
        for (int startVoxel; (startVoxel = remaining.nextSetBit(0)) >= 0;) {
            int head = 0, length = 1;
            queue[0] = startVoxel;
            remaining.clear(startVoxel);
            var columns = new java.util.BitSet(area);
            boolean boundary = false;
            while (head < length) {
                int index = queue[head++], x = index % span, z = index / span % span;
                columns.set(index % area);
                boundary |= x == 0 || z == 0 || x == span - 1 || z == span - 1;
                for (var direction : net.minecraft.core.Direction.values()) {
                    int nx = x + direction.getStepX(), nz = z + direction.getStepZ();
                    int next = index + direction.getStepX() + span * direction.getStepZ() + area * direction.getStepY();
                    if (nx < 0 || nx >= span || nz < 0 || nz >= span || next < 0 || !remaining.get(next)) continue;
                    remaining.clear(next);
                    queue[length++] = next;
                }
            }
            boolean keep = columns.cardinality() >= SnowyCavePockets.MIN_COLUMNS;
            if (!keep && boundary) continue;
            if (keep) { large++; protectedVoxels += length; } else sealedSmall++;
            for (int i = 0; i < length; i++) helper.assertTrue(generated.get(queue[i]) == keep,
                    "A complete cave component must be retained or removed without clipping");
        }
        helper.assertTrue(large > 0, "Exercise at least one large cave spanning multiple chunks");
        AOEMod.LOGGER.info("Snowy cave parity: checked four real noise chunks, {} large caves, {} protected air voxels, {} sealed small caves, {} ms",
                large, protectedVoxels, sealedSmall, (System.nanoTime() - start) / 1_000_000);
        helper.succeed();
    }

    private static ServerLevel snowy(GameTestHelper helper) { return helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY); }
    private static ProtoChunk solidChunk(ServerLevel level, ChunkPos pos) {
        var chunk = new ProtoChunk(pos, UpgradeData.EMPTY, level, level.registryAccess().registryOrThrow(Registries.BIOME), null);
        for (int x = pos.getMinBlockX(); x <= pos.getMaxBlockX(); x++) for (int z = pos.getMinBlockZ(); z <= pos.getMaxBlockZ(); z++)
            for (int y = level.getMinBuildHeight(); y <= 120; y++) chunk.setBlockState(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState(), false);
        return chunk;
    }
}
