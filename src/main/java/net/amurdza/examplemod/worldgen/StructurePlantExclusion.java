package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.event_handlers.MansionProtection;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.PlacedOrcVillages;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcCampPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkStatus;

/** Natural crop reservations cannot consume the gardens authored by these structures. */
public final class StructurePlantExclusion {
    private StructurePlantExclusion() { }

    public static boolean excludes(WorldGenLevel level, BlockPos pos) {
        while (level instanceof GrowthPlacementBuffer buffer) level = buffer.sourceLevel();
        if (GameplayDimensions.is(level, GameplayDimensions.PLAINS)) return MansionProtection.insideBasement(level, pos);
        if (!GameplayDimensions.is(level, GameplayDimensions.SAVANNA)) return false;
        if (level instanceof ServerLevel server && PlacedOrcVillages.get(server).contains(pos)) return true;
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var camp = registry.get(new ResourceLocation("aoemod:orc_camp"));
        if (camp == null) return false;
        if (level instanceof WorldGenRegion region) {
            var chunk = region.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.STRUCTURE_REFERENCES, false);
            if (chunk == null) return false;
            if (SavannaCampColumns.recorded(chunk)) return SavannaCampColumns.contains(chunk, pos);
            // Never route this through the ServerLevel: doing so can wait on
            // another generation task. FEATURES normally has these owners in
            // its region; SPAWN's radius-zero region uses the saved mask above.
            boolean unavailableOwner = false;
            for (long reference : chunk.getReferencesForStructure(camp)) {
                var owner = new ChunkPos(reference);
                var startChunk = region.getChunk(owner.x, owner.z, ChunkStatus.STRUCTURE_STARTS, false);
                if (startChunk == null) { unavailableOwner = true; continue; }
                var start = startChunk.getStartForStructure(camp);
                if (start != null && start.isValid()) for (var piece : start.getPieces())
                    if (piece instanceof SavannaOrcCampPiece village && village.containsSurfaceColumn(pos)) return true;
            }
            // A legacy chunk saved between FEATURES and SPAWN predates masks.
            // Only its referenced 16x16 area is conservatively reserved when
            // the original owner is unavailable. Completed old chunks use the
            // exact live structure lookup below, without this fallback.
            return unavailableOwner;
        }
        var manager = level.getLevel().structureManager();
        for (var start : manager.startsForStructure(new ChunkPos(pos), structure -> structure == camp))
            for (var piece : start.getPieces())
                if (piece instanceof SavannaOrcCampPiece village && village.containsSurfaceColumn(pos)) return true;
        return false;
    }

    public static boolean excludesPatch(WorldGenLevel level, int x, int z) {
        if (GameplayDimensions.is(level, GameplayDimensions.PLAINS)) {
            var forests = net.amurdza.examplemod.worldgen.structure.MansionForestFloor.forChunk(level, new ChunkPos(x >> 4, z >> 4));
            for (var forest : forests) for (int dx = -2; dx <= 3; dx++) for (int dz = -2; dz <= 3; dz++)
                if (forest.contains(x + dx, z + dz)) return true;
        }
        for (int dx = -2; dx <= 3; dx++) for (int dz = -2; dz <= 3; dz++) {
            int wx = x + dx, wz = z + dz;
            if (excludes(level, new BlockPos(wx, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, wx, wz), wz))) return true;
        }
        return false;
    }
}
