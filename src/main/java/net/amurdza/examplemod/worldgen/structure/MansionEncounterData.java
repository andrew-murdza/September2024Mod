package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Coordinates are relative to the native piece so moving a planned mansion preserves its roster. */
public record MansionEncounterData(UUID id, BlockPos anchorOffset, int expected, Map<Long, Integer> indices, boolean fixedRoster) {
    public MansionEncounterData(UUID id, BlockPos anchorOffset, int expected, Map<Long, Integer> indices) {
        this(id, anchorOffset, expected, indices, false);
    }
    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putUUID("Id", id); tag.putLong("AnchorOffset", anchorOffset.asLong()); tag.putInt("Expected", expected);
        tag.putBoolean("FixedRoster", fixedRoster);
        var members = new ListTag();
        indices.forEach((pos, index) -> {
            var member = new CompoundTag(); member.putLong("Pos", pos); member.putInt("Index", index); members.add(member);
        });
        tag.put("Members", members);
        return tag;
    }

    public static MansionEncounterData load(CompoundTag tag) {
        if (!tag.hasUUID("Id")) return null;
        var indices = new HashMap<Long, Integer>();
        for (var value : tag.getList("Members", Tag.TAG_COMPOUND)) {
            var member = (CompoundTag) value; indices.put(member.getLong("Pos"), member.getInt("Index"));
        }
        return new MansionEncounterData(tag.getUUID("Id"), BlockPos.of(tag.getLong("AnchorOffset")),
                tag.getInt("Expected"), Map.copyOf(indices), tag.getBoolean("FixedRoster"));
    }
}
