package net.amurdza.examplemod.worldgen.structure;

import net.amurdza.examplemod.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A saved, fixed population distributed across the mansion's actual furnished rooms. */
public final class MansionResidentsPiece extends StructurePiece {
    public static final int EVOKERS = 15, WITCHES = 15, PILLAGERS = 65, VINDICATORS = 65;
    public static final int RESIDENTS = EVOKERS + WITCHES + PILLAGERS + VINDICATORS;
    public static final int DEFENDERS = RESIDENTS + 1;
    public record Resident(int index, String type, BlockPos preferred, BoundingBox room) { }
    private final List<Resident> residents;
    private final UUID encounter;
    private BlockPos anchor;

    public MansionResidentsPiece(List<BoundingBox> rooms, UUID encounter, BlockPos anchor) {
        super(ModStructures.MANSION_RESIDENTS_PIECE.get(), 0, BoundingBox.encapsulatingBoxes(rooms).orElseThrow());
        this.encounter = encounter;
        this.anchor = anchor;
        residents = new ArrayList<>();
        // Interleave storeys so each type occupies the whole building.
        var ordered = rooms.stream().sorted(java.util.Comparator.comparingInt(BoundingBox::minX)
                .thenComparingInt(BoundingBox::minZ).thenComparingInt(BoundingBox::minY)).toList();
        for (int i = 0; i < RESIDENTS; i++) {
            var source = ordered.get(i % ordered.size());
            var room = new BoundingBox(source.minX(), source.minY(), source.minZ(), source.maxX(), source.maxY(), source.maxZ());
            var preferred = room.getCenter().atY(room.minY() + 1);
            residents.add(new Resident(i, type(i), preferred, room));
        }
    }

    public MansionResidentsPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.MANSION_RESIDENTS_PIECE.get(), tag);
        encounter = tag.getUUID("Encounter"); anchor = BlockPos.of(tag.getLong("Anchor"));
        residents = new ArrayList<>();
        for (var value : tag.getList("Residents", Tag.TAG_COMPOUND)) {
            var member = (CompoundTag) value; int[] b = member.getIntArray("Room");
            if (b.length == 6) residents.add(new Resident(member.getInt("Index"), member.getString("Type"),
                    BlockPos.of(member.getLong("Preferred")), new BoundingBox(b[0], b[1], b[2], b[3], b[4], b[5])));
        }
    }

    public List<Resident> residents() { return List.copyOf(residents); }

    private static String type(int index) {
        if (index < EVOKERS) return "minecraft:evoker";
        if (index < EVOKERS + WITCHES) return "minecraft:witch";
        if (index < EVOKERS + WITCHES + PILLAGERS) return "minecraft:pillager";
        return "minecraft:vindicator";
    }

    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putUUID("Encounter", encounter); tag.putLong("Anchor", anchor.asLong());
        var members = new ListTag();
        for (var resident : residents) {
            var entry = new CompoundTag(); var b = resident.room();
            entry.putInt("Index", resident.index()); entry.putString("Type", resident.type());
            entry.putLong("Preferred", resident.preferred().asLong());
            entry.putIntArray("Room", new int[]{b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()}); members.add(entry);
        }
        tag.put("Residents", members);
    }

    @Override public void move(int x, int y, int z) {
        super.move(x, y, z); anchor = anchor.offset(x, y, z);
        residents.replaceAll(resident -> {
            resident.room().move(x, y, z);
            return new Resident(resident.index(), resident.type(), resident.preferred().offset(x, y, z), resident.room());
        });
    }

    @Override public void postProcess(WorldGenLevel level, StructureManager manager, ChunkGenerator generator,
            RandomSource random, BoundingBox clip, ChunkPos chunk, BlockPos pivot) {
        for (var resident : residents) if (clip.isInside(resident.preferred())) {
            var plan = new MansionDefenderSpawns.Plan(encounter, anchor, DEFENDERS, resident.index(),
                    resident.type(), resident.preferred(), resident.room());
            var serverLevel = level.getLevel();
            Runnable place = () -> {
                MansionDefenderSpawns.get(serverLevel).plan(serverLevel, plan);
                MansionDefenderSpawns.retry(serverLevel, plan);
            };
            // Chunk workers only enqueue immutable plans. Saved data and live
            // entities are accessed on the server thread after chunks are ready.
            if (level instanceof net.minecraft.server.level.ServerLevel && serverLevel.getServer().isSameThread()) place.run();
            else serverLevel.getServer().execute(place);
        }
    }
}
