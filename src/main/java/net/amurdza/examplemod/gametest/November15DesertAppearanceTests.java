package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DesertHeight;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.amurdza.examplemod.worldgen.NetherRampDecorationGuard;
import net.amurdza.examplemod.worldgen.surface_rule.DimensionSurfaceRule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class November15DesertAppearanceTests {
    private November15DesertAppearanceTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void carvedEntrancesKeepStoneButUntouchedRimsKeepTheirSurface(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        var random = RandomState.create(generator.generatorSettings().value(),
                level.registryAccess().lookupOrThrow(Registries.NOISE), 12345);
        var terrain = NetherRampDecorationGuard.terrain(random).orElseThrow();
        var biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        int cut = 0, rim = 0;
        for (int cycle : new int[]{-1, 0}) {
            var route = terrain.desertOutlet(cycle, 0);
            BlockPos mouth = null;
            int mouthDistance = 0;
            for (int distance = 20; distance < route.length(); distance++) {
                int x = (int) Math.round(route.xAt(distance)), z = (int) Math.round(route.zAt(distance));
                double depth = terrain.desertUncarvedSurfaceY(x, z) - route.floorAt(distance);
                if (depth > 3 && depth < 5) {
                    mouth = new BlockPos(x, (int) route.floorAt(distance), z);
                    mouthDistance = distance;
                    break;
                }
            }
            helper.assertTrue(mouth != null, "Seeded outlet must meet the natural surface for cycle " + cycle);
            var chunks = new HashSet<ChunkPos>();
            double heading = route.headingAt(mouthDistance);
            for (int across : new int[]{-12, 0, 12}) chunks.add(new ChunkPos(mouth.offset(
                    (int) Math.round(across * Math.cos(heading)), 0, (int) Math.round(-across * Math.sin(heading)))));
            for (var chunkPos : chunks) {
                var chunk = new ProtoChunk(chunkPos, UpgradeData.EMPTY, level, biomes, null);
                int[] natural = new int[256], before = new int[256];
                boolean[] thickCap = new boolean[256];
                for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
                    int x = chunkPos.getMinBlockX() + dx, z = chunkPos.getMinBlockZ() + dz, index = dx + dz * 16;
                    natural[index] = terrain.desertUncarvedSurfaceY(x, z);
                    var column = generator.getBaseColumn(x, z, level, random);
                    for (int y = -32; y <= Math.min(level.getMaxBuildHeight() - 1, natural[index] + 2); y++) {
                        var state = column.getBlock(y);
                        if (!state.isAir()) chunk.setBlockState(new BlockPos(x, y, z), state, false);
                        if (state.is(Blocks.STONE)) before[index] = y;
                    }
                    thickCap[index] = column.getBlock(before[index] - 1).is(Blocks.STONE)
                            && column.getBlock(before[index] - 2).is(Blocks.STONE);
                }
                Heightmap.primeHeightmaps(chunk, Set.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG));
                random.surfaceSystem().buildSurface(random, level.getBiomeManager(), biomes, false,
                        new WorldGenerationContext(generator, level), chunk, null, new DimensionSurfaceRule("desert"));
                for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
                    int index = dx + dz * 16, top = before[index];
                    int x = chunkPos.getMinBlockX() + dx, z = chunkPos.getMinBlockZ() + dz;
                    var pos = new BlockPos(x, top, z);
                    var state = chunk.getBlockState(pos);
                    if (top >= 16 && top < natural[index] - 1) {
                        helper.assertTrue(state.is(Blocks.STONE), "The actual cut surface must remain stone at " + pos + ": " + state);
                        cut++;
                    } else if (top == natural[index] && thickCap[index] && route.density(x, top, z) < .75) {
                        boolean expected = top < DesertHeight.RED_SAND_START_HEIGHT ? state.is(Blocks.SAND)
                                : top < DesertHeight.FIRST_TERRACOTTA_Y ? state.is(Blocks.RED_SAND) : state.is(BlockTags.TERRACOTTA);
                        helper.assertTrue(expected, "Untouched original rim must retain its sand/terracotta, without a stone halo, at " + pos + ": " + state);
                        if (!terrain.desertCaves().surfaceWasCut(x, z)) {
                            var lower = chunk.getBlockState(pos.below());
                            boolean skin = top < DesertHeight.RED_SAND_START_HEIGHT ? lower.is(Blocks.SAND)
                                    : top < DesertHeight.FIRST_TERRACOTTA_Y ? lower.is(Blocks.RED_SAND) : lower.is(BlockTags.TERRACOTTA);
                            helper.assertTrue(skin, "An uncarved slope's exposed second skin block must not be repainted stone at " + pos.below());
                        }
                        rim++;
                    }
                }
            }
        }
        helper.assertTrue(cut > 20 && rim > 10, "Exercise real cut faces and unchanged rim columns: cut=" + cut + ", rim=" + rim);
        AOEMod.LOGGER.info("November 15 desert entrance material audit: {} cut columns, {} unchanged rim columns", cut, rim);
        helper.succeed();
    }
}
