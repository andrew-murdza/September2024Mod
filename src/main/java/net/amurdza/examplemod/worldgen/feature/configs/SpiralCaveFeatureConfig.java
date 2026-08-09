package net.amurdza.examplemod.worldgen.feature.configs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.amurdza.examplemod.config.CaveStructureConfig;
import net.minecraft.core.HolderSet;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public record SpiralCaveFeatureConfig(
        CaveType caveType,
        HolderSet<PlacedFeature> placedFeatures
) implements FeatureConfiguration {

    public static final Codec<SpiralCaveFeatureConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    CaveType.CODEC.fieldOf("cave_type").forGetter(SpiralCaveFeatureConfig::caveType),
                    PlacedFeature.LIST_CODEC
                            .optionalFieldOf("placed_features", HolderSet.direct(List.of()))
                            .forGetter(SpiralCaveFeatureConfig::placedFeatures)
            ).apply(instance, SpiralCaveFeatureConfig::new)
    );

    public enum CaveType implements StringRepresentable {
        MUSHROOM("mushroom"),
        NETHER("nether");

        public static final Codec<CaveType> CODEC = StringRepresentable.fromEnum(CaveType::values);

        private final String serializedName;

        CaveType(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public @NotNull String getSerializedName() {
            return this.serializedName;
        }
    }

    public Placement placement() {
        return switch (this.caveType) {
            case MUSHROOM -> new Placement(
                    CaveStructureConfig.MUSHROOM_CAVE_TARGET_CONTINENTS,
                    CaveStructureConfig.MUSHROOM_CAVE_BIOME_WIDTH,
                    CaveStructureConfig.MUSHROOM_CAVE_X_START,
                    CaveStructureConfig.MUSHROOM_CAVE_X_SPACING
            );
            case NETHER -> new Placement(
                    CaveStructureConfig.NETHER_CAVE_TARGET_CONTINENTS,
                    CaveStructureConfig.NETHER_CAVE_BIOME_WIDTH,
                    CaveStructureConfig.NETHER_CAVE_X_START,
                    CaveStructureConfig.NETHER_CAVE_X_SPACING
            );
        };
    }

    public Shape shape() {
        return switch (this.caveType) {
            case MUSHROOM -> new Shape(
                    CaveStructureConfig.MUSHROOM_CAVE_START_Y,
                    CaveStructureConfig.MUSHROOM_CAVE_END_Y,
                    CaveStructureConfig.MUSHROOM_CAVE_END_X,
                    CaveStructureConfig.MUSHROOM_CAVE_HORIZONTAL_RADIUS,
                    CaveStructureConfig.MUSHROOM_CAVE_VERTICAL_RADIUS,
                    CaveStructureConfig.MUSHROOM_CAVE_CENTRAL_PILLAR_DIAMETER,
                    CaveStructureConfig.MUSHROOM_CAVE_MIN_FLOOR_THICKNESS,
                    CaveStructureConfig.MUSHROOM_CAVE_LIQUID_DEPTH,
                    CaveStructureConfig.MUSHROOM_CAVE_LIQUID_RADIUS
            );
            case NETHER -> new Shape(
                    CaveStructureConfig.NETHER_CAVE_START_Y,
                    CaveStructureConfig.NETHER_CAVE_END_Y,
                    CaveStructureConfig.NETHER_CAVE_END_X,
                    CaveStructureConfig.NETHER_CAVE_HORIZONTAL_RADIUS,
                    CaveStructureConfig.NETHER_CAVE_VERTICAL_RADIUS,
                    CaveStructureConfig.NETHER_CAVE_CENTRAL_PILLAR_DIAMETER,
                    CaveStructureConfig.NETHER_CAVE_MIN_FLOOR_THICKNESS,
                    CaveStructureConfig.NETHER_CAVE_LIQUID_DEPTH,
                    CaveStructureConfig.NETHER_CAVE_LIQUID_RADIUS
            );
        };
    }

    public Optional<NetherLayers> netherLayers() {
        if (this.caveType != CaveType.NETHER) {
            return Optional.empty();
        }

        return Optional.of(new NetherLayers(
                CaveStructureConfig.NETHER_CAVE_MAX_DEEP_DARK_Y,
                CaveStructureConfig.NETHER_CAVE_MAX_SOUL_SAND_VALLEY_Y,
                CaveStructureConfig.NETHER_CAVE_MAX_WARPED_FOREST_Y,
                CaveStructureConfig.NETHER_CAVE_MAX_CRIMSON_FOREST_Y,
                CaveStructureConfig.NETHER_CAVE_MAX_BASALT_DELTAS_Y
        ));
    }

    public record Placement(
            double targetContinents,
            int biomeWidth,
            int xStart,
            int xSpacing
    ) {
    }

    public record Shape(
            int startY,
            int endY,
            int endX,
            float horizontalRadius,
            float verticalRadius,
            float centralPillarDiameter,
            float minFloorThickness,
            float liquidDepth,
            float liquidRadius
    ) {
    }

    public record NetherLayers(
            int maxDeepDarkY,
            int maxSoulSandValleyY,
            int maxWarpedForestY,
            int maxCrimsonForestY,
            int maxBasaltDeltasY
    ) {
    }
}
