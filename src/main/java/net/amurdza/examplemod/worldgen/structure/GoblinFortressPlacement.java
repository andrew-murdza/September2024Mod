package net.amurdza.examplemod.worldgen.structure;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.registry.ModStructurePlacementTypes;
import net.amurdza.examplemod.worldgen.DimensionBiomeSource;
import net.amurdza.examplemod.worldgen.MountainRidges;
import net.amurdza.examplemod.worldgen.SnowyBandCompression;
import net.amurdza.examplemod.worldgen.SnowyOceanFloor;
import net.amurdza.examplemod.worldgen.TropicalLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** A developed mountainside at each existing kilometre station, with farms on its dry flank. */
public final class GoblinFortressPlacement extends StructurePlacement implements LocatableStructurePlacement {
    public static final Codec<GoblinFortressPlacement> CODEC = Codec.unit(GoblinFortressPlacement::new);
    public static final int SPACING = 1000;
    private static final int SEARCH = 224;
    private static final java.util.Map<MountainRidges, java.util.concurrent.ConcurrentHashMap<Long, Site>> SITES = new java.util.WeakHashMap<>();

    /** Positive local Z always points uphill; the origin is the exterior courtyard floor. */
    public record Site(BlockPos origin, int uphill, boolean habitatsFit) { }

    public GoblinFortressPlacement() {
        super(Vec3i.ZERO, FrequencyReductionMethod.DEFAULT, 1, 0, Optional.empty());
    }

    private static MountainRidges ridges(RandomState random) {
        var field = random.router().ridges();
        while (true) {
            if (field instanceof DensityFunctions.MarkerOrMarked marker) field = marker.wrapped();
            else if (field instanceof DensityFunctions.HolderHolder holder) field = holder.function().value();
            else return field instanceof MountainRidges ridges ? ridges : null;
        }
    }

    private static int offset(int band) { return Math.floorMod(TropicalLayout.mix(band), SPACING); }
    private static int uphill(int band) { return (TropicalLayout.mix(band ^ 0x764943L) & 1) == 0 ? 1 : -1; }

    private static double spacing(MountainRidges ridges) {
        return SnowyBandCompression.period(ridges.stretch(), ridges.deepWaterWidth()) / 2;
    }

    private static Site fit(MountainRidges ridges, int band, int plannedX, int reach, int uphill) {
        Site best = null;
        double bestSpread = Double.POSITIVE_INFINITY;
        // The .4 contour is 30% of the half-period from the ridge. Set the
        // courtyard eight blocks uphill of that contour, leaving its crop
        // strips (-50..-16) within the narrow dappled flank.
        double distance = 72 * ridges.stretch() + SnowyBandCompression.EXTRA_PLATEAU_WIDTH / 2 - 8;
        for (int step = 0; step <= 2 * reach / 8; step++) {
            int x = plannedX + (step % 2 == 0 ? step / 2 : -(step + 1) / 2) * 8;
            double low = Double.POSITIVE_INFINITY, high = Double.NEGATIVE_INFINITY;
            for (int dx = -80; dx <= 80; dx += 4) {
                double bend = ridges.bend(x + dx);
                low = Math.min(low, bend);
                high = Math.max(high, bend);
            }
            int z = (int) Math.round(band * spacing(ridges) - (low + high) / 2 - uphill * distance);
            var origin = new BlockPos(x, 0, z);
            boolean fits = habitatsFit(ridges, origin, uphill);
            if (best != null && (best.habitatsFit && !fits || best.habitatsFit == fits && high - low >= bestSpread)) continue;
            int floor = 0;
            for (int dx = -72; dx <= 72; dx += 8) for (int dz : new int[]{-50, -16})
                floor = Math.max(floor, SnowyOceanFloor.floor(ridges, x + dx, z + uphill * dz));
            best = new Site(origin.atY(floor), uphill, fits);
            bestSpread = high - low;
            // Prefer the planned station whenever it already fits; search
            // only as far as the bends require, keeping spacing predictable.
            if (fits) return best;
        }
        return best;
    }

