package net.amurdza.examplemod.worldgen.feature.configs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.amurdza.examplemod.worldgen.feature.features.RegionalGridChoiceCellRule;
import net.amurdza.examplemod.worldgen.feature.features.GridChoiceEntry;
import net.amurdza.examplemod.worldgen.feature.features.RegionalGridChoiceGroup;
import net.amurdza.examplemod.worldgen.feature.features.RegionalGridChoiceMinimumSection;
import net.amurdza.examplemod.worldgen.feature.features.RegionalGridChoicePinnedEntry;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

import java.util.List;
import java.util.Optional;

public class RegionalGridChoiceConfig implements FeatureConfiguration {

    public static final Codec<RegionalGridChoiceConfig> CODEC =
            RecordCodecBuilder.create(inst -> inst.group(
                    GridChoiceEntry.CODEC.listOf().fieldOf("features")
                            .forGetter(c -> c.features),

                    RegionalGridChoiceGroup.CODEC.listOf().optionalFieldOf("groups", List.of())
                            .forGetter(c -> c.groups),

                    RegionalGridChoicePinnedEntry.CODEC.listOf().optionalFieldOf("pinned_features", List.of())
                            .forGetter(c -> c.pinnedFeatures),

                    RegionalGridChoiceMinimumSection.CODEC.listOf().optionalFieldOf("minimum_sections", List.of())
                            .forGetter(c -> c.minimumSections),

                    RegionalGridChoiceCellRule.CODEC.listOf().optionalFieldOf("cell_rules", List.of())
                            .forGetter(c -> c.cellRules),

                    ConfiguredFeature.CODEC.optionalFieldOf("default_feature")
                            .forGetter(c -> Optional.ofNullable(c.defaultFeature)),

                    PlacementModifier.CODEC.listOf().optionalFieldOf("placement", List.of())
                            .forGetter(c -> c.placement),

                    GridChoiceConfig.OffsetType.CODEC.optionalFieldOf("offset_type", GridChoiceConfig.OffsetType.CENTER)
                            .forGetter(c -> c.offsetType),

                    Codec.intRange(1, 512).fieldOf("region_width")
                            .forGetter(c -> c.regionWidth),

                    Codec.intRange(1, 512).fieldOf("region_depth")
                            .forGetter(c -> c.regionDepth),

                    Codec.intRange(1, 512).fieldOf("section_width")
                            .forGetter(c -> c.sectionWidth),

                    Codec.intRange(1, 512).fieldOf("section_depth")
                            .forGetter(c -> c.sectionDepth),

                    Codec.STRING.optionalFieldOf("salt", "")
                            .forGetter(c -> c.salt)
            ).apply(inst, (features, groups, pinnedFeatures, minimumSections, cellRules, defaultFeature, placement, offsetType, regionWidth, regionDepth, sectionWidth, sectionDepth, salt) ->
                    new RegionalGridChoiceConfig(
                            features,
                            groups,
                            pinnedFeatures,
                            minimumSections,
                            cellRules,
                            defaultFeature.orElse(null),
                            placement,
                            offsetType,
                            regionWidth,
                            regionDepth,
                            sectionWidth,
                            sectionDepth,
                            salt
                    )
            ));

    public final List<GridChoiceEntry> features;
    public final List<RegionalGridChoiceGroup> groups;
    public final List<RegionalGridChoicePinnedEntry> pinnedFeatures;
    public final List<RegionalGridChoiceMinimumSection> minimumSections;
    public final List<RegionalGridChoiceCellRule> cellRules;
    public final Holder<ConfiguredFeature<?, ?>> defaultFeature; // nullable
    public final List<PlacementModifier> placement;
    public final GridChoiceConfig.OffsetType offsetType;
    public final int regionWidth;
    public final int regionDepth;
    public final int sectionWidth;
    public final int sectionDepth;
    public final String salt;

    public RegionalGridChoiceConfig(
            List<GridChoiceEntry> features,
            List<RegionalGridChoiceGroup> groups,
            List<RegionalGridChoicePinnedEntry> pinnedFeatures,
            List<RegionalGridChoiceMinimumSection> minimumSections,
            List<RegionalGridChoiceCellRule> cellRules,
            Holder<ConfiguredFeature<?, ?>> defaultFeature,
            List<PlacementModifier> placement,
            GridChoiceConfig.OffsetType offsetType,
            int regionWidth,
            int regionDepth,
            int sectionWidth,
            int sectionDepth,
            String salt
    ) {
        this.features = features;
        this.groups = groups;
        this.pinnedFeatures = pinnedFeatures;
        this.minimumSections = minimumSections;
        this.cellRules = cellRules;
        this.defaultFeature = defaultFeature;
        this.placement = placement;
        this.offsetType = offsetType;
        this.regionWidth = regionWidth;
        this.regionDepth = regionDepth;
        this.sectionWidth = sectionWidth;
        this.sectionDepth = sectionDepth;
        this.salt = salt;
    }
}
