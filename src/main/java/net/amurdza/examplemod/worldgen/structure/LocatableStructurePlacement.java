package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;

import java.util.List;

/** Candidate enumeration for placements that vanilla's structure locator cannot search. */
public interface LocatableStructurePlacement {
    /** Radius is measured in placement cells, as with vanilla random-spread searches. */
    List<ChunkPos> locateCandidates(ChunkGeneratorStructureState state, BlockPos origin, int radius);
}
