package net.amurdza.examplemod.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class BandPolarStructurePlacement extends StructurePlacement {
    public static final double BAND_BLOCKS = 960.0D;
    public static final double Z_START_BLOCKS = 0.0D;
    public static final double Z_PERIOD_BLOCKS = 9600.0D;
    public static final double MAX_CONTINENTS = 0.5D;

    public static final Codec<BandPolarStructurePlacement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("target_band_pos").forGetter(p -> p.targetBandPos),
            Codec.DOUBLE.fieldOf("x_start").forGetter(p -> p.xStart),
            Codec.DOUBLE.fieldOf("x_spacing").forGetter(p -> p.xSpacing)
    ).apply(instance, BandPolarStructurePlacement::new));

    private final double targetBandPos;
    private final double xSpacing;
    private final double xStart;

    public BandPolarStructurePlacement(double targetBandPos, double xStart, double xSpacing) {
        super(
                Vec3i.ZERO,
                FrequencyReductionMethod.DEFAULT,
                1.0F,
                0,
                Optional.empty()
        );

        if (targetBandPos < 0.0D || targetBandPos > MAX_CONTINENTS) {
            throw new IllegalArgumentException("target_band_pos must be a continents value from 0.0 to 0.5");
        }

        if (xSpacing <= 0.0D) {
            throw new IllegalArgumentException("x_spacing must be positive");
        }

        this.targetBandPos = targetBandPos;
        this.xStart = xStart;
        this.xSpacing = xSpacing;
    }

    @Override
    protected boolean isPlacementChunk(@NotNull ChunkGeneratorStructureState structureState, int chunkX, int chunkZ) {
        double chunkCenterX = chunkX * 16.0D + 8.0D;
        double chunkCenterZ = chunkZ * 16.0D + 8.0D;

        if (chunkCenterZ < Z_START_BLOCKS) {
            return false;
        }

        double targetZ = this.getNearestTargetZFromContinents(chunkCenterZ);

        if (targetZ < Z_START_BLOCKS) {
            return false;
        }

        double targetX = this.getNearestTargetX(chunkCenterX);

        targetX = cleanNearZero(targetX);
        targetZ = cleanNearZero(targetZ);

        int targetChunkX = Mth.floor(targetX / 16.0D);
        int targetChunkZ = Mth.floor(targetZ / 16.0D);

        return chunkX == targetChunkX && chunkZ == targetChunkZ;
    }

    private double getNearestTargetZFromContinents(double z) {
        double phaseA = continentsToPhaseA(this.targetBandPos);
        double phaseB = continentsToPhaseB(this.targetBandPos);

        double localTargetA = phaseA * BAND_BLOCKS;
        double localTargetB = phaseB * BAND_BLOCKS;

        double cycle = Math.floor((z - Z_START_BLOCKS) / Z_PERIOD_BLOCKS);

        double bestZ = -1.0D;
        double bestDistance = Double.POSITIVE_INFINITY;

        for (int cycleOffset = -1; cycleOffset <= 1; cycleOffset++) {
            double baseZ = Z_START_BLOCKS + (cycle + cycleOffset) * Z_PERIOD_BLOCKS;

            double candidateA = baseZ + localTargetA;
            double candidateB = baseZ + localTargetB;

            if (candidateA >= Z_START_BLOCKS) {
                double distanceA = Math.abs(z - candidateA);

                if (distanceA < bestDistance) {
                    bestDistance = distanceA;
                    bestZ = candidateA;
                }
            }

            if (candidateB >= Z_START_BLOCKS) {
                double distanceB = Math.abs(z - candidateB);

                if (distanceB < bestDistance) {
                    bestDistance = distanceB;
                    bestZ = candidateB;
                }
            }
        }

        return bestZ;
    }

    private double getNearestTargetX(double x) {
        return this.xStart + Math.round((x - this.xStart) / this.xSpacing) * this.xSpacing;
    }

    private static double continentsToPhaseA(double continents) {
        return MAX_CONTINENTS * 10.0D - continents * 10.0D;
    }

    private static double continentsToPhaseB(double continents) {
        return MAX_CONTINENTS * 10.0D + continents * 10.0D;
    }

    private static double cleanNearZero(double value) {
        if (Math.abs(value) < 0.000001D) {
            return 0.0D;
        }

        return value;
    }

    @Override
    public @NotNull StructurePlacementType<?> type() {
        return ModStructurePlacementTypes.BAND_POLAR.get();
    }
}