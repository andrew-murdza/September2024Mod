package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.PlantGenerationSupport;
import net.amurdza.examplemod.worldgen.PlainsFlowers;
import net.amurdza.examplemod.worldgen.StructurePlantExclusion;
import net.amurdza.examplemod.worldgen.feature.configs.AllSurfacesFeatureConfig;
import net.amurdza.examplemod.worldgen.feature.features.AllSurfacesFeature;
import net.amurdza.examplemod.worldgen.structure.IllagerMansionStructure;
import net.amurdza.examplemod.worldgen.structure.MansionBasementPiece;
import net.amurdza.examplemod.worldgen.structure.MansionEstateShape;
import net.amurdza.examplemod.worldgen.structure.MansionForestFloor;
import net.amurdza.examplemod.worldgen.structure.MansionGroundCover;
import net.amurdza.examplemod.worldgen.structure.MansionRampTerrain;
import net.amurdza.examplemod.worldgen.structure.MansionSiteHeight;
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
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MansionNovember18GroundsTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void squareForestDoublesItsMarginAndOnlyItsOwnHighestGroundSetsTheEstateHeight(GameTestHelper h) {
        var house = new BoundingBox(-40, 150, -40, 40, 190, 40);
        var shape = new MansionEstateShape(house, 11);
        h.assertTrue(shape.margin() == 128 && shape.halfWidth() == 168 && shape.contains(168, 168),
                "The new flat forest must extend128 blocks beyond the mansion on every side, including square corners");
        h.assertFalse(shape.contains(169, 168), "The first ramp column must be outside the flat forest");
        h.assertTrue(new MansionEstateShape(house, 8).margin() == 64
                        && !new MansionEstateShape(house, 8).contains(104, 104), "Saved circle layouts must retain their original width and corner shape");
        for (int[] corner : new int[][]{{168, 168}, {-168, 168}, {168, -168}, {-168, -168}}) {
            int highest = MansionSiteHeight.highestGround(house, 11, (x, z) -> {
                h.assertTrue(Math.abs(x) <= 168 && Math.abs(z) <= 168, "Ramp terrain must never enter the estate height survey");
                return x == corner[0] && z == corner[1] ? 215 : 71;
            });
            h.assertTrue(highest == 215, "A summit in every square forest corner must determine its height");
        }
        h.assertTrue(MansionSiteHeight.highestGround(house, 11, (x, z) -> Math.max(Math.abs(x), Math.abs(z)) > 168 ? 300 : 71) == 71,
                "Even an entirely higher surrounding ramp cannot raise the mansion");
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1600)
    public static void widenedForestGroundAndTreesGenerateBeyondStructureReferencesWithoutOwnerLoads(GameTestHelper h) {
        var level = h.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        var generator = level.getChunkSource().getGenerator();
        var mansion = (IllagerMansionStructure) level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(AOEMod.makeID("illager_mansion"));
        var state = level.getChunkSource().getGeneratorState();
        var placement = state.possibleStructureSets().stream().map(holder -> holder.value())
                .filter(set -> set.structures().size() == 1 && set.structures().get(0).structure().value() == mansion)
                .map(set -> (RandomSpreadStructurePlacement) set.placement()).findFirst().orElseThrow();
        MansionRampTerrain.Site site = null;
        for (int i = 0; i < 20 && site == null; i++) {
            var owner = placement.getPotentialStructureChunk(level.getSeed(), (70 + i) * placement.spacing(), 71 * placement.spacing());
            var start = mansion.generate(level.registryAccess(), generator, generator.getBiomeSource(), level.getChunkSource().randomState(),
                    level.getStructureManager(), level.getSeed(), owner, 0, level, mansion.biomes()::contains);
            site = MansionRampTerrain.fromStart(start).orElse(null);
        }
        h.assertTrue(site != null && site.layoutVersion() >= 12 && !site.shape().square(), "Find a real seeded widened circular estate");
        int loaded = level.getChunkSource().getLoadedChunksCount();
        for (Rotation rotation : Rotation.values()) {
            var offset = new BlockPos(9, 0, 0).rotate(rotation);
            var target = new ChunkPos(site.owner().x + offset.getX(), site.owner().z + offset.getZ());
            var region = region(level, target);
            var chunk = region.getChunk(target.x, target.z);
            for (int x = target.getMinBlockX(); x <= target.getMaxBlockX(); x++) for (int z = target.getMinBlockZ(); z <= target.getMaxBlockZ(); z++)
                chunk.setBlockState(new BlockPos(x, 30, z), Blocks.GRASS_BLOCK.defaultBlockState(), false);
            h.assertFalse(region.hasChunk(site.owner().x, site.owner().z), "The fixture must be outside the vanilla eight-chunk owner region");
            MansionRampTerrain.prepare(region, chunk, generator);
            int trees = 0;
            for (int x = target.getMinBlockX(); x <= target.getMaxBlockX(); x++) for (int z = target.getMinBlockZ(); z <= target.getMaxBlockZ(); z++) {
                h.assertTrue(site.shape().contains(x, z) && region.getBlockState(new BlockPos(x, site.groundY(), z)).is(Blocks.GRASS_BLOCK),
                        "Every column of the distant circular forest must meet the flat estate floor: " + rotation);
                if (region.getBlockState(new BlockPos(x, site.groundY() + 1, z)).is(net.minecraft.tags.BlockTags.LOGS)) trees++;
                h.assertTrue(Integer.valueOf(site.groundY() + 1).equals(MansionForestFloor.rootY(region, x, z)),
                        "Forest floor metadata must reach the unreferenced outer forest under its canopy");
            }
            h.assertTrue(trees > 0 && level.getChunkSource().getLoadedChunksCount() == loaded,
                    "Every side of the outer forest must have trees while loading no owner or neighbor chunks: " + rotation);
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 500)
    public static void grassAndFlowersReachMansionWallsAndRampCanopiesWithoutWildCrops(GameTestHelper h) {
        var level = h.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        var loaded = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(AOEMod.makeID("plains/floor/forest_floor_full"));
        var original = (AllSurfacesFeatureConfig) loaded.config();
        var config = new AllSurfacesFeatureConfig(original.feature, original.deepFeature, List.of(), original.target,
                original.predicate, false, original.skipHeight, original.biomes, null, null);
        int sample = 0;
        for (int version : new int[]{11, 12}) for (boolean ramp : new boolean[]{false, true}) {
            var pos = new ChunkPos(6100 + sample++, 6100);
            var region = region(level, pos);
            var chunk = region.getChunk(pos.x, pos.z);
            var roomOrigin = pos.getWorldPosition().offset(ramp ? -180 : 1, 180, 1);
            var room = new WoodlandMansionPiece(level.getStructureManager(), "corridor_floor", roomOrigin, Rotation.NONE);
            // Keep the basement away from this surface-only fixture so its
            // authored stairwell exclusion cannot stand in for the wall edge.
            var cellar = new MansionBasementPiece(roomOrigin.offset(-300, 0, 0), Rotation.NONE, version);
            var mansion = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(AOEMod.makeID("illager_mansion"));
            chunk.addReferenceForStructure(mansion, pos.toLong());
            chunk.setStartForStructure(mansion, new StructureStart(mansion, pos, 0, new PiecesContainer(List.of(cellar, room))));
            var shape = new MansionEstateShape(room.getBoundingBox(), version);
            for (int x = pos.getMinBlockX(); x <= pos.getMaxBlockX(); x++) for (int z = pos.getMinBlockZ(); z <= pos.getMaxBlockZ(); z++) {
                int root = 180 - (int) Math.floor(Math.max(0, shape.distanceBeyondFlat(x, z)));
                chunk.setBlockState(new BlockPos(x, root - 1, z), Blocks.GRASS_BLOCK.defaultBlockState(), false);
                chunk.setBlockState(new BlockPos(x, root + 9, z), Blocks.OAK_LEAVES.defaultBlockState(), false);
            }
            new AllSurfacesFeature(AllSurfacesFeatureConfig.CODEC).place(new FeaturePlaceContext<>(Optional.empty(), region,
                    level.getChunkSource().getGenerator(), RandomSource.create(1234), pos.getWorldPosition(), config));
            int grass = 0, edgeGrass = 0;
            for (int x = pos.getMinBlockX(); x <= pos.getMaxBlockX(); x++) for (int z = pos.getMinBlockZ(); z <= pos.getMaxBlockZ(); z++) {
                int root = 180 - (int) Math.floor(Math.max(0, shape.distanceBeyondFlat(x, z)));
                if (region.getBlockState(new BlockPos(x, root, z)).is(Blocks.GRASS)) { grass++; if (x < pos.getMinBlockX() + 4) edgeGrass++; }
            }
            h.assertTrue(grass >= 40 && grass <= 100 && edgeGrass >= 8,
                    "Both the first four wall columns and ramp columns must retain normal grass density under canopies: " + ramp + "/" + grass + "/" + edgeGrass);
            var patch = new PlainsFlowers.FlowerPatch(pos.getMinBlockX() + 1, pos.getMinBlockZ() + 1, 41);
            h.assertTrue(PlainsFlowers.placePatch(region, patch) == 5 && PlainsFlowers.placePatch(region, patch) == 0,
                    "Regular flowers must grow under forest/ramp canopies and repeated decoration must not double a patch");
            if (!ramp) {
                var root = pos.getMiddleBlockPosition(180);
                h.assertTrue(StructurePlantExclusion.excludesPatch(region, root.getX(), root.getZ())
                                && !PlantGenerationSupport.isAllowed(region, root, Blocks.PUMPKIN.defaultBlockState())
                                && PlantGenerationSupport.isAllowed(region, root.below(7), Blocks.PUMPKIN.defaultBlockState()),
                        "The forest must exclude wild crops while preserving authored buried basement crops");
            }
        }
        h.succeed();
    }

    private static WorldGenRegion region(ServerLevel level, ChunkPos pos) {
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(ResourceKey.create(Registries.BIOME, AOEMod.makeID("plains")));
        var chunk = new ProtoChunk(pos, UpgradeData.EMPTY, level, level.registryAccess().registryOrThrow(Registries.BIOME), null);
        chunk.fillBiomesFromNoise((x, y, z, sampler) -> biome, level.getChunkSource().randomState().sampler());
        chunk.setStatus(ChunkStatus.CARVERS);
        return new WorldGenRegion(level, List.of(chunk), ChunkStatus.FEATURES, 0);
    }
}
