package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.EnumSet;

/** Reconciles the interpolated seabed with the biome Minecraft actually displays at the water surface. */
public final class TropicalChannelSurface {
    public static final int WATER_SURFACE_Y = 63;
    public static final ResourceKey<Biome> MANGROVE = biome("mangrove_swamp");
    public static final ResourceKey<Biome> CORAL = biome("coral_reef");
    public static final ResourceKey<Biome> RAINFOREST = biome("rainforest");
    public static final ResourceKey<Biome> CHERRY = biome("cherry_grove");

    private TropicalChannelSurface() { }

    public static void normalize(ChunkAccess chunk, BiomeManager biomes) {
        normalize(chunk, biomes, null);
    }

    public static void normalize(ChunkAccess chunk, BiomeManager biomes, TropicalLayout layout) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean changed = false;
        for (int localX = 0; localX < 16; localX++) for (int localZ = 0; localZ < 16; localZ++) {
            int x = chunk.getPos().getMinBlockX() + localX;
            int z = chunk.getPos().getMinBlockZ() + localZ;
            var biome = biomes.getBiome(pos.set(x, WATER_SURFACE_Y, z));
            int oldBed = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, localX, localZ);
            int bed = oldBed;
            TropicalLayout.Sample sample = layout == null ? null : layout.sample(x, z);
            // Match the exact terraces before decoration: density interpolation
            // can shorten the first slope or shift a Y74/Y75 biome boundary.
            if (sample != null) bed = (int) Math.floor(sample.height() + .5);
            if (biome.is(MANGROVE)) {
                // The displayed mangrove biome has a level Y63 land strip, even at fuzzy quart edges.
                bed = bed >= WATER_SURFACE_Y ? 63 : Math.max(57, bed);
            } else if (biome.is(CORAL)) {
                bed = Math.max(55, Math.min(62, bed));
                // Voronoi biome zoom occasionally moves the six-deep mangrove
                // limit into the deepest part of the source ramp. One buffered
                // coral cell keeps that clamp from creating a two-high wall.
                if (bed == 55 && sample != null && sample.distance() < sample.mangroveEdge() + 8
                        && touchesMangrove(biomes, x, z)) bed = 56;
            }
            if (bed == oldBed) continue;

            // Surface rules have already made this profile. Preserve it when moving
            // the seabed by the few blocks introduced by quart-biome Voronoi edges.
            for (int y = Math.min(oldBed, bed) - 3; y <= Math.max(oldBed, bed); y++) {
                var state = y > WATER_SURFACE_Y && y > bed ? Blocks.AIR.defaultBlockState()
                        : y > bed ? Blocks.WATER.defaultBlockState()
                        : Blocks.MOSS_BLOCK.defaultBlockState();
                chunk.setBlockState(pos.set(x, y, z), state, false);
            }
            // Coral edges can include a column that was just above sea level.
            for (int y = bed + 1; y <= WATER_SURFACE_Y; y++) {
                chunk.setBlockState(pos.set(x, y, z), Blocks.WATER.defaultBlockState(), false);
            }
            changed = true;
        }
        if (changed) {
            Heightmap.primeHeightmaps(chunk, EnumSet.of(Heightmap.Types.WORLD_SURFACE_WG, Heightmap.Types.OCEAN_FLOOR_WG));
        }
    }

    private static boolean touchesMangrove(BiomeManager biomes, int x, int z) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            if ((dx != 0 || dz != 0) && biomes.getBiome(new BlockPos(x + dx, WATER_SURFACE_Y, z + dz)).is(MANGROVE)) return true;
        }
        return false;
    }

    private static ResourceKey<Biome> biome(String name) {
        return ResourceKey.create(Registries.BIOME, new ResourceLocation(AOEMod.MOD_ID, name));
    }
}
