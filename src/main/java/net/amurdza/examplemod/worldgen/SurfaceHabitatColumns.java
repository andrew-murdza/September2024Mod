package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

/** Exact habitat chosen from the finished noise terrain before foliage, snow or player builds. */
public interface SurfaceHabitatColumns {
    String TAG = "aoemod:surface_habitats";
    List<ResourceKey<Biome>> HABITATS = List.of("snowy_forest", "dappled_forest", "cold_ocean",
            "desert", "desert_lake", "savanna", "savanna_lake", "plains", "plains_lake").stream()
            .map(id -> ResourceKey.create(Registries.BIOME, AOEMod.makeID(id))).toList();

    byte[] aoemod$getHabitats();
    void aoemod$setHabitats(byte[] values);

    static SurfaceHabitatColumns data(ChunkAccess chunk) {
        return (SurfaceHabitatColumns) (chunk instanceof ImposterProtoChunk proxy ? proxy.getWrapped() : chunk);
    }

    static boolean mapped(ResourceKey<Level> dimension) {
        return dimension.equals(GameplayDimensions.SNOWY) || dimension.equals(GameplayDimensions.DESERT)
                || dimension.equals(GameplayDimensions.PLAINS) || dimension.equals(GameplayDimensions.SAVANNA);
    }

    static boolean surfaceHabitat(ResourceKey<Level> dimension, int y) {
        if (dimension.equals(GameplayDimensions.SNOWY)) return y < -32 || y >= 0;
        return !dimension.equals(GameplayDimensions.DESERT) || y >= 0;
    }

    static int classify(ResourceKey<Level> dimension, int floor, boolean wet) {
        if (dimension.equals(GameplayDimensions.SNOWY)) return wet ? 2 : floor >= 71 ? 0 : 1;
        if (dimension.equals(GameplayDimensions.DESERT)) return wet ? 4 : 3;
        if (dimension.equals(GameplayDimensions.SAVANNA)) return wet ? 6 : 5;
        return wet ? 8 : 7;
    }

    static void capture(net.minecraft.server.level.ServerLevel level, ChunkAccess chunk, boolean legacy) {
        if (!mapped(level.dimension())) return;
        byte[] columns = new byte[256];
        int sea = level.getChunkSource().getGenerator().getSeaLevel();
        var pos = new BlockPos.MutableBlockPos();
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            int floor = chunk.getHeight(legacy ? Heightmap.Types.OCEAN_FLOOR : Heightmap.Types.OCEAN_FLOOR_WG, x, z);
            int bx = chunk.getPos().getMinBlockX() + x, bz = chunk.getPos().getMinBlockZ() + z;
            // Older chunks have no pre-decoration snapshot. Recover the existing
            // soil underneath trees and snow, including worlds with older band widths.
            if (legacy && level.dimension().equals(GameplayDimensions.SNOWY)) {
                for (int y = floor; y >= chunk.getMinBuildHeight(); y--) {
                    var ground = chunk.getBlockState(pos.set(bx, y, bz));
                    if (ground.is(BlockTags.DIRT) || ground.is(Blocks.MUD) || ground.is(Blocks.CLAY)
                            || ground.is(Blocks.STONE)) { floor = y; break; }
                }
            }
            boolean wet = floor < sea - 1 && chunk.getFluidState(pos.set(bx, sea - 1, bz)).is(FluidTags.WATER);
            columns[z * 16 + x] = (byte) classify(level.dimension(), floor, wet);
        }
        data(chunk).aoemod$setHabitats(columns);
    }

    static Holder<Biome> biome(Level level, int index) {
        return level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(HABITATS.get(index));
    }

    static void read(ChunkAccess chunk, CompoundTag tag) {
        byte[] values = tag.getByteArray(TAG);
        if (values.length != 256) return;
        for (byte value : values) if (value < 0 || value >= HABITATS.size()) return;
        data(chunk).aoemod$setHabitats(values);
    }

    static void write(ChunkAccess chunk, CompoundTag tag) {
        byte[] values = data(chunk).aoemod$getHabitats();
        if (values != null) tag.putByteArray(TAG, values);
    }
}
