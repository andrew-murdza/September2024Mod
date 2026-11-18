package net.amurdza.examplemod.worldgen.structure;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.registry.ModStructurePlacementTypes;
import net.amurdza.examplemod.worldgen.MountainRidges;
import net.amurdza.examplemod.worldgen.SnowyBandCompression;
import net.amurdza.examplemod.worldgen.TropicalLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** One lair per kilometre along every curved summit, staggered between summits. */
public final class PlateauLairPlacement extends StructurePlacement implements LocatableStructurePlacement {
    public static final Codec<PlateauLairPlacement> CODEC = Codec.unit(PlateauLairPlacement::new);
    public static final int SPACING = 1000;

    public PlateauLairPlacement() {
        super(Vec3i.ZERO, FrequencyReductionMethod.DEFAULT, 1, 0, Optional.empty());
    }

    private static MountainRidges ridges(net.minecraft.world.level.levelgen.RandomState random) {
        var field = random.router().ridges();
        while (true) {
            if (field instanceof DensityFunctions.MarkerOrMarked marker) field = marker.wrapped();
            else if (field instanceof DensityFunctions.HolderHolder holder) field = holder.function().value();
            else return field instanceof MountainRidges ridges ? ridges : null;
        }
    }

    private static int offset(int band) {
        return Math.floorMod(TropicalLayout.mix(band), SPACING);
    }

    private static final int SEARCH = 224;
    private static final int CHAMBER_MARGIN = BossLairStructure.YETI_PLATEAU_RADIUS + 4;

    /** Reserve the whole chamber, including the four-block noise interpolation cell,
     * between the two summit edges. A longitudinal bend shifts those edges independently. */
    private static BlockPos site(MountainRidges ridges, double spacing, int band, int station) {
        return fitChamber(ridges, spacing, band, station * SPACING + offset(band), SEARCH);
    }

    private static BlockPos fitChamber(MountainRidges ridges, double spacing, int band, int planned, int reach) {
        double halfWidth = SnowyBandCompression.plateauWidth(ridges.stretch()) / 2;
        BlockPos best = null;
        double bestClearance = Double.NEGATIVE_INFINITY;
        for (int step = 0; step <= 2 * reach / 8; step++) {
            int x = planned + (step % 2 == 0 ? step / 2 : -(step + 1) / 2) * 8;
            double minBend = Double.POSITIVE_INFINITY, maxBend = Double.NEGATIVE_INFINITY;
            for (int dx = -CHAMBER_MARGIN; dx <= CHAMBER_MARGIN; dx += 2) {
                double bend = ridges.bend(x + dx);
                minBend = Math.min(minBend, bend);
                maxBend = Math.max(maxBend, bend);
            }
            int z = (int) Math.round(band * spacing - (minBend + maxBend) / 2);
            double clearance = halfWidth - Math.max(z + maxBend - band * spacing, band * spacing - z - minBend);
            if (clearance > bestClearance) {
                best = new BlockPos(x, 0, z);
                bestClearance = clearance;
            }
            if (clearance >= CHAMBER_MARGIN) return best;
        }
        return best;
    }

    private static ChunkPos candidate(MountainRidges ridges, double spacing, int band, int station) {
        return new ChunkPos(site(ridges, spacing, band, station));
    }

    public static BlockPos center(net.minecraft.world.level.levelgen.RandomState random, ChunkPos owner) {
        var ridges = ridges(random);
        if (ridges == null) return null;
        int x = owner.getMiddleBlockX();
        double spacing = SnowyBandCompression.period(ridges.stretch(), ridges.deepWaterWidth()) / 2;
        int band = (int) Math.round((owner.getMiddleBlockZ() + ridges.bend(x)) / spacing);
        int station = (int) Math.round((x - offset(band)) / (double) SPACING);
        var pos = site(ridges, spacing, band, station);
        if (new ChunkPos(pos).equals(owner)) return pos;
        // /place and existing datapacks can supply their own placement grid.
        // Preserve their ability to find a nearby plateau within reference range.
        var local = fitChamber(ridges, spacing, band, x, 64);
        return Math.abs(local.getZ() - owner.getMiddleBlockZ()) <= 64 ? local : null;
    }

    public static ChunkPos candidate(net.minecraft.world.level.levelgen.RandomState random, int band, int station) {
        var ridges = ridges(random);
        if (ridges == null) throw new IllegalArgumentException("Expected snowy mountain ridges");
        return candidate(ridges, SnowyBandCompression.period(ridges.stretch(), ridges.deepWaterWidth()) / 2, band, station);
    }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int cx, int cz) {
        var ridges = ridges(state.randomState());
        if (ridges == null) return false;
        int x = cx * 16 + 8;
        double spacing = SnowyBandCompression.period(ridges.stretch(), ridges.deepWaterWidth()) / 2;
        int band = (int) Math.round((cz * 16 + 8 + ridges.bend(x)) / spacing);
        int station = (int) Math.round((x - offset(band)) / (double) SPACING);
        if (Math.abs(x - (station * SPACING + offset(band))) > SEARCH + 8
                || Math.abs(cz * 16 + 8 + ridges.bend(x) - band * spacing) > 140) return false;
        var target = candidate(ridges, spacing, band, station);
        return target.x == cx && target.z == cz;
    }

    @Override
    public List<ChunkPos> locateCandidates(ChunkGeneratorStructureState state, BlockPos origin, int radius) {
        var ridges = ridges(state.randomState());
        if (ridges == null) return List.of();
        double spacing = SnowyBandCompression.period(ridges.stretch(), ridges.deepWaterWidth()) / 2;
        int nearBand = (int) Math.round((origin.getZ() + ridges.bend(origin.getX())) / spacing);
        List<ChunkPos> result = new ArrayList<>();
        for (int band = nearBand - radius; band <= nearBand + radius; band++) {
            int nearStation = (int) Math.round((origin.getX() - offset(band)) / (double) SPACING);
            for (int station = nearStation - radius; station <= nearStation + radius; station++)
                result.add(candidate(ridges, spacing, band, station));
        }
        return result;
    }

    @Override public StructurePlacementType<?> type() { return ModStructurePlacementTypes.PLATEAU_LAIR.get(); }
}
