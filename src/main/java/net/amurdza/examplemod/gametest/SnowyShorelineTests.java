package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.SnowyCaveColumns;
import net.amurdza.examplemod.worldgen.SnowyOceanFloor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SnowyShorelineTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 800)
    public static void dryShoreUndercutIsSealedWithoutClosingDeeperCaves(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var random = level.getChunkSource().randomState();
        var supports = SnowyCaveColumns.supports(random);
        BlockPos selected = null;
        for (int z = -4096; z < 4096; z++) {
            int floor = SnowyOceanFloor.floor(random, 0, z);
            if (floor >= 62 && floor <= 65 && supports.clearance(0, z) > 3) { selected = new BlockPos(0, floor, z); break; }
        }
        helper.assertTrue(selected != null, "Find a seeded dry shore outside pillar attachments");
        var chunk = new ProtoChunk(new ChunkPos(selected), UpgradeData.EMPTY, level,
                level.registryAccess().registryOrThrow(Registries.BIOME), null);
        int x = selected.getX(), z = selected.getZ(), floor = selected.getY();
        for (int y = -32; y <= floor; y++) chunk.setBlockState(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState(), false);
        for (int y = 59; y < Math.min(63, floor); y++) chunk.setBlockState(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), false);
        for (int y = 40; y < 48; y++) chunk.setBlockState(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), false);
        SnowyOceanFloor.restoreCoverAndWater(chunk, random, Blocks.STONE.defaultBlockState());
        SnowyCaveColumns.finish(chunk, random, Blocks.STONE.defaultBlockState());
        for (int y = 59; y <= floor; y++) helper.assertTrue(chunk.getBlockState(new BlockPos(x, y, z)).isSolid(),
                "Interpolated air beneath the dry shore shelf must be closed, including after ceiling expansion");
        for (int y = 40; y < 48; y++) helper.assertTrue(chunk.getBlockState(new BlockPos(x, y, z)).isAir(),
                "The narrow shoreline collar must preserve deeper tall cave air");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 2400)
    public static void completedShorelineFeaturesLeaveNoDryCellsBesideOceanWater(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var random = level.getChunkSource().randomState();
        ChunkPos selected = null;
        for (int z = -8_500_000; z < -8_497_000; z += 16) {
            int wet = 0, dry = 0;
            for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
                int floor = SnowyOceanFloor.floor(random, 8_600_000 + dx, z + dz);
                if (floor >= 62 && floor <= 65) dry++;
                else if (floor >= 58 && floor < 62) wet++;
            }
            if (wet >= 16 && dry >= 16) { selected = new ChunkPos(8_600_000 >> 4, z >> 4); break; }
        }
        helper.assertTrue(selected != null, "Find a fresh mixed wet/dry shoreline chunk");
        java.util.Map<ChunkPos, ChunkAccess> chunks = new java.util.HashMap<>();
        for (int dx = 0; dx <= 1; dx++) for (int dz = 0; dz <= 1; dz++) {
            var pos = new ChunkPos(selected.x + dx, selected.z + dz);
            chunks.put(pos, level.getChunk(pos.x, pos.z, ChunkStatus.FEATURES, true));
        }
        int dryColumns = 0, wetColumns = 0, boundaryChecks = 0;
        for (var entry : chunks.entrySet()) for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            int x = entry.getKey().getMinBlockX() + dx, z = entry.getKey().getMinBlockZ() + dz;
            int floor = SnowyOceanFloor.floor(random, x, z);
            var chunk = entry.getValue();
            if (floor >= 62 && floor <= 65) {
                dryColumns++;
                for (int y = 59; y <= floor; y++) helper.assertFalse(chunk.getBlockState(new BlockPos(x, y, z)).isAir(),
                        "The completed dry coastal shelf cannot retain an air strip below sea level");
            } else if (floor >= 58 && floor < 62) {
                wetColumns++;
                for (int y = floor + 1; y < 63; y++) helper.assertFalse(chunk.getBlockState(new BlockPos(x, y, z)).isAir(),
                        "Completed shallow ocean columns must contain water or their aquatic feature, never dry air");
            }
            for (int y = 59; y < 63; y++) {
                BlockPos pos = new BlockPos(x, y, z);
                if (!chunk.getBlockState(pos).isAir()) continue;
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    BlockPos next = pos.relative(direction);
                    var neighbor = chunks.get(new ChunkPos(next));
                    if (neighbor == null) continue;
                    helper.assertFalse(neighbor.getFluidState(next).is(FluidTags.WATER),
                            "Below-waterline air cannot remain directly exposed to the completed ocean at " + pos);
                    boundaryChecks++;
                }
            }
        }
        helper.assertTrue(wetColumns >= 16 && dryColumns >= 16, "The completed-chunk audit must exercise both sides of the shoreline");
        AOEMod.LOGGER.info("Completed Snowy shore audit: chunks={} dryColumns={} wetColumns={} open-edgeChecks={}",
                chunks.keySet(), dryColumns, wetColumns, boundaryChecks);
        helper.succeed();
    }
}
