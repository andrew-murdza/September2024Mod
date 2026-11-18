package net.amurdza.examplemod.worldgen.structure;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.registry.ModStructurePlacementTypes;
import net.amurdza.examplemod.worldgen.SavannaBands;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/** Put large camps on the dry divides, keeping the entire square apron inland. */
public final class SavannaCampPlacement extends StructurePlacement implements LocatableStructurePlacement {
    public static final Codec<SavannaCampPlacement> CODEC = Codec.unit(SavannaCampPlacement::new);
    public static final int X_SPACING = 1152;
    public static final int Z_SPACING = (int) (2 * SavannaBands.PERIOD);
    private static final Map<RandomState, DensityFunction> TERRAIN_LAND = new WeakHashMap<>();
    private final Map<DensityFunction, Map<Integer, BlockPos>> centers = new WeakHashMap<>();

    public SavannaCampPlacement() {
        super(Vec3i.ZERO, FrequencyReductionMethod.DEFAULT, 1, 91374833, Optional.empty());
    }

    /** Temperature remains vanilla climate; camp siting must use the seeded terrain's actual warp. */
    public static synchronized DensityFunction terrainLand(RandomState state) {
        return TERRAIN_LAND.computeIfAbsent(state, seeded -> {
            DensityFunction[] found = {null};
            seeded.router().finalDensity().mapAll(function -> {
                if (function instanceof DimensionTerrain terrain && terrain.dimension().equals("savanna")) found[0] = terrain.land();
                return function;
            });
            // Older serialized routers also expose this same warp as ridges.
            return found[0] == null ? seeded.router().ridges() : found[0];
        });
    }

    public static BlockPos findCenter(DensityFunction land, int column, int row) {
        var bands = new SavannaBands(land);
        int nominalX = column * X_SPACING + X_SPACING / 2 + 8;
        double crest = row * (double) Z_SPACING + SavannaBands.PERIOD / 2;
        BlockPos best = null;
        double bestClearance = -Double.MAX_VALUE;
        // Favor the highest minimum elevation over the complete footprint,
        // rather than picking a high center whose corners still enter water.
        for (int offset = -256; offset <= 256; offset += 32) {
            int x = nominalX + offset;
            double low = Double.POSITIVE_INFINITY, high = Double.NEGATIVE_INFINITY;
            for (int dx = -128; dx <= 128; dx += 4) {
                double bend = bands.bend(x + dx);
                low = Math.min(low, bend);
                high = Math.max(high, bend);
            }
            int z = Math.floorDiv((int) Math.round(crest - (low + high) / 2), 16) * 16 + 8;
            double clearance = SavannaBands.LAND_HALF_WIDTH - 128
                    - Math.max(Math.abs(z + low - crest), Math.abs(z + high - crest));
            if (clearance > bestClearance + 1e-6 || Math.abs(clearance - bestClearance) <= 1e-6
                    && (best == null || Math.abs(offset) < Math.abs(best.getX() - nominalX))) {
                bestClearance = clearance;
                best = new BlockPos(x, 0, z);
            }
        }
        return best;
    }

    private synchronized BlockPos center(DensityFunction land, int column, int row) {
        var cache = centers.computeIfAbsent(land, ignored -> new HashMap<>());
        // Every second divide is exactly 60 chunks away. Translating row zero
        // preserves both the ridge and its alignment to the owning chunk.
        BlockPos value = cache.get(column);
        if (value == null) {
            value = findCenter(land, column, 0);
            if (cache.size() >= 4096) cache.clear();
            cache.put(column, value);
        }
        return value.offset(0, 0, row * Z_SPACING);
    }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int x, int z) {
        BlockPos center = center(terrainLand(state.randomState()), Math.floorDiv(x * 16 + 8, X_SPACING),
                Math.floorDiv(z * 16 + 8, Z_SPACING));
        return x == center.getX() >> 4 && z == center.getZ() >> 4;
    }

    @Override
    public List<ChunkPos> locateCandidates(ChunkGeneratorStructureState state, BlockPos origin, int radius) {
        int column = Math.floorDiv(origin.getX(), X_SPACING), row = Math.floorDiv(origin.getZ(), Z_SPACING);
        var result = new ArrayList<ChunkPos>();
        var land = terrainLand(state.randomState());
        for (int x = column - radius; x <= column + radius; x++) {
            for (int z = row - radius; z <= row + radius; z++) result.add(new ChunkPos(center(land, x, z)));
        }
        return result;
    }

    @Override public StructurePlacementType<?> type() { return ModStructurePlacementTypes.SAVANNA_CAMP.get(); }
}
