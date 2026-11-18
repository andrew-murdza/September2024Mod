package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.SnowyLakeCaves;
import net.amurdza.examplemod.worldgen.SnowyOceanFloor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SnowyLakeBoundaryTests {
    private SnowyLakeBoundaryTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void snowyBiomeZoomUsesExactBlockHeightAndKeepsOtherDimensionsFuzzy(GameTestHelper helper) {
        ServerLevel snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var biomes = snowy.registryAccess().registryOrThrow(Registries.BIOME);
        Holder<Biome> mushroom = biomes.getHolderOrThrow(ResourceKey.create(Registries.BIOME, AOEMod.makeID("mushroom_caves")));
        Holder<Biome> forest = biomes.getHolderOrThrow(ResourceKey.create(Registries.BIOME, AOEMod.makeID("snowy_forest")));
        BiomeManager.NoiseBiomeSource layer = (x, y, z) -> y >= -8 && y < 0 ? mushroom : forest;
        int fuzzyBoundarySamples = 0;
        for (long seed : new long[]{0, 12345, -8152788754161584091L}) {
            // This source substitution is the same path WorldGenRegion and
            // carvers use; the original Level constructor path also serves clients.
            BiomeManager fixed = new BiomeManager(snowy, seed).withDifferentSource(layer);
            BiomeManager ordinary = new BiomeManager(helper.getLevel(), seed).withDifferentSource(layer);
            BiomeManager crossed = new BiomeManager(snowy, seed)
                    .withDifferentSource(helper.getLevel()).withDifferentSource(layer);
            BiomeManager vanilla = new BiomeManager(layer, seed);
            for (int x = -32; x <= 32; x++) for (int z = -32; z <= 32; z++) {
                for (int y : new int[]{-34, -33, -32, -31, -2, -1, 0, 1}) {
                    BlockPos pos = new BlockPos(x, y, z);
                    boolean expected = y >= -32 && y < 0;
                    helper.assertTrue(fixed.getBiome(pos).equals(expected ? mushroom : forest),
                            "Snowy's displayed mushroom boundary must depend only on block Y: " + pos + ", seed=" + seed);
                    helper.assertTrue(ordinary.getBiome(pos).equals(vanilla.getBiome(pos)),
                            "Other dimensions must retain the original fuzzy biome lookup");
                    helper.assertTrue(crossed.getBiome(pos).equals(vanilla.getBiome(pos)),
                            "Replacing a Snowy source with another Level must discard its vertical-band scope");
                    if (vanilla.getBiome(pos).equals(mushroom) != expected) fuzzyBoundarySamples++;
                }
            }
        }
        helper.assertTrue(fuzzyBoundarySamples > 100,
                "This fixture must exercise actual vertical fuzzy-border cases, rather than an already aligned lookup");
        for (int x = -96; x <= 96; x += 8) for (int z = -96; z <= 96; z += 8) {
            for (int y : new int[]{-33, -32, -31, -1, 0, 1}) {
                var biome = snowy.getBiome(new BlockPos(x, y, z));
                helper.assertTrue(biome.is(mushroom.unwrapKey().orElseThrow()) == (y >= -32 && y < 0),
                        "Real ServerLevel block biomes must use the exact mushroom stratum at " + new BlockPos(x, y, z));
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void oceanBedRepairRetainsUndergroundAirAndWater(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        RandomState random = level.getChunkSource().randomState();
        BlockPos column = locateLake(level, false);
        ProtoChunk chunk = new ProtoChunk(new ChunkPos(column), UpgradeData.EMPTY, level,
                level.registryAccess().registryOrThrow(Registries.BIOME), null);
        int floor = SnowyOceanFloor.floor(random, column.getX(), column.getZ());
        for (int y = level.getMinBuildHeight(); y <= floor; y++)
            chunk.setBlockState(new BlockPos(column.getX(), y, column.getZ()), Blocks.STONE.defaultBlockState(), false);
        chunk.setBlockState(column.atY(floor - 12), Blocks.AIR.defaultBlockState(), false);
        chunk.setBlockState(column.atY(floor - 13), Blocks.WATER.defaultBlockState(), false);
        chunk.setBlockState(column.atY(floor - 2), Blocks.AIR.defaultBlockState(), false);
        for (int y = floor + 1; y < 63; y++) chunk.setBlockState(column.atY(y), Blocks.WATER.defaultBlockState(), false);
        SnowyLakeCaves.closeBuriedPockets(chunk, random, Blocks.STONE.defaultBlockState());
        helper.assertTrue(chunk.getBlockState(column.atY(floor - 12)).isAir()
                        && chunk.getBlockState(column.atY(floor - 13)).is(Blocks.WATER)
                        && chunk.getBlockState(column.atY(floor - 2)).is(Blocks.STONE),
                "Bed repair must preserve deeper caves and fluids while sealing only the seafloor cover");
        for (int y = floor + 1; y < 63; y++) helper.assertTrue(chunk.getFluidState(column.atY(y)).is(FluidTags.WATER),
                "Bed repair must retain open surface water at Y=" + y);
        CarvingMask mask = new CarvingMask(level.getHeight(), level.getMinBuildHeight());
        SnowyLakeCaves.protectCarverColumns(chunk, random, mask);
        for (int y = floor - 3; y <= floor; y++)
            helper.assertTrue(mask.get(column.getX() & 15, y, column.getZ() & 15),
                    "The compatibility mask must protect the bed cover at Y=" + y);
        helper.assertFalse(mask.get(column.getX() & 15, floor - 10, column.getZ() & 15),
                "No whole-column exclusion may suppress caves below the protected bed");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void generatedLakeRetainsWaterWithSolidSubstrateAfterCarvers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        RandomState random = level.getChunkSource().randomState();
        BlockPos anchor = locateLake(level, true);
        var chunk = level.getChunk(anchor.getX() >> 4, anchor.getZ() >> 4, ChunkStatus.CARVERS, true);
        int columns = 0, wet = 0, solids = 0;
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            int x = chunk.getPos().getMinBlockX() + dx, z = chunk.getPos().getMinBlockZ() + dz;
            if (!SnowyLakeCaves.lakeColumn(random, x, z)) continue;
            columns++;
            int floor = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, dx, dz);
            if (chunk.getFluidState(new BlockPos(x, floor + 1, z)).is(FluidTags.WATER)) wet++;
            for (int y = floor - 3; y <= floor; y++) {
                var state = chunk.getBlockState(new BlockPos(x, y, z));
                helper.assertTrue(!state.isAir() && state.getFluidState().isEmpty(),
                        "Generated lake substrate contains a cave after carvers: " + new BlockPos(x, y, z) + ", " + state);
                solids++;
            }
        }
        helper.assertTrue(columns >= 32 && wet >= 16 && solids >= 128,
                "Physical lake audit needs substantial wet seafloor: columns=" + columns + ", wet=" + wet + ", solids=" + solids);
        System.out.println("SNOWY_SOLID_LAKE chunk=" + chunk.getPos() + " columns=" + columns + " wet=" + wet + " solidBlocks=" + solids);
        helper.succeed();
    }

    private static BlockPos locateLake(ServerLevel level, boolean submerged) {
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        RandomState random = level.getChunkSource().randomState();
        for (int z = -4_600_000; z < -4_597_952; z += 32) for (int x = 4_500_000; x < 4_502_048; x += 32) {
            if (!SnowyLakeCaves.lakeColumn(random, x, z)) continue;
            if (!submerged || generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, random) < 45)
                return new BlockPos(x, 0, z);
        }
        throw new IllegalStateException("Could not find a bounded fresh mountain-lake column");
    }
}
