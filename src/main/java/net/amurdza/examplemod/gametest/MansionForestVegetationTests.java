package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.PlainsFlowers;
import net.amurdza.examplemod.worldgen.feature.configs.AllSurfacesFeatureConfig;
import net.amurdza.examplemod.worldgen.feature.features.AllSurfacesFeature;
import net.amurdza.examplemod.worldgen.structure.MansionBasementPiece;
import net.amurdza.examplemod.worldgen.structure.MansionForestFloor;
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
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MansionForestVegetationTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 300)
    public static void bothMansionVariantsPreserveNativeFloorsWhileGradingTheirAirPadding(GameTestHelper h) {
        var level = h.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        var mansion = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(AOEMod.makeID("illager_mansion"));
        int sample = 0;
        for (boolean walls : new boolean[]{true, false}) {
            var chunkPos = new ChunkPos(2900 + sample++, 2900);
            var region = region(level, chunkPos);
            var origin = chunkPos.getWorldPosition().offset(1, 180, 1);
            var cellar = new MansionBasementPiece(origin, Rotation.NONE, 7, walls);
            var lower = new WoodlandMansionPiece(level.getStructureManager(), "corridor_floor", origin, Rotation.NONE);
            var upper = new WoodlandMansionPiece(level.getStructureManager(), "carpet_south_1", origin.above(8), Rotation.NONE);
            var pieces = new PiecesContainer(List.of(lower, upper, cellar));
            var clip = BoundingBox.fromCorners(origin.offset(0, -4, 0), origin.offset(10, 12, 8));
            for (var room : List.of(lower, upper))
                room.postProcess(region, level.structureManager().forWorldGenRegion(region), level.getChunkSource().getGenerator(),
                        RandomSource.create(0), clip, chunkPos, origin);
            // Native carpet covers X1..5; X6 is the genuinely bare floor edge.
            var floor = origin.offset(6, 0, 1);
            var padding = origin.offset(7, 0, 1);
            var outside = origin.offset(9, 0, 1);
            h.assertTrue(region.getBlockState(floor).is(Blocks.BIRCH_PLANKS)
                            && region.isEmptyBlock(floor.above()) && region.isEmptyBlock(padding),
                    "Fixture needs a native bare birch floor and adjacent air padding");
            var upperBefore = region.getBlockState(padding.above(8));
            h.assertTrue(upperBefore.is(Blocks.BIRCH_PLANKS), "Padding must have a real upper-room floor overhead");
            region.getChunk(chunkPos.x, chunkPos.z).setBlockState(outside.above(2), Blocks.DIRT.defaultBlockState(), false);
            for (int repeat = 0; repeat < 2; repeat++) {
                mansion.afterPlace(region, level.structureManager().forWorldGenRegion(region), level.getChunkSource().getGenerator(),
                        RandomSource.create(0), clip, chunkPos, pieces);
                h.assertTrue(region.getBlockState(floor).is(Blocks.BIRCH_PLANKS) && region.isEmptyBlock(floor.above()),
                        "Grading replaced the native room floor or buried it in the " + (walls ? "walled" : "plain") + " variant");
                h.assertTrue(region.getBlockState(padding.above(8)).equals(upperBefore), "Padding grading damaged the upper floor");
                for (var column : List.of(padding, outside)) {
                    var lawn = column.atY(cellar.groundsY());
                    h.assertTrue(region.getBlockState(lawn).is(Blocks.GRASS_BLOCK)
                                    && region.isEmptyBlock(lawn.above()) && region.isEmptyBlock(outside.above(2)),
                            "Air padding must meet the same lawn without a trench or dirt cliff in both variants");
                }
            }
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 300)
    public static void gradedForestRetainsPlainsGrassAndFlowersUnderCanopiesAtAnyElevation(GameTestHelper h) {
        var level = h.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        var loaded = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(AOEMod.makeID("plains/floor/forest_floor_full"));
        var original = (AllSurfacesFeatureConfig) loaded.config();
        var config = new AllSurfacesFeatureConfig(original.feature, original.deepFeature, List.of(),
                original.target, original.predicate, false, original.skipHeight, original.biomes, null, null);
        for (int rootY : new int[]{150, 220}) {
            var chunkPos = new ChunkPos(2500 + rootY, 2500);
            var region = region(level, chunkPos);
            var owner = region.getChunk(chunkPos.x, chunkPos.z);
            var entrance = chunkPos.getWorldPosition().offset(-32, rootY, 0);
            var cellar = new MansionBasementPiece(entrance);
            var room = new WoodlandMansionPiece(level.getStructureManager(), "corridor_floor", entrance, Rotation.NONE);
            record(region, chunkPos, cellar, room);
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                var root = chunkPos.getWorldPosition().offset(x, rootY, z);
                owner.setBlockState(root.below(), Blocks.GRASS_BLOCK.defaultBlockState(), false);
                owner.setBlockState(root.above(8), Blocks.OAK_LEAVES.defaultBlockState(), false);
            }
            var middle = chunkPos.getMiddleBlockPosition(rootY);
            h.assertTrue(Integer.valueOf(rootY).equals(MansionForestFloor.rootY(region, middle.getX(), middle.getZ())),
                    "Graded floor must be recovered from the owner structure instead of the canopy or original terrain");
            new AllSurfacesFeature(AllSurfacesFeatureConfig.CODEC).place(new FeaturePlaceContext<>(Optional.empty(), region,
                    level.getChunkSource().getGenerator(), RandomSource.create(1234), middle, config));
            int grass = 0;
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                var pos = chunkPos.getWorldPosition().offset(x, rootY, z);
                if (region.getBlockState(pos).is(Blocks.GRASS)) grass++;
                h.assertFalse(region.getBlockState(pos).is(Blocks.TALL_GRASS), "Forest tall-grass choices must leave air");
            }
            h.assertTrue(grass >= 40 && grass <= 100, "Raised forest must retain the existing short-grass lottery: " + grass);
            for (int x = 1; x < 7; x++) for (int z = 1; z < 7; z++)
                owner.setBlockState(chunkPos.getWorldPosition().offset(x, rootY, z), Blocks.AIR.defaultBlockState(), false);
            h.assertTrue(PlainsFlowers.placePatch(region, new PlainsFlowers.FlowerPatch(chunkPos.getMinBlockX() + 1,
                            chunkPos.getMinBlockZ() + 1, 19)) == 5,
                    "All five flowers must grow on the actual raised forest soil under the canopy");
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 200)
    public static void naturalGrassCannotInvadePodzolOrMansionAnimalPens(GameTestHelper h) {
        var level = h.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        int sample = 0;
        for (int version = 4; version <= 7; version++) for (var rotation : Rotation.values()) {
            var entrance = new BlockPos(48000 + 128 * sample++, 180, 48000);
            var cellar = new MansionBasementPiece(entrance, rotation, version);
            var pen = cellar.world(24, 1, version == 4 ? 55 : 19);
            var chunkPos = new ChunkPos(pen);
            var region = region(level, chunkPos);
            var room = new WoodlandMansionPiece(level.getStructureManager(), "corridor_floor", entrance, Rotation.NONE);
            record(region, chunkPos, cellar, room);
            var chunk = region.getChunk(chunkPos.x, chunkPos.z);
            chunk.setBlockState(pen.below(), Blocks.GRASS_BLOCK.defaultBlockState(), false);
            for (var block : List.of(Blocks.GRASS, Blocks.TALL_GRASS)) {
                h.assertFalse(region.setBlock(pen, block.defaultBlockState(), 2),
                        "Natural grass entered a saved or raised animal pen: V" + version + "/" + rotation);
            }
            var podzol = pen.above(30);
            chunk.setBlockState(podzol.below(), Blocks.PODZOL.defaultBlockState(), false);
            for (var block : List.of(Blocks.GRASS, Blocks.TALL_GRASS))
                h.assertFalse(region.setBlock(podzol, block.defaultBlockState(), 2), "Podzol must remain free of natural grass");
            var ordinary = podzol.offset(1, 0, 0);
            chunk.setBlockState(ordinary.below(), Blocks.GRASS_BLOCK.defaultBlockState(), false);
            h.assertTrue(region.setBlock(ordinary, Blocks.GRASS.defaultBlockState(), 2),
                    "The restriction must preserve ordinary Plains short grass");
        }
        h.succeed();
    }

    private static WorldGenRegion region(ServerLevel level, ChunkPos pos) {
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                ResourceKey.create(Registries.BIOME, AOEMod.makeID("plains")));
        var chunk = new ProtoChunk(pos, UpgradeData.EMPTY, level,
                level.registryAccess().registryOrThrow(Registries.BIOME), null);
        chunk.fillBiomesFromNoise((x, y, z, sampler) -> biome, level.getChunkSource().randomState().sampler());
        chunk.setStatus(ChunkStatus.CARVERS);
        return new WorldGenRegion(level, List.of(chunk), ChunkStatus.FEATURES, 0);
    }

    private static void record(WorldGenRegion region, ChunkPos owner, MansionBasementPiece cellar, WoodlandMansionPiece room) {
        var mansion = region.registryAccess().registryOrThrow(Registries.STRUCTURE).get(AOEMod.makeID("illager_mansion"));
        var chunk = region.getChunk(owner.x, owner.z);
        chunk.addReferenceForStructure(mansion, owner.toLong());
        chunk.setStartForStructure(mansion, new StructureStart(mansion, owner, 0, new PiecesContainer(List.of(cellar, room))));
    }
}
