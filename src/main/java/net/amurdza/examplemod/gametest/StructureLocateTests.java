package net.amurdza.examplemod.gametest;

import com.mojang.datafixers.util.Pair;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.amurdza.examplemod.worldgen.structure.BandStructurePlacement;
import net.amurdza.examplemod.worldgen.structure.IslandBossPlacement;
import net.amurdza.examplemod.worldgen.structure.LocatableStructurePlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StructureLocateTests {
    private StructureLocateTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 2400)
    public static void spiderLocateFindsRealStartsAndHonorsReferences(GameTestHelper helper) {
        var level = level(helper, GameplayDimensions.TROPICAL);
        var generator = level.getChunkSource().getGenerator();
        var structure = structure(level, "spider_cave");
        var requested = HolderSet.direct(structure);
        var origin = new BlockPos(-4096, 74, -4096);
        var found = generator.findNearestMapStructure(level, requested, origin, 8, false);
        assertStart(helper, level, structure, found);
        var chunk = new ChunkPos(found.getFirst());
        var layout = DimensionTerrain.tropicalLayout(level.getChunkSource().randomState().router().temperature());
        var island = layout.nearest(chunk.getMiddleBlockX(), chunk.getMiddleBlockZ());
        helper.assertTrue(chunk.equals(new ChunkPos(IslandBossPlacement.center(island))),
                "Located spider cave must match the seeded rainforest position");
        var fromCenter = generator.findNearestMapStructure(level, requested,
                new BlockPos((int) Math.floor(island.x()), 74, (int) Math.floor(island.z())), 0, false);
        helper.assertTrue(found.equals(fromCenter), "Zero-radius locate missed the cave at this island center");
        int commandResult = level.getServer().getCommands().performPrefixedCommand(
                level.getServer().createCommandSourceStack().withLevel(level)
                        .withPosition(Vec3.atCenterOf(origin)).withSuppressedOutput(),
                "locate structure aoemod:spider_cave");
        helper.assertTrue(commandResult > 0, "The real /locate command failed with its normal search radius");

        var repeated = generator.findNearestMapStructure(level, requested, origin, 8, false);
        helper.assertTrue(found.equals(repeated), "Repeated ordinary locate changed its nearest result");
        var firstUnreferenced = generator.findNearestMapStructure(level, requested, origin, 8, true);
        helper.assertTrue(found.equals(firstUnreferenced), "First exploration search skipped the unreferenced nearest cave");
        var secondUnreferenced = generator.findNearestMapStructure(level, requested, origin, 8, true);
        assertStart(helper, level, structure, secondUnreferenced);
        helper.assertTrue(!new ChunkPos(secondUnreferenced.getFirst()).equals(chunk),
                "Exploration search returned an already referenced cave");
        helper.assertTrue(found.equals(generator.findNearestMapStructure(level, requested, origin, 8, false)),
                "Ordinary locate must still find previously referenced caves");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 2400)
    public static void spiderLocateWorksAtPositiveCoordinatesAndRejectsWrongDimension(GameTestHelper helper) {
        var tropical = level(helper, GameplayDimensions.TROPICAL);
        var structure = structure(tropical, "spider_cave");
        var found = tropical.getChunkSource().getGenerator().findNearestMapStructure(tropical,
                HolderSet.direct(structure), new BlockPos(4096, 74, 4096), 8, false);
        assertStart(helper, tropical, structure, found);
        var savanna = level(helper, GameplayDimensions.SAVANNA);
        helper.assertTrue(savanna.getChunkSource().getGenerator().findNearestMapStructure(savanna,
                        HolderSet.direct(structure), BlockPos.ZERO, 8, false) == null,
                "Spider locate returned a cave outside a dimension containing its biome");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 2400)
    public static void allOtherActiveBossStructuresRemainLocatable(GameTestHelper helper) {
        var snowy = level(helper, GameplayDimensions.SNOWY);
        var yeti = structure(snowy, "yeti_cave");
        assertStart(helper, snowy, yeti, snowy.getChunkSource().getGenerator().findNearestMapStructure(
                snowy, HolderSet.direct(yeti), new BlockPos(-3190, 74, 499), 2, false));
        checkPlacement(helper, GameplayDimensions.NETHER, "wither_cave", "wither_caves");
        checkPlacement(helper, GameplayDimensions.SAVANNA, "orc_camp", "orc_camps");
        checkPlacement(helper, GameplayDimensions.PLAINS, "illager_mansion", "illager_mansions");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 100)
    public static void bandLocateCandidatesMatchGenerationAtNegativeX(GameTestHelper helper) {
        var state = level(helper, GameplayDimensions.TROPICAL).getChunkSource().getGeneratorState();
        for (double target : new double[]{0, 0.25, 0.5}) {
            var placement = new BandStructurePlacement(target, 0, 12, 16);
            var expected = new ChunkPos(-60, (int) (target * 160));
            var origin = new BlockPos(expected.getMiddleBlockX(), 74, expected.getMiddleBlockZ());
            var candidates = placement.locateCandidates(state, origin, 1);
            helper.assertTrue(candidates.contains(expected), "Band locate missed the placement at negative X: " + target);
            helper.assertTrue(new HashSet<>(candidates).size() == candidates.size(),
                    "Band locate repeated overlapping phases: " + target);
            for (var candidate : candidates) {
                helper.assertTrue(candidate.z >= 0 && placement.isStructureChunk(state, candidate.x, candidate.z),
                        "Band locate supplied a chunk that cannot generate its structure: " + candidate);
            }
        }
        // Keep the locator's floating-point arithmetic identical to generation:
        // (4 + .1) * 480 rounds below the chunk boundary, while 4 * 480 + .1 * 480 does not.
        var boundaryPlacement = new BandStructurePlacement(0.1, 0, 12, 3);
        var boundaryChunk = new ChunkPos(-60, 123);
        var boundaryOrigin = new BlockPos(boundaryChunk.getMiddleBlockX(), 74, boundaryChunk.getMiddleBlockZ());
        helper.assertTrue(boundaryPlacement.isStructureChunk(state, boundaryChunk.x, boundaryChunk.z),
                "Expected generation candidate at Z=1968");
        for (int radius : new int[]{0, 1}) {
            helper.assertTrue(boundaryPlacement.locateCandidates(state, boundaryOrigin, radius).contains(boundaryChunk),
                    "Band locate lost the Z=1968 chunk boundary at radius " + radius);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 100)
    public static void allModStructureSetsHaveLocateSupport(GameTestHelper helper) {
        var sets = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
        int checked = 0;
        for (var entry : sets.entrySet()) {
            if (!entry.getKey().location().getNamespace().equals(AOEMod.MOD_ID)) continue;
            var placement = entry.getValue().placement();
            helper.assertTrue(placement instanceof RandomSpreadStructurePlacement
                            || placement instanceof ConcentricRingsStructurePlacement
                            || placement instanceof LocatableStructurePlacement,
                    "Mod structure set has no locate support: " + entry.getKey().location());
            checked++;
        }
        helper.assertTrue(checked > 0, "No mod structure sets were audited");
        helper.succeed();
    }

    private static void checkPlacement(GameTestHelper helper, ResourceKey<Level> dimension,
                                          String structureId, String setId) {
        var level = level(helper, dimension);
        var generator = level.getChunkSource().getGenerator();
        var structure = structure(level, structureId);
        var set = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
                .get(new ResourceLocation(AOEMod.MOD_ID, setId));
        helper.assertTrue(set != null && (set.placement() instanceof RandomSpreadStructurePlacement
                        || set.placement() instanceof net.amurdza.examplemod.worldgen.structure.SavannaCampPlacement),
                "Expected a supported native or dry-divide placement for " + structureId);
        var placement = set.placement();
        for (int regionX = -8; regionX < 8; regionX++) {
            for (int regionZ = -8; regionZ < 8; regionZ++) {
                var chunk = placement instanceof RandomSpreadStructurePlacement spread
                        ? spread.getPotentialStructureChunk(level.getSeed(), regionX * spread.spacing(), regionZ * spread.spacing())
                        : new ChunkPos(net.amurdza.examplemod.worldgen.structure.SavannaCampPlacement.findCenter(
                                net.amurdza.examplemod.worldgen.structure.SavannaCampPlacement.terrainLand(level.getChunkSource().randomState()), regionX, regionZ));
                var context = new Structure.GenerationContext(level.registryAccess(), generator,
                        generator.getBiomeSource(), level.getChunkSource().randomState(), level.getStructureManager(),
                        level.getSeed(), chunk, level, structure.value().biomes()::contains);
                if (structure.value().findValidGenerationPoint(context).isEmpty()) continue;
                var found = generator.findNearestMapStructure(level, HolderSet.direct(structure),
                        new BlockPos(chunk.getMiddleBlockX(), 74, chunk.getMiddleBlockZ()), 0, false);
                assertStart(helper, level, structure, found);
                helper.assertTrue(new ChunkPos(found.getFirst()).equals(chunk),
                        "Locate returned the wrong natural placement chunk for " + structureId);
                return;
            }
        }
        helper.assertTrue(false, "No natural candidate found for " + structureId);
    }

    private static void assertStart(GameTestHelper helper, ServerLevel level, Holder<Structure> expected,
                                    Pair<BlockPos, Holder<Structure>> result) {
        helper.assertTrue(result != null, "Locate failed for " + expected.unwrapKey().orElseThrow().location());
        helper.assertTrue(result.getSecond().equals(expected), "Locate returned a different structure");
        var pos = new ChunkPos(result.getFirst());
        var chunk = level.getChunk(pos.x, pos.z, ChunkStatus.STRUCTURE_STARTS);
        var start = level.structureManager().getStartForStructure(SectionPos.bottomOf(chunk), expected.value(), chunk);
        helper.assertTrue(start != null && start.isValid(), "Locate did not resolve to a valid generated structure start");
    }

    private static Holder<Structure> structure(ServerLevel level, String id) {
        return level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolderOrThrow(
                ResourceKey.create(Registries.STRUCTURE, new ResourceLocation(AOEMod.MOD_ID, id)));
    }

    private static ServerLevel level(GameTestHelper helper, ResourceKey<Level> dimension) {
        var result = helper.getLevel().getServer().getLevel(dimension);
        helper.assertTrue(result != null, "Missing test dimension " + dimension.location());
        return result;
    }
}
