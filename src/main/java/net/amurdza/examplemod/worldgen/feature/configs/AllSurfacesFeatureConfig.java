package net.amurdza.examplemod.worldgen.feature.configs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class AllSurfacesFeatureConfig implements FeatureConfiguration {

    public enum Target implements net.minecraft.util.StringRepresentable {
        AIR("air"),
        WATER("water"),
        LAVA("lava");

        private final String name;

        Target(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }

        public static final Codec<Target> CODEC =
                net.minecraft.util.StringRepresentable.fromEnum(Target::values);
    }

    public static final Codec<TagKey<Biome>> BIOME_TAG_CODEC =
            TagKey.codec(Registries.BIOME);

    public static final Codec<AllSurfacesFeatureConfig> CODEC =
            RecordCodecBuilder.create(inst -> inst.group(
                    PlacedFeature.CODEC.optionalFieldOf("feature")
                            .forGetter(c -> Optional.ofNullable(c.feature)),

                    PlacedFeature.CODEC.optionalFieldOf("deep_feature")
                            .forGetter(c -> Optional.ofNullable(c.deepFeature)),

                    PlacedFeature.CODEC.listOf().optionalFieldOf("guaranteed_features", List.of())
                            .forGetter(c -> c.guaranteedFeatures),

                    Target.CODEC.fieldOf("target")
                            .forGetter(c -> c.target),

                    BlockPredicate.CODEC.fieldOf("predicate")
                            .forGetter(c -> c.predicate),

                    Codec.BOOL.fieldOf("all_layers")
                            .forGetter(c -> c.allLayers),

                    Codec.INT.optionalFieldOf("skip_height", 0)
                            .forGetter(c -> c.skipHeight),

                    BIOME_TAG_CODEC.optionalFieldOf("biomes")
                            .forGetter(c -> Optional.ofNullable(c.biomes)),

                    Codec.INT.optionalFieldOf("min_y")
                            .forGetter(c -> Optional.ofNullable(c.minY)),

                    Codec.INT.optionalFieldOf("max_y")
                            .forGetter(c -> Optional.ofNullable(c.maxY))
            ).apply(inst, (feature, deepFeature, guaranteedFeatures, target, predicate, allLayers, skipHeight, biomes, minY, maxY) ->
                    new AllSurfacesFeatureConfig(
                            feature.orElse(null),
                            deepFeature.orElse(null),
                            guaranteedFeatures,
                            target,
                            predicate,
                            allLayers,
                            skipHeight,
                            biomes.orElse(null),
                            minY.orElse(null),
                            maxY.orElse(null)
                    )
            ));

    public final Holder<PlacedFeature> feature;                  // nullable
    public final Holder<PlacedFeature> deepFeature;              // nullable
    public final List<Holder<PlacedFeature>> guaranteedFeatures;
    public final Target target;
    public final boolean allLayers;
    public final int skipHeight;
    public final BlockPredicate predicate;
    public final TagKey<Biome> biomes;                           // nullable
    public final Integer minY;                                   // nullable
    public final Integer maxY;                                   // nullable

    public AllSurfacesFeatureConfig(
            Holder<PlacedFeature> feature,
            Holder<PlacedFeature> deepFeature,
            List<Holder<PlacedFeature>> guaranteedFeatures,
            Target target,
            BlockPredicate predicate,
            boolean allLayers,
            int skipHeight,
            TagKey<Biome> biomes,
            Integer minY,
            Integer maxY
    ) {
        this.feature = feature;
        this.deepFeature = deepFeature;
        this.guaranteedFeatures = guaranteedFeatures;
        this.target = target;
        this.predicate = predicate;
        this.allLayers = allLayers;
        this.skipHeight = Math.max(0, skipHeight);
        this.biomes = biomes;
        this.minY = minY;
        this.maxY = maxY;
    }
}