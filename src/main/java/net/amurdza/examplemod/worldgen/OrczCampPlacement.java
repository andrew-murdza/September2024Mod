package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcCampPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraftforge.registries.ForgeRegistries;

/** Command placement must not roll Orcz's natural-generation rarity and terrain checks. */
public final class OrczCampPlacement {
    private static final ResourceLocation SETUP = new ResourceLocation("orcz", "camp_setup_big");
    private static final ResourceLocation GENERATOR = new ResourceLocation("orcz", "camp_structure_generator");

    private OrczCampPlacement() { }

    public static boolean handles(ServerLevel level, ConfiguredFeature<?, ?> feature) {
        if (!level.dimension().equals(Level.OVERWORLD) && !GameplayDimensions.is(level, GameplayDimensions.SAVANNA)) return false;
        ResourceLocation id = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getKey(feature);
        return id != null && id.getNamespace().equals("orcz")
                && (id.getPath().equals("camp") || id.getPath().equals("camp_the_other"));
    }

    public static boolean place(ServerLevel level, BlockPos pos, RandomSource random) {
        if (GameplayDimensions.is(level, GameplayDimensions.SAVANNA)) {
            if (level.getStructureManager().get(SavannaOrcCampPiece.TEMPLATE).isEmpty()) return false;
            // Keep the command camp's existing mound, then raise its floor by
            // the two additional blocks requested for the expanded village.
            var piece = new SavannaOrcCampPiece(level.getStructureManager(), pos.above(4));
            net.amurdza.examplemod.worldgen.structure.PlacedOrcVillages.get(level).record(pos.above(6));
            var bounds = piece.getBoundingBox();
            for (int x = Math.floorDiv(bounds.minX(), 16); x <= Math.floorDiv(bounds.maxX(), 16); x++) {
                for (int z = Math.floorDiv(bounds.minZ(), 16); z <= Math.floorDiv(bounds.maxZ(), 16); z++) {
                    var chunk = new ChunkPos(x, z);
                    level.getChunk(x, z);
                    var clip = new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                            chunk.getMaxBlockX(), level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ());
                    piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), random,
                            clip, chunk, pos);
                }
            }
            SavannaCampVegetation.decoratePlaced(level, piece);
            return true;
        }
        var template = level.getStructureManager().get(SETUP).orElse(null);
        var generator = ForgeRegistries.BLOCKS.getValue(GENERATOR);
        if (template == null || generator == null) return false;
        var settings = new StructurePlaceSettings().setRandom(random)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK).setIgnoreEntities(false);
        if (!template.placeInWorld(level, pos, pos, settings, random, 2)) return false;
        // The native setup is only a 3x3 marker. Trigger its normal expansion immediately,
        // including the biome-specific camp, loot and inhabitants, even when placed in air.
        for (var marker : template.filterBlocks(pos, settings, generator)) {
            if (level.getBlockState(marker.pos()).is(generator)) {
                level.neighborChanged(marker.pos(), generator, marker.pos());
            }
        }
        return true;
    }
}
