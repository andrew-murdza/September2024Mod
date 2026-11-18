package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.SavannaCampColumns;
import net.amurdza.examplemod.worldgen.SavannaCampTrees;
import net.amurdza.examplemod.worldgen.SavannaCampVegetation;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcCampPiece;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcVillage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Real configured trees in detached native chunks; no live terrain needs generating. */
@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SavannaCampTreeTests {
    private static final int FLOOR = 100;

    private SavannaCampTreeTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void referencedMixedChunksGetCompleteNativeAcaciasWithoutRepeatTrees(GameTestHelper helper) {
        ServerLevel level = savanna(helper);
        for (int sample = 0; sample < 3; sample++) {
            var fixture = fixture(level, 32000 + sample * 256, 8);
            int open = 0, reserved = 0;
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                BlockPos pos = fixture.chunk().getWorldPosition().offset(x, FLOOR + 1, z);
                if (SavannaCampVegetation.reserved(fixture.region(), pos, true)) reserved++;
                else open++;
            }
            helper.assertTrue(open == 128 && reserved == 128
                            && SavannaCampVegetation.reserved(fixture.region(),
                            fixture.chunk().getMiddleBlockPosition(FLOOR + 1), true),
                    "Use the real V8 eight-wide lawn beside an eight-wide road, with its chunk center on the road");
            helper.assertTrue(fixture.piece().hasMoundColumn(fixture.chunk().getWorldPosition().offset(3, FLOOR + 1, 8)),
                    "Fixture must have the completed raised-ground plan before the final tree pass");

            var camp = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                    .get(new ResourceLocation("aoemod:orc_camp"));
            var owner = fixture.region().getChunk(fixture.chunk().x, fixture.chunk().z);
            owner.addReferenceForStructure(camp, fixture.chunk().toLong());
            owner.setStartForStructure(camp, new StructureStart(camp, fixture.chunk(), 0,
                    new PiecesContainer(List.of(fixture.piece()))));
            var manager = level.structureManager().forWorldGenRegion(fixture.region());
            SavannaCampTrees.placeNatural(fixture.region(), fixture.chunk(), manager);
            assertCompleteTree(helper, fixture);

            Map<BlockPos, BlockState> first = Map.copyOf(fixture.region().writes);
            for (int repeat = 0; repeat < 3; repeat++) {
                SavannaCampTrees.placeNatural(fixture.region(), fixture.chunk(), manager);
                helper.assertTrue(first.equals(fixture.region().writes),
                        "A replay must keep the same tree, without a second trunk or extra leaves");
            }
            long roots = fixture.region().writes.entrySet().stream()
                    .filter(entry -> entry.getKey().getY() == FLOOR + 1 && entry.getValue().is(Blocks.ACACIA_LOG)).count();
            helper.assertTrue(roots == 1, "Each newly filled lawn chunk must have exactly one rooted tree");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void nativeCrownCannotLeavePartialTreesAcrossProtectedPlantingColumns(GameTestHelper helper) {
        ServerLevel level = savanna(helper);
        var blocked = fixture(level, 33024, 8);
        var control = fixture(level, 33024, 8);
        // Model an otherwise valid lawn pocket between the four protected kinds.
        // Five columns pass the cheap lawn-space check, but cannot fit the
        // native seven-wide crown. Reject whole shapes instead of clipping leaves.
        var masks = new BitSet(512);
        var before = new LinkedHashMap<BlockPos, BlockState>();
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            BlockPos floor = blocked.chunk().getWorldPosition().offset(x, FLOOR, z);
            boolean pocket = x >= 5 && x <= 9 && z >= 5 && z <= 9;
            BlockState soil = pocket ? Blocks.GRASS_BLOCK.defaultBlockState()
                    : x < 5 ? Blocks.GRASS_BLOCK.defaultBlockState()
                    : x > 9 ? Blocks.DIRT_PATH.defaultBlockState()
                    : z < 5 ? Blocks.PODZOL.defaultBlockState() : Blocks.FARMLAND.defaultBlockState();
            blocked.region().getChunk(floor).setBlockState(floor, soil, false);
            control.region().getChunk(floor).setBlockState(floor, soil, false);
            before.put(floor, soil);
            if (!pocket) {
                masks.set(256 + z * 16 + x);
                // Bare grass represents a pen: groundcover allowed, trees forbidden.
                if (!soil.is(Blocks.GRASS_BLOCK)) masks.set(z * 16 + x);
            }
        }
        SavannaCampColumns.loadVegetation(blocked.region().getChunk(blocked.chunk().x, blocked.chunk().z), masks.toLongArray());
        SavannaCampColumns.loadVegetation(control.region().getChunk(control.chunk().x, control.chunk().z), new long[0]);
        helper.assertTrue(SavannaCampTrees.placeChunk(control.region(), control.chunk(), control.piece()),
                "The actual native acacia must grow in the same physical fixture when its reservation mask is absent");
        helper.assertTrue(control.region().writes.values().stream().anyMatch(state -> state.is(Blocks.ACACIA_LEAVES)),
                "Control must exercise a real crown, not a stub or synthetic tree");
        blocked.region().canopyReads = 0;
        helper.assertFalse(SavannaCampTrees.placeChunk(blocked.region(), blocked.chunk(), blocked.piece()),
                "No complete native crown fits the five-wide lawn between protected columns");
        helper.assertTrue(blocked.region().canopyReads > 0,
                "Fixture must pass the ground-space prefilter and exercise native above-ground tree generation");
        helper.assertTrue(blocked.region().writes.isEmpty(),
                "Rejected trees must not leave logs, clipped leaves, or changed soil");
        before.forEach((pos, state) -> helper.assertTrue(blocked.region().getBlockState(pos).equals(state),
                "Tree attempts changed protected pen/path/podzol/farmland at " + pos));
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void savedVersionSevenCampsDoNotAcquireTheNewTreePass(GameTestHelper helper) {
        ServerLevel level = savanna(helper);
        var fixture = fixture(level, 34048, 7);
        var context = StructurePieceSerializationContext.fromLevel(level);
        var before = fixture.piece().createTag(context);
        helper.assertTrue(before.getInt("SettlementVersion") == 7 && !fixture.piece().isCompactSquareVillage(),
                "Fixture must reload an actual saved V7 piece through legacy template selection");
        helper.assertFalse(SavannaCampTrees.placeChunk(fixture.region(), fixture.chunk(), fixture.piece()),
                "V7 camps must retain their existing vegetation instead of gaining the V8 guaranteed tree pass");
        helper.assertTrue(fixture.region().writes.isEmpty() && fixture.piece().createTag(context).equals(before),
                "Skipping a legacy camp must preserve its blocks and saved population bits");
        helper.succeed();
    }

    private static void assertCompleteTree(GameTestHelper helper, Fixture fixture) {
        int logs = 0, leaves = 0, lowest = Integer.MAX_VALUE, highest = Integer.MIN_VALUE;
        for (var entry : fixture.region().writes.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState state = entry.getValue();
            helper.assertTrue(new ChunkPos(pos).equals(fixture.chunk()), "A complete crown crossed its owning chunk at " + pos);
            helper.assertFalse(SavannaCampVegetation.reserved(fixture.region(), pos, true),
                    "A trunk, branch, leaf or soil change entered a reserved camp column at " + pos);
            if (state.is(Blocks.ACACIA_LOG)) {
                logs++;
                lowest = Math.min(lowest, pos.getY()); highest = Math.max(highest, pos.getY());
            } else if (state.is(Blocks.ACACIA_LEAVES)) leaves++;
        }
        helper.assertTrue(logs >= 9 && leaves >= 30 && lowest == FLOOR + 1 && highest - lowest >= 6,
                "Expected a complete native tall acacia, logs=" + logs + ", leaves=" + leaves + ", height=" + (highest - lowest));
    }

    private static Fixture fixture(ServerLevel level, int center, int version) {
        var piece = new SavannaOrcCampPiece(level.getStructureManager(), new BlockPos(center, FLOOR - 2, center));
        if (version == 8) {
            // Native postProcess normally supplies this before final decoration.
            // These detached chunks provide that same flat plan without placing
            // a complete camp and its actors for a single tree contract test.
            try {
                var field = SavannaOrcCampPiece.class.getDeclaredField("groundPlan");
                field.setAccessible(true);
                field.set(piece, SavannaOrcVillage.planGround(FLOOR, (x, z) -> FLOOR));
            } catch (ReflectiveOperationException failure) {
                throw new AssertionError("Cannot initialize the completed camp fixture", failure);
            }
        }
        if (version != 8) {
            var context = StructurePieceSerializationContext.fromLevel(level);
            var saved = piece.createTag(context);
            saved.putInt("SettlementVersion", version);
            piece = new SavannaOrcCampPiece(context, saved);
        }
        ChunkPos target = new ChunkPos((center >> 4) + 5, (center >> 4) + 4);
        var chunks = new ArrayList<ChunkAccess>();
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                net.minecraft.resources.ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod:savanna")));
        for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++) {
            ChunkPos pos = new ChunkPos(target.x + dx, target.z + dz);
            ProtoChunk chunk = new ProtoChunk(pos, UpgradeData.EMPTY, level,
                    level.registryAccess().registryOrThrow(Registries.BIOME), null);
            chunk.fillBiomesFromNoise((x, y, z, sampler) -> biome, level.getChunkSource().randomState().sampler());
            chunk.setStatus(ChunkStatus.SURFACE);
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                BlockPos floor = pos.getWorldPosition().offset(x, FLOOR, z);
                chunk.setBlockState(floor.below(), Blocks.DIRT.defaultBlockState(), false);
                var soil = SavannaCampVegetation.reserved(piece, floor, false) ? Blocks.DIRT_PATH : Blocks.GRASS_BLOCK;
                chunk.setBlockState(floor, soil.defaultBlockState(), false);
            }
            chunk.setStatus(ChunkStatus.CARVERS);
            SavannaCampColumns.record(chunk, piece);
            chunks.add(chunk);
        }
        var region = new RecordingRegion(level, chunks);
        // Freeze the raised-floor snapshots before native tree writes, matching FEATURES.
        for (var chunk : chunks) SavannaCampVegetation.prepareMoundChunk(region, chunk.getPos());
        return new Fixture(region, target, piece);
    }

    private static ServerLevel savanna(GameTestHelper helper) {
        return helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
    }

    private record Fixture(RecordingRegion region, ChunkPos chunk, SavannaOrcCampPiece piece) { }

    private static final class RecordingRegion extends WorldGenRegion {
        private final Map<BlockPos, BlockState> writes = new LinkedHashMap<>();
        private int canopyReads;

        private RecordingRegion(ServerLevel level, List<ChunkAccess> chunks) {
            super(level, chunks, ChunkStatus.FEATURES, 1);
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            if (pos.getY() > FLOOR + 4) canopyReads++;
            return super.getBlockState(pos);
        }

        @Override
        public boolean setBlock(BlockPos pos, BlockState state, int flags, int recursion) {
            boolean result = super.setBlock(pos, state, flags, recursion);
            if (result) writes.put(pos.immutable(), state);
            return result;
        }
    }
}
