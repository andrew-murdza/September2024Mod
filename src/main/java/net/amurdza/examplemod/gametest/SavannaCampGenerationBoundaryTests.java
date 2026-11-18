package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.SavannaCampColumns;
import net.amurdza.examplemod.worldgen.SavannaCampSpawnRules;
import net.amurdza.examplemod.worldgen.StructurePlantExclusion;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcCampPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SavannaCampGenerationBoundaryTests {
    private SavannaCampGenerationBoundaryTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void radiusZeroPopulationUsesExactSavedCampColumns(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        var camp = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(new ResourceLocation("aoemod:orc_camp"));
        for (int version : new int[]{3, 4, 5, 6, 7, 8, 9, 10, 11}) {
            var piece = piece(level, version);
            int radius = version >= 9 ? 96 : version == 8 ? 98 : version == 7 ? 105 : version == 6 ? 94 : version >= 4 ? 60 : 44;
            int offset = version >= 9 ? 98 : version == 8 ? 100 : version == 7 ? 107 : version == 6 ? 96 : version >= 4 ? 62 : 46;
            BlockPos center = piece.templatePosition().offset(offset, 0, offset);
            BlockPos inside = center.offset(radius - 1, 0, 8), outside = center.offset(radius + 1, 0, 8);
            ChunkPos pos = new ChunkPos(inside), remoteOwner = new ChunkPos(pos.x - 5, pos.z - 1);
            helper.assertTrue(pos.equals(new ChunkPos(outside)), "Fixture must compare both sides within one available chunk");
            ProtoChunk proto = chunk(level, pos);
            proto.addReferenceForStructure(camp, remoteOwner.toLong());
            SavannaCampColumns.record(proto, piece);
            verifyColumns(helper, level, proto, piece, inside, outside);

            // Post actual Forge events so this also checks subscriber registration.
            CompoundTag saved = new CompoundTag();
            MinecraftForge.EVENT_BUS.post(new ChunkDataEvent.Save(proto, level, saved));
            helper.assertTrue(saved.contains(SavannaCampColumns.NBT_KEY, Tag.TAG_LONG_ARRAY)
                            && saved.getLongArray(SavannaCampColumns.NBT_KEY).length <= 4,
                    "Unfinished chunks must persist their exact footprint in at most four longs");
            ProtoChunk restored = chunk(level, pos);
            restored.addReferenceForStructure(camp, remoteOwner.toLong());
            MinecraftForge.EVENT_BUS.post(new ChunkDataEvent.Load(restored, saved, ChunkStatus.ChunkType.PROTOCHUNK));
            verifyColumns(helper, level, restored, piece, inside, outside);
            LevelChunk promoted = new LevelChunk(level, restored, null);
            var wrapper = new ImposterProtoChunk(promoted, false);
            verifyColumns(helper, level, wrapper, piece, inside, outside);
            helper.assertTrue(SavannaCampColumns.recorded(promoted), "Proto promotion must copy the footprint without a disk-load event");

            // A referenced ramp/fringe chunk may have no authored surface cells.
            ProtoChunk fringe = chunk(level, new ChunkPos(center.offset(radius + 20, 0, 0)));
            fringe.addReferenceForStructure(camp, remoteOwner.toLong());
            SavannaCampColumns.record(fringe, piece);
            helper.assertTrue(SavannaCampColumns.recorded(fringe) && SavannaCampColumns.save(fringe).length == 0,
                    "A known empty footprint must be distinguishable from missing legacy metadata");
            CompoundTag empty = new CompoundTag();
            MinecraftForge.EVENT_BUS.post(new ChunkDataEvent.Save(fringe, level, empty));
            ProtoChunk emptyReload = chunk(level, fringe.getPos());
            emptyReload.addReferenceForStructure(camp, remoteOwner.toLong());
            MinecraftForge.EVENT_BUS.post(new ChunkDataEvent.Load(emptyReload, empty, ChunkStatus.ChunkType.PROTOCHUNK));
            helper.assertFalse(StructurePlantExclusion.excludes(region(helper, level, emptyReload),
                            fringe.getPos().getMiddleBlockPosition(100)),
                    "A saved empty fringe must not become conservatively blocked after reload");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void legacyPartialChunksUseOnlyAvailableStartOwners(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        var camp = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(new ResourceLocation("aoemod:orc_camp"));
        for (int version : new int[]{3, 4, 5, 6, 7, 8, 9, 10, 11}) {
            var piece = piece(level, version);
            int radius = version >= 9 ? 96 : version == 8 ? 98 : version == 7 ? 105 : version == 6 ? 94 : version >= 4 ? 60 : 44;
            int offset = version >= 9 ? 98 : version == 8 ? 100 : version == 7 ? 107 : version == 6 ? 96 : version >= 4 ? 62 : 46;
            BlockPos center = piece.templatePosition().offset(offset, 0, offset);
            BlockPos inside = center.offset(radius - 1, 0, 8), outside = inside.east(2);
            ChunkPos pos = new ChunkPos(inside);
            ProtoChunk legacy = chunk(level, pos);
            legacy.addReferenceForStructure(camp, new ChunkPos(pos.x - 5, pos.z - 1).toLong());
            var narrow = region(helper, level, legacy);
            helper.assertTrue(StructurePlantExclusion.excludes(narrow, inside),
                    "An old partial chunk with a remote camp reference must remain protected without fetching its owner");
            helper.assertFalse(StructurePlantExclusion.excludes(narrow, pos.getWorldPosition().east(16)),
                    "Legacy fallback must stay inside the available referenced chunk");

            ProtoChunk available = chunk(level, pos);
            available.addReferenceForStructure(camp, pos.toLong());
            available.setStartForStructure(camp, new StructureStart(camp, pos, 0, new PiecesContainer(List.of(piece))));
            verifyColumns(helper, level, available, piece, inside, outside);
            helper.assertFalse(StructurePlantExclusion.excludes(region(helper, level, chunk(level, pos)), inside),
                    "A chunk with neither camp metadata nor a camp reference must stay available for ordinary spawning");
        }
        helper.succeed();
    }

    private static SavannaOrcCampPiece piece(ServerLevel level, int version) {
        var context = StructurePieceSerializationContext.fromLevel(level);
        // Keep the inside/outside samples in one chunk for every footprint.
        int center = version >= 9 ? 24834 : 24832;
        var current = new SavannaOrcCampPiece(level.getStructureManager(), new BlockPos(center, 100, center));
        var tag = current.createTag(context);
        tag.putInt("SettlementVersion", version);
        int offset = version >= 9 ? 98 : version == 8 ? 100 : version == 7 ? 107 : version == 6 ? 96 : version >= 4 ? 62 : 46;
        tag.putInt("TPX", center - offset);
        tag.putInt("TPZ", center - offset);
        return new SavannaOrcCampPiece(context, tag);
    }

    private static void verifyColumns(GameTestHelper helper, ServerLevel level, ChunkAccess chunk,
            SavannaOrcCampPiece piece, BlockPos inside, BlockPos outside) {
        var region = region(helper, level, chunk);
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            BlockPos pos = chunk.getPos().getWorldPosition().offset(x, 100, z);
            helper.assertTrue(StructurePlantExclusion.excludes(region, pos) == piece.containsSurfaceColumn(pos),
                    "Radius-zero population must use the exact version-aware footprint at " + pos);
        }
        helper.assertTrue(SavannaCampSpawnRules.blocked(region, inside, EntityType.COW)
                        && SavannaCampSpawnRules.blocked(region, inside, EntityType.ZOMBIE),
                "Both livestock and undead must be excluded inside the camp without its start owner");
        helper.assertFalse(SavannaCampSpawnRules.blocked(region, outside, EntityType.COW),
                "Ordinary livestock outside the actual versioned outline must not be excluded");
    }

    private static ProtoChunk chunk(ServerLevel level, ChunkPos pos) {
        ProtoChunk chunk = new ProtoChunk(pos, UpgradeData.EMPTY, level,
                level.registryAccess().registryOrThrow(Registries.BIOME), null);
        chunk.setStatus(ChunkStatus.FEATURES);
        return chunk;
    }

    private static WorldGenRegion region(GameTestHelper helper, ServerLevel level, ChunkAccess chunk) {
        return new WorldGenRegion(level, List.of(chunk), ChunkStatus.SPAWN, -1) {
            @Override public ChunkAccess getChunk(int x, int z, ChunkStatus status, boolean create) {
                helper.assertFalse(create && !hasChunk(x, z), "Exclusion must never require an unavailable start-owner chunk");
                return super.getChunk(x, z, status, create);
            }
        };
    }
}
