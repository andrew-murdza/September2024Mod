package net.amurdza.examplemod.worldgen.structure;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.GrowthPlacementBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;

import java.util.ArrayList;
import java.util.List;

/** Read the graded forest floor from its structure, independently of canopy heightmaps. */
public final class MansionForestFloor {
    private MansionForestFloor() { }

    public record Forest(BoundingBox house, int rootY, int layoutVersion) {
        public Forest(BoundingBox house, int rootY) { this(house, rootY, 8); }
        public boolean contains(int x, int z) {
            return new MansionEstateShape(house, layoutVersion).contains(x, z);
        }

        public Integer rootY(WorldGenLevel level, int x, int z) {
            double distance = new MansionEstateShape(house, layoutVersion).distanceBeyondFlat(x, z);
            if (distance <= 0) return rootY;
            int rampRoot = rootY - (int) Math.floor(distance);
            return rampRoot > level.getMinBuildHeight() && level.getBlockState(new BlockPos(x, rampRoot - 1, z))
                    .is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK) ? rampRoot : null;
        }
    }

    public static Integer rootY(WorldGenLevel level, int x, int z) {
        for (Forest forest : forChunk(level, new ChunkPos(x >> 4, z >> 4))) {
            Integer root = forest.rootY(level, x, z);
            if (root != null) return root;
        }
        return null;
    }

    public static List<Forest> forChunk(WorldGenLevel level, ChunkPos pos) {
        while (level instanceof GrowthPlacementBuffer buffer) level = buffer.sourceLevel();
        if (!GameplayDimensions.is(level, GameplayDimensions.PLAINS)) return List.of();
        if (!(level instanceof ServerLevel) && !(level instanceof WorldGenRegion)) return List.of();
        List<Forest> result = new ArrayList<>();
        if (level instanceof ServerLevel server) {
            for (var layout : PlacedMansions.get(server).at(pos.getMiddleBlockPosition(0)))
                BoundingBox.encapsulatingBoxes(layout.rooms()).ifPresent(house ->
                        result.add(new Forest(house, layout.basement().groundsY() + 1, layout.basement().layoutVersion())));
        }
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(AOEMod.makeID("illager_mansion"));
        if (structure == null) return result;
        if (level instanceof WorldGenRegion region) {
            var chunk = region.getChunk(pos.x, pos.z, ChunkStatus.STRUCTURE_REFERENCES, false);
            if (chunk == null) return result;
            // Only consult chunks already owned by this generation region; a ServerLevel
            // lookup here can wait on a neighboring FEATURES task indefinitely.
            for (long reference : chunk.getReferencesForStructure(structure)) {
                var owner = new ChunkPos(reference);
                var startChunk = region.getChunk(owner.x, owner.z, ChunkStatus.STRUCTURE_STARTS, false);
                if (startChunk == null) continue;
                var start = startChunk.getStartForStructure(structure);
                if (start != null && start.isValid()) add(result, start.getPieces());
            }
            for (var site : MansionRampTerrain.sitesForChunk(region, pos, region.getLevel().getChunkSource().getGenerator()))
                result.add(new Forest(site.house(), site.groundY() + 1, site.layoutVersion()));
        } else {
            for (var start : level.getLevel().structureManager().startsForStructure(pos, candidate -> candidate == structure))
                add(result, start.getPieces());
            for (var site : MansionRampTerrain.sitesForChunk((ServerLevel) level, pos, level.getLevel().getChunkSource().getGenerator()))
                result.add(new Forest(site.house(), site.groundY() + 1, site.layoutVersion()));
        }
        return result.stream().distinct().toList();
    }

    private static void add(List<Forest> result, List<StructurePiece> pieces) {
        var basement = pieces.stream().filter(MansionBasementPiece.class::isInstance)
                .map(MansionBasementPiece.class::cast).findFirst().orElse(null);
        if (basement == null) return;
        BoundingBox.encapsulatingBoxes(pieces.stream().filter(WoodlandMansionPiece.class::isInstance)
                .map(StructurePiece::getBoundingBox).toList())
                .ifPresent(house -> result.add(new Forest(house, basement.groundsY() + 1, basement.layoutVersion())));
    }
}
