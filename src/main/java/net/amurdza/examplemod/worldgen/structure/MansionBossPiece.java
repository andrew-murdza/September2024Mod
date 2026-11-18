package net.amurdza.examplemod.worldgen.structure;

import net.amurdza.examplemod.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public final class MansionBossPiece extends StructurePiece {
    private boolean spawned;
    private BlockPos keyAnchor;
    private java.util.UUID encounterId;
    private int defenderCount;
    private int defenderIndex;

    public void encounter(java.util.UUID id, int expected, int index) {
        encounterId = id; defenderCount = expected; defenderIndex = index;
    }

    public MansionBossPiece(BoundingBox room, BlockPos keyAnchor) {
        super(ModStructures.MANSION_BOSS_PIECE.get(), 0, new BoundingBox(room.minX(), room.minY(), room.minZ(),
                room.maxX(), room.maxY(), room.maxZ()));
        this.keyAnchor = keyAnchor;
    }
    public MansionBossPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.MANSION_BOSS_PIECE.get(), tag);
        spawned = tag.getBoolean("LordPlaced");
        if (tag.contains("KeyAnchor")) keyAnchor = BlockPos.of(tag.getLong("KeyAnchor"));
        if (tag.hasUUID("EncounterId")) encounter(tag.getUUID("EncounterId"), tag.getInt("DefenderCount"), tag.getInt("DefenderIndex"));
    }
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putBoolean("LordPlaced", spawned);
        if (keyAnchor != null) tag.putLong("KeyAnchor", keyAnchor.asLong());
        if (encounterId != null) {
            tag.putUUID("EncounterId", encounterId); tag.putInt("DefenderCount", defenderCount); tag.putInt("DefenderIndex", defenderIndex);
        }
    }
    @Override public void move(int x, int y, int z) {
        super.move(x, y, z);
        if (keyAnchor != null) keyAnchor = keyAnchor.offset(x, y, z);
    }
    @Override public synchronized void postProcess(WorldGenLevel level, StructureManager manager, ChunkGenerator generator,
                                      RandomSource random, BoundingBox chunk, ChunkPos chunkPos, BlockPos pivot) {
        if (spawned) return;
        BlockPos center = boundingBox.getCenter();
        // Only the room-center chunk owns the spawn. All inspected blocks belong
        // to that chunk, avoiding generation-order dependence and duplicate lords.
        if (!chunk.isInside(new BlockPos(center.getX(), boundingBox.minY(), center.getZ()))) return;
        if (encounterId != null) {
            MansionDefenderSpawns.get(level.getLevel()).plan(level.getLevel(), new MansionDefenderSpawns.Plan(encounterId, keyAnchor,
                    defenderCount, defenderIndex, "aoemod:illager_lord", center.atY(boundingBox.minY() + 1), boundingBox));
            if (!AuthoredEncounterProgress.get(level.getLevel()).canPlaceDefender(encounterId, defenderIndex)) { spawned = true; return; }
        }
        for (int radius = 0; radius <= 6; radius++) {
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                for (int y = boundingBox.minY() + 1; y <= boundingBox.maxY() - 3; y++) {
                    BlockPos feet = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
                    if (!chunk.isInside(feet) || !boundingBox.isInside(feet)) continue;
                    if (level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)
                            && level.isEmptyBlock(feet) && level.isEmptyBlock(feet.above()) && level.isEmptyBlock(feet.above(2))) {
                        spawned = BossLairPiece.spawn(level, "aoemod:illager_lord", feet, lord -> {
                            if (encounterId != null) {
                                lord.setUUID(MansionDefenderSpawns.entityId(encounterId, defenderIndex));
                                AuthoredEncounterProgress.markDefender(lord, encounterId, keyAnchor, defenderCount, "mansion", defenderIndex);
                            }
                        });
                        return;
                    }
                }
            }
        }
    }
}
