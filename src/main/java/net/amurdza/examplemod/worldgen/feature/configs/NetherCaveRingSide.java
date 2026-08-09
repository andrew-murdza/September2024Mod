package net.amurdza.examplemod.worldgen.feature.configs;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.worldgen.NetherCaveSectionClassifier;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;

public enum NetherCaveRingSide implements StringRepresentable {
    ANY("any"),
    INNER_NETHER_CAVE_RING("inner_nether_cave_ring"),
    OUTER_NETHER_CAVE_RING("outer_nether_cave_ring");

    public static final Codec<NetherCaveRingSide> CODEC = StringRepresentable.fromEnum(NetherCaveRingSide::values);

    private final String serializedName;

    NetherCaveRingSide(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }

    public boolean allows(BlockPos pos) {
        return allows(pos.getX(), pos.getZ());
    }

    public boolean allows(int blockX, int blockZ) {
        if (this == ANY) {
            return true;
        }

        boolean inner = NetherCaveSectionClassifier.isOnApproximateInnerSideOfRiver(blockX, blockZ);
        return this == INNER_NETHER_CAVE_RING ? inner : !inner;
    }
}
