package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.registry.ModConfiguredFeatures;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcCampPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/** Fill each usable camp lawn chunk with a complete native acacia after ordinary vegetation. */
public final class SavannaCampTrees {
    private static final long SALT = 0x64D29613E75B08AFL;

    private SavannaCampTrees() { }

    public static void placeNatural(WorldGenLevel level, ChunkPos chunk, StructureManager structures) {
        if (!GameplayDimensions.is(level, GameplayDimensions.SAVANNA)) return;
        var camp = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(new ResourceLocation("aoemod:orc_camp"));
        if (camp == null) return;
        // Use the generation region's manager; never fetch FULL chunks from the live server.
        for (var start : structures.startsForStructure(chunk, structure -> structure == camp)) {
            for (var piece : start.getPieces()) if (piece instanceof SavannaOrcCampPiece village) {
                placeChunk(level, chunk, village);
            }
        }
    }

    public static boolean placeChunk(WorldGenLevel level, ChunkPos chunk, SavannaOrcCampPiece village) {
        if (!GameplayDimensions.is(level, GameplayDimensions.SAVANNA) || !village.isCompactSquareVillage()) return false;
        var tree = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(ModConfiguredFeatures.ACACIA_TREE.location());
        if (tree == null) return false;
        var roots = new java.util.ArrayList<BlockPos>();
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int worldX = chunk.getMinBlockX() + x, worldZ = chunk.getMinBlockZ() + z;
            // This pre-vegetation snapshot still identifies the raised ground below a tree.
            BlockPos root = new BlockPos(worldX, level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, worldX, worldZ), worldZ);
            if (!village.hasMoundColumn(root) || SavannaCampVegetation.reserved(level, root, true)) continue;
            var soil = level.getBlockState(root.below());
            if ((soil.is(Blocks.DIRT) || soil.is(Blocks.GRASS_BLOCK)) && level.getBlockState(root).is(Blocks.ACACIA_LOG)) return true;
            if (soil.is(Blocks.GRASS_BLOCK) && level.getBlockState(root).canBeReplaced()
                    && level.getFluidState(root).isEmpty() && hasLawnSpace(level, root, chunk)) roots.add(root);
        }
        RandomSource random = RandomSource.create(TropicalLayout.mix(level.getSeed() ^ chunk.toLong() ^ SALT));
        // Exhaust the available lawn, instead of losing a whole mixed chunk when its center is a road.
        for (int i = roots.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            BlockPos swap = roots.get(i); roots.set(i, roots.get(j)); roots.set(j, swap);
        }
        return SavannaCampVegetation.natural(level, () -> {
            for (BlockPos root : roots) {
                GrowthPlacementBuffer buffer = new GrowthPlacementBuffer(level);
                if (!tree.place(buffer, level.getLevel().getChunkSource().getGenerator(), random, root)) continue;
                // Keep each complete crown in its owning chunk. Later neighboring structure
                // decoration cannot cut it in half, and narrow grass margins cannot grow over roads.
                if (buffer.isValid((pos, state) -> chunk.x == pos.getX() >> 4 && chunk.z == pos.getZ() >> 4
                        && SavannaCampVegetation.allowsTree(level, pos)) && buffer.commit()) return true;
            }
            return false;
        });
    }

    private static boolean hasLawnSpace(WorldGenLevel level, BlockPos root, ChunkPos chunk) {
        // A canopy cannot fit in the one/two-block stockade margin. Reject those
        // impossible sites before asking the native tree generator for a shape.
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            BlockPos pos = root.offset(dx, 0, dz);
            if (chunk.x != pos.getX() >> 4 || chunk.z != pos.getZ() >> 4
                    || SavannaCampVegetation.reserved(level, pos, true)) return false;
        }
        return true;
    }
}