    /** Test every quart column used by farms and the two separate snowy pens. */
    public static boolean habitatsFit(DensityFunction ridges, BlockPos origin, int uphill) {
        for (int dx = -76; dx <= 76; dx += 4) {
            for (int dz = -50; dz <= -16; dz += 2)
                if (biome(ridges, origin.getX() + dx, origin.getZ() + uphill * dz) != 3) return false;
            for (int dz : new int[]{12, 35})
                if (biome(ridges, origin.getX() + dx, origin.getZ() + uphill * dz) != 0) return false;
        }
        return true;
    }

    private static int biome(DensityFunction ridges, int x, int z) {
        // Match the biome source's quart quantization, including its floor for
        // negative coordinates. Avoid planting a valid-looking block beside
        // a biome boundary whose actual quart still belongs to snow or ocean.
        int qx = Math.floorDiv(x, 4) * 4, qz = Math.floorDiv(z, 4) * 4;
        double w = ridges.compute(new DensityFunction.SinglePointContext(qx, 0, qz));
        float sampled = net.minecraft.world.level.biome.Climate.unquantizeCoord(
                net.minecraft.world.level.biome.Climate.quantizeCoord((float) w));
        return DimensionBiomeSource.snowySurfaceIndex(sampled);
    }

    private static Site planned(MountainRidges ridges, int band, int station) {
        java.util.concurrent.ConcurrentHashMap<Long, Site> sites;
        synchronized (SITES) { sites = SITES.computeIfAbsent(ridges, unused -> new java.util.concurrent.ConcurrentHashMap<>()); }
        if (sites.size() > 4096) sites.clear();
        return sites.computeIfAbsent(ChunkPos.asLong(band, station),
                unused -> fit(ridges, band, station * SPACING + offset(band), SEARCH, uphill(band)));
    }

    public static ChunkPos candidate(RandomState random, int band, int station) {
        var ridges = ridges(random);
        if (ridges == null) throw new IllegalArgumentException("Expected snowy mountain ridges");
        return new ChunkPos(planned(ridges, band, station).origin);
    }

    /** Natural starts and nearby /place requests never move more than the reference window allows. */
    public static Site site(RandomState random, ChunkPos owner) {
        var ridges = ridges(random);
        if (ridges == null) return null;
        int x = owner.getMiddleBlockX(), z = owner.getMiddleBlockZ();
        int band = (int) Math.round((z + ridges.bend(x)) / spacing(ridges));
        int station = (int) Math.round((x - offset(band)) / (double) SPACING);
        var planned = planned(ridges, band, station);
        if (new ChunkPos(planned.origin).equals(owner)) return planned.habitatsFit ? planned : null;
        int side = z + ridges.bend(x) < band * spacing(ridges) ? 1 : -1;
        var local = fit(ridges, band, x, 0, side);
        return local.habitatsFit && Math.abs(local.origin.getZ() - z) <= 24 ? local : null;
    }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int cx, int cz) {
        var ridges = ridges(state.randomState());
        if (ridges == null) return false;
        int x = cx * 16 + 8, z = cz * 16 + 8;
        int band = (int) Math.round((z + ridges.bend(x)) / spacing(ridges));
        int station = (int) Math.round((x - offset(band)) / (double) SPACING);
        if (Math.abs(x - (station * SPACING + offset(band))) > SEARCH + 8) return false;
        var site = planned(ridges, band, station);
        var target = new ChunkPos(site.origin);
        return site.habitatsFit && target.x == cx && target.z == cz;
    }

    @Override
    public List<ChunkPos> locateCandidates(ChunkGeneratorStructureState state, BlockPos origin, int radius) {
        var ridges = ridges(state.randomState());
        if (ridges == null) return List.of();
        int nearBand = (int) Math.round((origin.getZ() + ridges.bend(origin.getX())) / spacing(ridges));
        List<ChunkPos> result = new ArrayList<>();
        for (int band = nearBand - radius; band <= nearBand + radius; band++) {
            int nearStation = (int) Math.round((origin.getX() - offset(band)) / (double) SPACING);
            for (int station = nearStation - radius; station <= nearStation + radius; station++) {
                var site = planned(ridges, band, station);
                if (site.habitatsFit) result.add(new ChunkPos(site.origin));
            }
        }
        return result;
    }

    @Override public StructurePlacementType<?> type() { return ModStructurePlacementTypes.GOBLIN_FORTRESS.get(); }
}
