package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.AtomicTreeGrowth;
import net.amurdza.examplemod.worldgen.DocumentedFloraFeature;
import net.amurdza.examplemod.worldgen.GrowthPlacementBuffer;
import net.amurdza.examplemod.worldgen.SavannaCampColumns;
import net.amurdza.examplemod.worldgen.SavannaCampVegetation;
import net.amurdza.examplemod.worldgen.StructurePlantExclusion;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcCampPiece;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcVillage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SavannaCampVegetationTests {
    private SavannaCampVegetationTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void nativeFeaturesSnapshotRaisedMoundBeforeTreesAcrossChunkEdges(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        ChunkPos center = new ChunkPos(1800, 1800);
        var chunks = new ArrayList<ChunkAccess>();
        for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++) {
            var chunk = proto(level, new ChunkPos(center.x + dx, center.z + dz));
            chunk.setStatus(ChunkStatus.SURFACE);
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                chunk.setBlockState(chunk.getPos().getWorldPosition().offset(x, 70, z), Blocks.GRASS_BLOCK.defaultBlockState(), false);
            }
            // This is the actual pre-FEATURES status. Vanilla now updates only
            // the live maps, leaving both generation maps at the old surface.
            chunk.setStatus(ChunkStatus.CARVERS);
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                chunk.setBlockState(chunk.getPos().getWorldPosition().offset(x, 90, z), Blocks.GRASS_BLOCK.defaultBlockState(), false);
            }
            helper.assertTrue(chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, 0, 0) == 70
                            && chunk.getHeight(Heightmap.Types.WORLD_SURFACE, 0, 0) == 90,
                    "Fixture must reproduce the stale native FEATURES snapshot");
            chunks.add(chunk);
        }
        var region = new WorldGenRegion(level, chunks, ChunkStatus.FEATURES, 1);
        for (var chunk : chunks) SavannaCampVegetation.prepareMoundChunk(region, chunk.getPos());
        BlockPos west = center.getWorldPosition().offset(15, 91, 8), east = west.east();
        for (BlockPos pos : List.of(west, east)) {
            helper.assertTrue(region.getHeight(Heightmap.Types.WORLD_SURFACE_WG, pos.getX(), pos.getZ()) == 91
                            && region.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, pos.getX(), pos.getZ()) == 91,
                    "Both sides of a chunk boundary must sample the raised mound before vegetation at " + pos);
            helper.assertTrue(Blocks.GRASS.defaultBlockState().canSurvive(region, pos),
                    "The native grass origin must now find its raised support");
            region.setBlock(pos.above(4), Blocks.ACACIA_LOG.defaultBlockState(), 2);
            helper.assertTrue(region.getHeight(Heightmap.Types.WORLD_SURFACE_WG, pos.getX(), pos.getZ()) == 91
                            && region.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ()) == 96,
                    "Later trees must retain the pre-tree WG snapshot, just like surrounding natural terrain");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void plantingMasksSurviveRadiusZeroReloadWithoutChangingCampExclusion(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        for (int version : new int[]{5, 6, 7, 8}) {
            BlockPos origin = new BlockPos(28672, 188, 28672);
            var piece = piece(level, origin, version);
            ChunkPos chunkPos = new ChunkPos(origin.offset(16, 0, version == 5 ? 32 : 64));
            var camp = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(new ResourceLocation("aoemod:orc_camp"));
            helper.assertTrue(camp.step().ordinal() < GenerationStep.Decoration.VEGETAL_DECORATION.ordinal(),
                    "The complete mound and authored template must precede biome vegetation");
            ProtoChunk chunk = proto(level, chunkPos);
            chunk.addReferenceForStructure(camp, new ChunkPos(chunkPos.x - 5, chunkPos.z - 1).toLong());
            SavannaCampColumns.record(chunk, piece);
            CompoundTag saved = new CompoundTag();
            MinecraftForge.EVENT_BUS.post(new ChunkDataEvent.Save(chunk, level, saved));
            helper.assertTrue(saved.getLongArray(SavannaCampColumns.VEGETATION_NBT_KEY).length <= 8,
                    "The two planting masks should use at most eight longs per chunk");
            ProtoChunk loaded = proto(level, chunkPos);
            loaded.addReferenceForStructure(camp, new ChunkPos(chunkPos.x - 5, chunkPos.z - 1).toLong());
            MinecraftForge.EVENT_BUS.post(new ChunkDataEvent.Load(loaded, saved, ChunkStatus.ChunkType.PROTOCHUNK));
            for (ChunkAccess candidate : List.of(chunk, loaded, new ImposterProtoChunk(new LevelChunk(level, loaded, null), false))) {
                var region = new WorldGenRegion(level, List.of(candidate), ChunkStatus.SPAWN, -1) {
                    @Override public ChunkAccess getChunk(int x, int z, ChunkStatus status, boolean create) {
                        helper.assertFalse(create && !hasChunk(x, z), "Planting must never fetch an unavailable start owner");
                        return super.getChunk(x, z, status, create);
                    }
                };
                var buffered = new GrowthPlacementBuffer(new GrowthPlacementBuffer(region));
                int groundcover = 0, protectedCells = 0;
                for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                    BlockPos pos = chunkPos.getWorldPosition().offset(x, 191, z);
                    for (boolean tree : new boolean[]{false, true}) {
                        helper.assertTrue(SavannaCampVegetation.reserved(buffered, pos, tree)
                                        == SavannaCampVegetation.reserved(piece, pos, tree),
                                "Loaded radius-zero region lost its exact planting mask at " + pos + ", tree=" + tree);
                    }
                    if (SavannaCampVegetation.reserved(piece, pos, false)) protectedCells++;
                    else if (SavannaCampVegetation.reserved(piece, pos, true)) {
                        groundcover++;
                        helper.assertTrue(StructurePlantExclusion.excludes(buffered, pos),
                                "Allowing pen grass must not admit wild crops or ambient camp spawns");
                    }
                }
                helper.assertTrue(groundcover > 0 && protectedCells > 0, "Fixture needs both pen lawn and authored columns");
            }
            var context = StructurePieceSerializationContext.fromLevel(level);
            var oldTag = piece.createTag(context);
            oldTag.putInt("SettlementVersion", 4);
            oldTag.putString("Template", "aoemod:orc_village");
            var oldPiece = new SavannaOrcCampPiece(context, oldTag);
            ProtoChunk oldChunk = proto(level, chunkPos);
            SavannaCampColumns.record(oldChunk, oldPiece);
            helper.assertFalse(SavannaCampColumns.vegetationRecorded(oldChunk),
                    "Older authored villages must retain their existing planting behavior");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void nativeGrassAndFlowersUseRaisedPenLawnsAndPreserveAuthoredColumns(GameTestHelper helper) {
        var fixture = lawn(helper, new BlockPos(29184, 188, 29184));
        var level = fixture.level();
        SavannaCampVegetation.prepareDecorationChunk(level, fixture.chunk());
        BlockPos raised = fixture.penLawn().get(0);
        helper.assertTrue(level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, raised.getX(), raised.getZ()) == raised.getY()
                        && level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, raised.getX(), raised.getZ()) == raised.getY(),
                "Repeated command decoration must refresh previously cached heightmaps to the newly raised floor");
        var grass = level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE)
                .get(new ResourceLocation("aoemod:savanna/grass"));
        helper.assertTrue(SavannaCampVegetation.isVegetationFeature(level, grass), "Use the actual native biome grass feature");
        var ore = level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE).get(new ResourceLocation("minecraft:ore_coal_upper"));
        helper.assertFalse(SavannaCampVegetation.isVegetationFeature(level, ore), "Camp planting masks must not change underground ores");
        for (int attempt = 0; attempt < 12; attempt++) {
            grass.placeWithBiomeCheck(level, level.getChunkSource().getGenerator(), RandomSource.create(401 + attempt),
                    fixture.chunk().getWorldPosition());
        }
        long penGrass = fixture.penLawn().stream().filter(pos -> level.getBlockState(pos).is(Blocks.GRASS)
                || level.getBlockState(pos).is(Blocks.TALL_GRASS)).count();
        helper.assertTrue(penGrass > 0, "Native grass must find the raised pen floor, not the old noise surface");
        for (BlockPos pos : fixture.authored()) helper.assertTrue(level.getBlockState(pos).isAir(),
                "Native grass entered an authored column at " + pos);
        BlockPos pen = fixture.penLawn().get(0), authored = fixture.authored().get(0);
        level.setBlock(pen.above(), Blocks.AIR.defaultBlockState(), 18);
        level.setBlock(pen, Blocks.AIR.defaultBlockState(), 18);
        SavannaCampVegetation.natural(level, () -> {
            helper.assertTrue(DocumentedFloraFeature.placePlant(level, pen, "minecraft:red_tulip", RandomSource.create(8)),
                    "Native flowers must remain allowed on pen grass");
            helper.assertFalse(DocumentedFloraFeature.placePlant(level, authored, "minecraft:red_tulip", RandomSource.create(8)),
                    "Native flowers must not grow in authored columns");
            return true;
        });
        helper.assertTrue(level.getBlockState(pen).is(Blocks.RED_TULIP), "The permitted pen flower must persist");
        helper.assertTrue(level.setBlock(authored, Blocks.RED_TULIP.defaultBlockState(), 2),
                "Feature protection must end before later player or template writes");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void entireTreeRollsBackWhenItsBranchesEnterAPen(GameTestHelper helper) {
        var fixture = lawn(helper, new BlockPos(29696, 188, 29696));
        var level = fixture.level();
        BlockPos pen = fixture.penLawn().get(0), open = fixture.openLawn().get(0);
        var context = new FeaturePlaceContext<>(Optional.empty(), level, level.getChunkSource().getGenerator(),
                RandomSource.create(177), open, NoneFeatureConfiguration.INSTANCE);
        boolean placed = SavannaCampVegetation.natural(level, () -> AtomicTreeGrowth.place(context, staged -> {
            staged.level().setBlock(open, Blocks.ACACIA_LOG.defaultBlockState(), 2);
            staged.level().setBlock(pen.above(3), Blocks.ACACIA_LEAVES.defaultBlockState(), 2);
            return true;
        }));
        helper.assertFalse(placed, "A branch crossing a pen must reject the entire tree");
        helper.assertTrue(level.getBlockState(open).isAir() && level.getBlockState(pen.above(3)).isAir(),
                "Rejected tree attempts must not leave a trunk or detached canopy");
        boolean allowed = SavannaCampVegetation.natural(level, () -> AtomicTreeGrowth.place(context, staged -> {
            staged.level().setBlock(open, Blocks.ACACIA_LOG.defaultBlockState(), 2);
            staged.level().setBlock(open.above(), Blocks.ACACIA_LEAVES.defaultBlockState(), 2);
            return true;
        }));
        helper.assertTrue(allowed && level.getBlockState(open).is(Blocks.ACACIA_LOG),
                "Open camp lawn must remain eligible for complete tree growth");
        helper.succeed();
    }

    private static Lawn lawn(GameTestHelper helper, BlockPos origin) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        var piece = piece(level, origin, 7);
        ChunkPos chunk = new ChunkPos(origin.offset(16, 0, 64));
        var pen = new ArrayList<BlockPos>();
        var authored = new ArrayList<BlockPos>();
        var open = new ArrayList<BlockPos>();
        // Cache the old maps before rebuilding, as a previous /place feature can do.
        level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, chunk.getMinBlockX(), chunk.getMinBlockZ());
        level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, chunk.getMinBlockX(), chunk.getMinBlockZ());
        // One elevated real chunk exercises normal heightmaps and the setBlock mixins.
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            BlockPos pos = chunk.getWorldPosition().offset(x, origin.getY() + 3, z);
            level.setBlock(pos.below(2), Blocks.DIRT.defaultBlockState(), 18);
            level.setBlock(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 18);
            for (int dy = 0; dy <= 10; dy++) level.setBlock(pos.above(dy), Blocks.AIR.defaultBlockState(), 18);
            if (SavannaCampVegetation.reserved(piece, pos, false)) authored.add(pos);
            else if (SavannaCampVegetation.reserved(piece, pos, true)) pen.add(pos);
            else open.add(pos);
        }
        SavannaCampColumns.record(level.getChunk(chunk.x, chunk.z), piece);
        helper.assertTrue(!pen.isEmpty() && !authored.isEmpty() && !open.isEmpty(), "Lawn fixture needs all three planting zones");
        return new Lawn(level, chunk, pen, authored, open);
    }

    private static SavannaOrcCampPiece piece(ServerLevel level, BlockPos origin, int version) {
        var current = new SavannaOrcCampPiece(level.getStructureManager(), origin.offset(SavannaOrcVillage.CENTER, 0, SavannaOrcVillage.CENTER));
        if (version == 8) return current;
        var context = StructurePieceSerializationContext.fromLevel(level);
        var saved = current.createTag(context);
        saved.putInt("SettlementVersion", version);
        return new SavannaOrcCampPiece(context, saved);
    }

    private static ProtoChunk proto(ServerLevel level, ChunkPos pos) {
        var chunk = new ProtoChunk(pos, UpgradeData.EMPTY, level, level.registryAccess().registryOrThrow(Registries.BIOME), null);
        chunk.setStatus(ChunkStatus.FEATURES);
        return chunk;
    }

    private record Lawn(ServerLevel level, ChunkPos chunk, List<BlockPos> penLawn,
                        List<BlockPos> authored, List<BlockPos> openLawn) { }
}
