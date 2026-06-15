package net.amurdza.examplemod.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.amurdza.examplemod.registry.ModStructurePlacementTypes;
import net.minecraft.core.Vec3i;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class BandStructurePlacement extends StructurePlacement {
    public static final double Z_START_BLOCKS = 0.0D;
    public static final double MAX_CONTINENTS = 0.5D;

    public static final Codec<BandStructurePlacement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("target_continents").forGetter(p -> p.targetContinents),
            Codec.INT.fieldOf("x_start").forGetter(p -> p.xStart),
            ExtraCodecs.POSITIVE_INT.fieldOf("x_spacing").forGetter(p -> p.xSpacing),
            ExtraCodecs.POSITIVE_INT.fieldOf("biome_width").forGetter(p -> p.biomeWidth)
    ).apply(instance, BandStructurePlacement::new));

    private final double targetContinents;
    private final int xSpacing;
    private final int xStart;
    private final int biomeWidth;

    public BandStructurePlacement(double targetContinents, int xStart, int xSpacing, int biomeWidth) {
        super(
                Vec3i.ZERO,
                FrequencyReductionMethod.DEFAULT,
                1.0F,
                0,
                Optional.empty()
        );

        if (targetContinents < 0.0D || targetContinents > MAX_CONTINENTS) {
            throw new IllegalArgumentException("target_band_pos must be a continents value from 0.0 to 0.5");
        }

        this.targetContinents = targetContinents;
        this.xStart = xStart;
        this.xSpacing = xSpacing;
        this.biomeWidth = biomeWidth;
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

    private static final int CHUNK_SIZE = 16;

    private double getNearestTargetZFromContinents(double z) {
        double phaseA = continentsToPhaseA(this.targetContinents);
        double phaseB = continentsToPhaseB(this.targetContinents);
        double period = getPeriodBlocks();

        double localTargetA = phaseA * period;
        double localTargetB = phaseB * period;

        double cycle = Math.floor((z - Z_START_BLOCKS) / period);

        double bestZ = -1.0D;
        double bestDistance = Double.POSITIVE_INFINITY;

        for (int cycleOffset = -1; cycleOffset <= 1; cycleOffset++) {
            double baseZ = Z_START_BLOCKS + (cycle + cycleOffset) * period;

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
        double xStartBlocks = this.xStart;
        double xSpacingBlocks = this.xSpacing * CHUNK_SIZE;

        return xStartBlocks + Math.round((x - xStartBlocks) / xSpacingBlocks) * xSpacingBlocks;
    }

    private double getPeriodBlocks() {
        return this.biomeWidth * 10.0D * CHUNK_SIZE;
    }
    private double continentsToPhaseA(double continents) {
        return continents;
    }

    private double continentsToPhaseB(double continents) {
        return 1 - continents;
    }

    private static double cleanNearZero(double value) {
        if (Math.abs(value) < 0.000001D) {
            return 0.0D;
        }

        return value;
    }

    @Override
    public @NotNull StructurePlacementType<?> type() {
        return ModStructurePlacementTypes.Z_BANDS.get();
    }
}