package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.IllagerMansionStructure;
import net.amurdza.examplemod.worldgen.structure.MansionBasementPiece;
import net.amurdza.examplemod.worldgen.structure.MansionRampTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MansionGentleRampTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 300)
    public static void tallEstateRampDescendsOneBlockAtATimeAcrossChunkSeamsAndMeetsNaturalGround(GameTestHelper h) {
        var level = h.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        var house = new BoundingBox(-40, 201, -40, 40, 230, 40);
        var site = new MansionRampTerrain.Site(new ChunkPos(0, 0), house, 200);
        int previous = 200;
        boolean beyondReferences = false;
        for (int cx = 6; cx <= 17; cx++) {
            var pos = new ChunkPos(cx, 0);
            var region = region(level, pos);
            var chunk = region.getChunk(cx, 0);
            fill(chunk, pos, 40);
            if (cx == 13) {
                // A pre-existing neighboring tree/flower feature must not
                // leave its original-height stem or canopy above raised soil.
                for (int y = 41; y <= 110; y++) chunk.setBlockState(new BlockPos(210, y, 0), Blocks.OAK_LOG.defaultBlockState(), false);
                chunk.setBlockState(new BlockPos(210, 112, 0), Blocks.OAK_LEAVES.defaultBlockState(), false);
                chunk.setBlockState(new BlockPos(211, 41, 0), Blocks.DANDELION.defaultBlockState(), false);
            }
            var clip = clip(level, pos);
            MansionRampTerrain.place(region, clip, site);
            MansionRampTerrain.place(region, clip, site);
            if (cx == 13) h.assertTrue(region.isEmptyBlock(new BlockPos(210, 110, 0))
                            && region.isEmptyBlock(new BlockPos(210, 112, 0))
                            && region.getBlockState(new BlockPos(211, 41, 0)).is(Blocks.STONE),
                    "Raising soil must remove the original flowers, logs and overhead leaves in every changed column");
            for (int x = Math.max(105, pos.getMinBlockX()); x <= pos.getMaxBlockX(); x++) {
                int expected = Math.max(40, 304 - x);
                int actual = region.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, 0) - 1;
                h.assertTrue(actual == expected, "Ramp must follow its full one-to-one descent at X" + x + ": " + actual + " versus " + expected);
                h.assertTrue(previous - actual >= 0 && previous - actual <= 1, "A chunk boundary introduced a cliff at X" + x);
                h.assertTrue(region.getBlockState(new BlockPos(x, actual, 0)).is(Blocks.GRASS_BLOCK), "Ramp must finish in a grass surface");
                previous = actual;
                if (cx > 8 && actual > 40) beyondReferences = true;
            }
        }
        h.assertTrue(beyondReferences && previous == 40, "A 160-block descent must extend beyond reference range and actually meet original ground");

        // Higher terrain is already the destination. Do not cut another cliff
        // into it, and keep lake water above portions of the submerged ramp.
        var wetPos = new ChunkPos(15, 0);
        var wet = region(level, wetPos);
        var wetChunk = wet.getChunk(15, 0);
        fill(wetChunk, wetPos, 40);
        for (int x = wetPos.getMinBlockX(); x <= wetPos.getMaxBlockX(); x++) for (int z = 0; z < 16; z++)
            for (int y = 41; y <= 80; y++) wetChunk.setBlockState(new BlockPos(x, y, z), Blocks.WATER.defaultBlockState(), false);
        var peak = new BlockPos(250, 120, 8);
        wetChunk.setBlockState(peak, Blocks.DIRT.defaultBlockState(), false);
        MansionRampTerrain.place(wet, clip(level, wetPos), site);
        h.assertTrue(wet.getBlockState(peak).is(Blocks.DIRT), "Natural hills above the ramp must survive");
        h.assertTrue(wet.getBlockState(new BlockPos(252, 80, 0)).is(Blocks.WATER)
                        && wet.getBlockState(new BlockPos(252, 52, 0)).is(Blocks.GRASS_BLOCK),
                "The ramp must continue underwater without draining the lake above it");
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void seededDecorationBuildsTheRampBeyondEightChunksWithoutLoadingItsOwner(GameTestHelper h) {
        var level = h.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        var generator = level.getChunkSource().getGenerator();
        var mansion = (IllagerMansionStructure) level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(AOEMod.makeID("illager_mansion"));
        var state = level.getChunkSource().getGeneratorState();
        var placement = state.possibleStructureSets().stream().map(holder -> holder.value())
                .filter(set -> set.structures().size() == 1 && set.structures().get(0).structure().value() == mansion)
                .map(set -> (RandomSpreadStructurePlacement) set.placement()).findFirst().orElseThrow();
        MansionRampTerrain.Site expectedSite = null;
        // Only pure noise/template planning is used, including the same biome
        // predicate as natural structure generation. No candidate chunk loads.
        for (int i = 0; i < 12 && expectedSite == null; i++) {
            var owner = placement.getPotentialStructureChunk(level.getSeed(), (70 + i) * placement.spacing(), 71 * placement.spacing());
            if (!placement.isStructureChunk(state, owner.x, owner.z)) continue;
            var start = mansion.generate(level.registryAccess(), generator, generator.getBiomeSource(), level.getChunkSource().randomState(),
                    level.getStructureManager(), level.getSeed(), owner, 0, level, mansion.biomes()::contains);
            expectedSite = MansionRampTerrain.fromStart(start).orElse(null);
        }
        h.assertTrue(expectedSite != null, "Find an actual valid seeded mansion candidate for the outer-ramp fixture");
        var site = expectedSite;
        var target = new ChunkPos(site.owner().x + (int) Math.ceil(site.flatRadius() / 16) + 3, site.owner().z);
        var region = region(level, target);
        var chunk = region.getChunk(target.x, target.z);
        int natural = level.getMinBuildHeight() + 1;
        fill(chunk, target, natural);
        h.assertTrue(!region.hasChunk(site.owner().x, site.owner().z)
                        && chunk.getAllReferences().isEmpty(), "Fixture must have no owner chunk or vanilla mansion reference");
        int loadedBefore = level.getChunkSource().getLoadedChunksCount();
        var plans = MansionRampTerrain.sitesForChunk(region, target, generator);
        h.assertTrue(plans.stream().anyMatch(plan -> plan.owner().equals(site.owner()) && plan.groundY() == site.groundY()
                        && plan.house().equals(site.house())), "Outside-reference planning must recover the exact seeded mansion height and footprint");
        for (int pass = 0; pass < 2; pass++) {
            MansionRampTerrain.prepare(region, chunk, generator);
            int raised = 0;
            for (int x = target.getMinBlockX(); x <= target.getMaxBlockX(); x++) for (int z = target.getMinBlockZ(); z <= target.getMaxBlockZ(); z++) {
                int expected = natural;
                for (var plan : plans) if (plan.distanceBeyondFlat(x, z) > 0)
                    expected = Math.max(expected, MansionRampTerrain.targetHeight(plan.groundY(), plan.distanceBeyondFlat(x, z), natural));
                var top = new BlockPos(x, expected, z);
                h.assertTrue(region.getBlockState(top).is(Blocks.GRASS_BLOCK) && region.isEmptyBlock(top.above()),
                        "Actual decoration failed to fill the predicted outer ramp at " + top);
                if (expected > natural) raised++;
            }
            h.assertTrue(raised > 0, "The fixture must exercise actual fill beyond eight chunks");
        }
        h.assertTrue(level.getChunkSource().getLoadedChunksCount() == loadedBefore
                        && level.getChunkSource().getChunkNow(site.owner().x, site.owner().z) == null,
                "Predicting or grading a distant ramp must never load its owner or another chunk");
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 200)
    public static void nativeAfterPlacePreservesAnElevatedLakeBeforeTheManualOuterRampRuns(GameTestHelper h) {
        var level = h.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        var generator = level.getChunkSource().getGenerator();
        var mansion = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(AOEMod.makeID("illager_mansion"));
        var entrance = new BlockPos(8, 201, 8);
        var room = new WoodlandMansionPiece(level.getStructureManager(), "corridor_floor", entrance, Rotation.NONE);
        var cellar = new MansionBasementPiece(entrance, Rotation.NONE, 8);
        var pieces = new PiecesContainer(List.of(room, cellar));
        var pos = new ChunkPos(6, 0);
        var region = region(level, pos);
        var chunk = region.getChunk(pos.x, pos.z);
        fill(chunk, pos, 40);
        for (int x = pos.getMinBlockX(); x <= pos.getMaxBlockX(); x++) for (int z = 0; z < 16; z++)
            for (int y = 41; y <= 240; y++) chunk.setBlockState(new BlockPos(x, y, z), Blocks.WATER.defaultBlockState(), false);
        var site = new MansionRampTerrain.Site(new ChunkPos(entrance), room.getBoundingBox(), cellar.groundsY());
        for (int pass = 0; pass < 2; pass++) {
            mansion.afterPlace(region, level.structureManager().forWorldGenRegion(region), generator, RandomSource.create(0),
                    clip(level, pos), pos, pieces);
            MansionRampTerrain.place(region, clip(level, pos), site);
            for (int x = pos.getMinBlockX(); x <= pos.getMaxBlockX(); x++) {
                int expected = MansionRampTerrain.targetHeight(site.groundY(), site.distanceBeyondFlat(x, 8), 40);
                h.assertTrue(region.getBlockState(new BlockPos(x, expected, 8)).is(Blocks.GRASS_BLOCK)
                                && region.getBlockState(new BlockPos(x, expected + 1, 8)).is(Blocks.WATER)
                                && region.getBlockState(new BlockPos(x, 240, 8)).is(Blocks.WATER),
                        "Native afterPlace must preserve the actual high lake surface before the manual extension observes it");
            }
        }
        h.succeed();
    }

    private static void fill(ChunkAccess chunk, ChunkPos pos, int height) {
        for (int x = pos.getMinBlockX(); x <= pos.getMaxBlockX(); x++) for (int z = pos.getMinBlockZ(); z <= pos.getMaxBlockZ(); z++)
            chunk.setBlockState(new BlockPos(x, height, z), Blocks.GRASS_BLOCK.defaultBlockState(), false);
    }

    private static BoundingBox clip(ServerLevel level, ChunkPos pos) {
        return new BoundingBox(pos.getMinBlockX(), level.getMinBuildHeight(), pos.getMinBlockZ(),
                pos.getMaxBlockX(), level.getMaxBuildHeight() - 1, pos.getMaxBlockZ());
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
