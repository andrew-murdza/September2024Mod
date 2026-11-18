package net.amurdza.examplemod.worldgen.structure;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.registry.ModStructurePlacementTypes;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.amurdza.examplemod.worldgen.TropicalLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** One seeded cave at the middle radius of each rainforest band. */
public final class IslandBossPlacement extends StructurePlacement implements LocatableStructurePlacement {
    public static final Codec<IslandBossPlacement> CODEC = Codec.unit(IslandBossPlacement::new);

    public IslandBossPlacement() {
        super(Vec3i.ZERO, FrequencyReductionMethod.DEFAULT, 1, 0, Optional.empty());
    }

    public static BlockPos center(TropicalLayout.Island island) {
        double angle = (island.hash() >>> 11) * 0x1.0p-53 * 2 * Math.PI;
        double radius = (TropicalLayout.CHERRY_RADIUS + TropicalLayout.FOREST_RADIUS) / 2;
        return BlockPos.containing(island.x() + radius * Math.cos(angle), 0, island.z() + radius * Math.sin(angle));
    }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int chunkX, int chunkZ) {
        var island = DimensionTerrain.tropicalLayout(state.randomState().router().temperature())
                .nearest(chunkX * 16 + 8, chunkZ * 16 + 8);
        BlockPos center = center(island);
        return chunkX == center.getX() >> 4 && chunkZ == center.getZ() >> 4;
    }

    @Override
    public List<ChunkPos> locateCandidates(ChunkGeneratorStructureState state, BlockPos origin, int radius) {
        var layout = DimensionTerrain.tropicalLayout(state.randomState().router().temperature());
        List<ChunkPos> candidates = new ArrayList<>();
        for (var island : layout.nearbyIslands(origin.getX(), origin.getZ(), radius)) {
            var chunk = new ChunkPos(center(island));
            if (isStructureChunk(state, chunk.x, chunk.z)) candidates.add(chunk);
        }
        return candidates;
    }

    @Override public StructurePlacementType<?> type() { return ModStructurePlacementTypes.ISLAND_BOSS.get(); }
}
