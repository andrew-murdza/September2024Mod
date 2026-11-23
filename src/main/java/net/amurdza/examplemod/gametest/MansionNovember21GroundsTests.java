package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.MansionEstateShape;
import net.amurdza.examplemod.worldgen.structure.MansionForestFloor;
import net.amurdza.examplemod.worldgen.structure.MansionRampTerrain;
import net.amurdza.examplemod.worldgen.structure.MansionSiteHeight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MansionNovember21GroundsTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 300)
    public static void widenedCirclePreservesSavedShapesAndSurveysOnlyItsOwnGround(GameTestHelper h) {
        var center = new BlockPos(37, 150, -93);
        for (Rotation rotation : Rotation.values()) {
            var house = BoundingBox.fromCorners(new BlockPos(-40, 0, -28).rotate(rotation).offset(center),
                    new BlockPos(40, 40, 28).rotate(rotation).offset(center));
            var circle = new MansionEstateShape(house, 12);
            var square = new MansionEstateShape(house, 11);
            var original = new MansionEstateShape(house, 10);
            h.assertTrue(!circle.square() && circle.widened() && circle.margin() == 128 && circle.halfWidth() == 168,
                    "The new circular forest retains the complete widened radius in every orientation");
            h.assertTrue(square.square() && square.margin() == 128 && !original.square() && original.margin() == 64,
                    "Saved V11 squares and earlier narrow circles retain their own shape and width");
            var boundary = new BlockPos(168, 0, 0).rotate(rotation).offset(center);
            var outside = new BlockPos(169, 0, 0).rotate(rotation).offset(center);
            var corner = new BlockPos(150, 0, 150).rotate(rotation).offset(center);
            h.assertTrue(circle.contains(boundary.getX(), boundary.getZ()) && !circle.contains(outside.getX(), outside.getZ())
                            && !circle.contains(corner.getX(), corner.getZ()) && square.contains(corner.getX(), corner.getZ())
                            && !original.contains(boundary.getX(), boundary.getZ()),
                    "Only V11 retains the former square corner; V12 keeps the full cardinal extent");
            int[] visits = {0};
            int highest = MansionSiteHeight.highestGround(house, (x, z) -> {
                h.assertTrue(Math.hypot(x - center.getX(), z - center.getZ()) <= 168,
                        "The default height survey must exclude every column outside the widened circle");
                visits[0]++;
                return x == boundary.getX() && z == boundary.getZ() ? 213 : 71;
            });
            h.assertTrue(highest == 213 && visits[0] > 88000,
                    "A one-column summit on the circular boundary must set the new estate height");
            h.assertTrue(MansionSiteHeight.highestGround(house, (x, z) ->
                            x == corner.getX() && z == corner.getZ() ? 300 : 71) == 71
                            && MansionSiteHeight.highestGround(house, 11, (x, z) ->
                            x == corner.getX() && z == corner.getZ() ? 300 : 71) == 300,
                    "A high former square corner must no longer raise a new mansion, while saved V11 keeps its survey");
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void circularOuterForestAndRampStayContinuousWithoutLoadingNeighborChunks(GameTestHelper h) {
        var level = h.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        var center = new BlockPos(16 * 7200 + 8, 180, -16 * 7200 + 8);
        int loaded = level.getChunkSource().getLoadedChunksCount();
        for (Rotation rotation : Rotation.values()) {
            var house = BoundingBox.fromCorners(new BlockPos(-40, 0, -28).rotate(rotation).offset(center),
                    new BlockPos(40, 40, 28).rotate(rotation).offset(center));
            var site = new MansionRampTerrain.Site(new ChunkPos(center), house, 180, 12);
            var forest = new MansionForestFloor.Forest(house, 181, 12);
            // Test an unreferenced flat forest chunk, the former square corner,
            // and terrain beyond the point where the ramp meets natural ground.
            for (var offset : List.of(new BlockPos(144, 0, 0), new BlockPos(150, 0, 150), new BlockPos(280, 0, 280))) {
                var target = new ChunkPos(offset.rotate(rotation).offset(center));
                var region = region(level, target);
                var chunk = region.getChunk(target.x, target.z);
                var clip = new BoundingBox(target.getMinBlockX(), level.getMinBuildHeight(), target.getMinBlockZ(),
                        target.getMaxBlockX(), level.getMaxBuildHeight() - 1, target.getMaxBlockZ());
                for (int x = clip.minX(); x <= clip.maxX(); x++) for (int z = clip.minZ(); z <= clip.maxZ(); z++)
                    chunk.setBlockState(new BlockPos(x, 30, z), Blocks.GRASS_BLOCK.defaultBlockState(), false);
                h.assertFalse(region.hasChunk(site.owner().x, site.owner().z), "The actual owner must remain outside this radius-zero fixture");
                MansionRampTerrain.place(region, clip, site);
                for (int x = clip.minX(); x <= clip.maxX(); x++) for (int z = clip.minZ(); z <= clip.maxZ(); z++) {
                    double distance = Math.hypot(x - center.getX(), z - center.getZ()) - 168;
                    int expected = Math.max(30, 180 - (int) Math.floor(Math.max(0, distance)));
                    var surface = new BlockPos(x, expected, z);
                    h.assertTrue(region.getBlockState(surface).is(Blocks.GRASS_BLOCK) && region.isEmptyBlock(surface.above()),
                            "Flat forest and one-to-one circular ramp must meet without ledges or square corner plateaus: " + rotation + "/" + surface);
                    if (expected > 30) {
                        h.assertTrue(region.getBlockState(surface.below()).is(Blocks.DIRT)
                                        && Integer.valueOf(expected + 1).equals(forest.rootY(region, x, z)),
                                "Every raised forest/ramp column needs supported soil and its correct vegetation root");
                    }
                }
                h.assertTrue(level.getChunkSource().getLoadedChunksCount() == loaded,
                        "The circular terrain pass must not load owners or neighbors");
            }
            var horizonCorner = new ChunkPos(new BlockPos(400, 0, 400).rotate(rotation).offset(center));
            h.assertFalse(site.reaches(horizonCorner, level.getMinBuildHeight()),
                    "Chunks beyond the circular ramp horizon must not be visited merely because they lie in its enclosing square");
            var savedSquare = new MansionRampTerrain.Site(site.owner(), house, 180, 11);
            h.assertTrue(savedSquare.reaches(horizonCorner, level.getMinBuildHeight()),
                    "Saved V11 retains its square ramp extent");
        }
        h.succeed();
    }

    private static WorldGenRegion region(ServerLevel level, ChunkPos pos) {
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                ResourceKey.create(Registries.BIOME, AOEMod.makeID("plains")));
        var chunk = new ProtoChunk(pos, UpgradeData.EMPTY, level, level.registryAccess().registryOrThrow(Registries.BIOME), null);
        chunk.fillBiomesFromNoise((x, y, z, sampler) -> biome, level.getChunkSource().randomState().sampler());
        chunk.setStatus(ChunkStatus.CARVERS);
        return new WorldGenRegion(level, List.of(chunk), ChunkStatus.FEATURES, 0);
    }
}
