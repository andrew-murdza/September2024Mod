package net.amurdza.examplemod.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.amurdza.examplemod.Config;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class BandPolarStructurePlacement extends StructurePlacement {
    public static final double BAND_PERIOD = 12.0D;

    public static final Codec<BandPolarStructurePlacement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("target_band_pos").forGetter(p -> p.targetBandPos),
            Codec.DOUBLE.fieldOf("arc_spacing").forGetter(p -> p.arcSpacing)
    ).apply(instance, BandPolarStructurePlacement::new));

    private final double targetBandPos;
    private final double arcSpacing;

    public BandPolarStructurePlacement(double targetBandPos, double arcSpacing) {
        super(
                Vec3i.ZERO,
                FrequencyReductionMethod.DEFAULT,
                1.0F,
                0,
                Optional.empty()
        );

        this.targetBandPos = targetBandPos;
        this.arcSpacing = arcSpacing;
    }

    @Override
    protected boolean isPlacementChunk(@NotNull ChunkGeneratorStructureState structureState, int chunkX, int chunkZ) {
        double chunkCenterX = chunkX * 16.0D + 8.0D;
        double chunkCenterZ = chunkZ * 16.0D + 8.0D;

        double chunkCenterRadius = Math.sqrt(chunkCenterX * chunkCenterX + chunkCenterZ * chunkCenterZ);

        if (chunkCenterRadius <= 0.000001D) {
            return false;
        }

        double targetRadius = this.getNearestTargetRadius(chunkCenterRadius);

        if (targetRadius <= 0.0D) {
            return false;
        }

        double theta = Math.atan2(chunkCenterZ, chunkCenterX);

        if (theta < 0.0D) {
            theta += Math.PI * 2.0D;
        }

        int slotCount = this.getSlotCount(targetRadius);
        double angleStep = (Math.PI * 2.0D) / slotCount;

        int nearestSlot = Math.floorMod((int)Math.round(theta / angleStep), slotCount);
        double targetAngle = nearestSlot * angleStep;

        double targetX = Math.cos(targetAngle) * targetRadius;
        double targetZ = Math.sin(targetAngle) * targetRadius;

        targetX = cleanNearZero(targetX);
        targetZ = cleanNearZero(targetZ);

        int targetChunkX = Mth.floor(targetX / 16.0D);
        int targetChunkZ = Mth.floor(targetZ / 16.0D);

        return chunkX == targetChunkX && chunkZ == targetChunkZ;
    }

    private double getNearestTargetRadius(double radius) {
        double bandWidth = Config.BAND_WIDTH;
        double periodBlocks = bandWidth * BAND_PERIOD;

        double targetA = this.targetBandPos * bandWidth;
        double targetB = (BAND_PERIOD - this.targetBandPos) * bandWidth;

        double cycle = Math.floor(radius / periodBlocks);

        double bestRadius = -1.0D;
        double bestDistance = Double.POSITIVE_INFINITY;

        for (int cycleOffset = -1; cycleOffset <= 1; cycleOffset++) {
            double baseRadius = (cycle + cycleOffset) * periodBlocks;

            double candidateA = baseRadius + targetA;
            double candidateB = baseRadius + targetB;

            if (candidateA > 0.0D) {
                double distanceA = Math.abs(radius - candidateA);

                if (distanceA < bestDistance) {
                    bestDistance = distanceA;
                    bestRadius = candidateA;
                }
            }

            if (candidateB > 0.0D) {
                double distanceB = Math.abs(radius - candidateB);

                if (distanceB < bestDistance) {
                    bestDistance = distanceB;
                    bestRadius = candidateB;
                }
            }
        }

        return bestRadius;
    }

    private int getSlotCount(double radius) {
        double circumference = Math.PI * 2.0D * radius;
        int approximateSlots = Math.max(4, (int)Math.round(circumference / this.arcSpacing));

        return Math.max(4, Math.round(approximateSlots / 4.0F) * 4);
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