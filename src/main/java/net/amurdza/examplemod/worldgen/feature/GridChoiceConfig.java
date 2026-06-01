package net.amurdza.examplemod.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

import java.util.List;
import java.util.Optional;

public class GridChoiceConfig implements FeatureConfiguration {

    public static final Codec<GridChoiceConfig> CODEC =
            RecordCodecBuilder.create(inst -> inst.group(
                    GridChoiceEntry.CODEC.listOf().fieldOf("features")
                            .forGetter(c -> c.features),

                    ConfiguredFeature.CODEC.optionalFieldOf("default_feature")
                            .forGetter(c -> Optional.ofNullable(c.defaultFeature)),

                    PlacementModifier.CODEC.listOf().optionalFieldOf("placement", List.of())
                            .forGetter(c -> c.placement),

                    OffsetType.CODEC.optionalFieldOf("offset_type", OffsetType.CENTER)
                            .forGetter(c -> c.offsetType)
            ).apply(inst, (features, defaultFeature, placement, offsetType) ->
                    new GridChoiceConfig(
                            features,
                            defaultFeature.orElse(null),
                            placement,
                            offsetType
                    )
            ));

    public final List<GridChoiceEntry> features;
    public final Holder<ConfiguredFeature<?, ?>> defaultFeature; // nullable
    public final List<PlacementModifier> placement;
    public final OffsetType offsetType;

    public GridChoiceConfig(
            List<GridChoiceEntry> features,
            Holder<ConfiguredFeature<?, ?>> defaultFeature,
            List<PlacementModifier> placement,
            OffsetType offsetType
    ) {
        this.features = features;
        this.defaultFeature = defaultFeature;
        this.placement = placement;
        this.offsetType = offsetType;
    }

    public enum OffsetType implements StringRepresentable {
        IN_SQUARE("in_square"),
        CENTER("center"),
        CORNER("corner");

        public static final Codec<OffsetType> CODEC = StringRepresentable.fromEnum(OffsetType::values);

        private final String serializedName;

        OffsetType(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return this.serializedName;
        }
    }
}