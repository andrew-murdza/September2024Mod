package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.event_handlers.GoblinFortressProtection;
import net.amurdza.examplemod.worldgen.structure.GoblinFortressPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/** Natural forest-floor decoration must not pass through the fortress's inhabited ceilings. */
public final class GoblinFortressGroundCover {
    private GoblinFortressGroundCover() { }

    public static boolean beneathRoof(WorldGenLevel level, BlockPos root, int surfaceY) {
        while (level instanceof GrowthPlacementBuffer buffer) level = buffer.sourceLevel();
        boolean roof = false;
        for (int y = root.getY(); y < surfaceY; y++) {
            var pos = new BlockPos(root.getX(), y, root.getZ());
            var state = level.getBlockState(pos);
            // Forest canopies and mushroom caps still permit their ordinary
            // outdoor ground cover. Authored masonry and terrace floors do not.
            if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)
                    || state.is(Blocks.BROWN_MUSHROOM_BLOCK) || state.is(Blocks.RED_MUSHROOM_BLOCK)
                    || state.is(Blocks.MUSHROOM_STEM)) continue;
            if (state.isCollisionShapeFullBlock(level, pos)) { roof = true; break; }
        }
        if (!roof) return false;
        if (level instanceof ServerLevel server) return GoblinFortressProtection.boundaryAt(server, root) != null;

        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(new ResourceLocation("aoemod", "yeti_cave"));
        if (structure == null) return false;
        // Read only the current worldgen region. A ServerLevel structure lookup
        // here could wait for a neighboring generation task or miss the queued
        // boundary registration of a structure whose FEATURES pass is active.
        var chunk = level.getChunk(root.getX() >> 4, root.getZ() >> 4, ChunkStatus.STRUCTURE_REFERENCES, false);
        if (chunk == null) return false;
        if (contains(chunk.getStartForStructure(structure), root)) return true;
        for (long reference : chunk.getReferencesForStructure(structure)) {
            var owner = new ChunkPos(reference);
            var source = level.getChunk(owner.x, owner.z, ChunkStatus.STRUCTURE_STARTS, false);
            if (source != null && contains(source.getStartForStructure(structure), root)) return true;
        }
        return false;
    }

    private static boolean contains(StructureStart start, BlockPos pos) {
        if (start == null || !start.isValid()) return false;
        return start.getPieces().stream().anyMatch(piece -> piece instanceof GoblinFortressPiece
                && piece.getBoundingBox().isInside(pos));
    }
}
