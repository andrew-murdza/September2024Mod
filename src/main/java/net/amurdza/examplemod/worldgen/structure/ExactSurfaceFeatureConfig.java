package net.amurdza.examplemod.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import java.util.List;
import java.util.Optional;

public class ExactSurfaceFeatureConfig {
    public static final Codec<ExactSurfaceFeatureConfig> CODEC =
            RecordCodecBuilder.create(instance ->
                    instance.group(
                            ConfiguredFeature.CODEC
                                    .fieldOf("feature")
                                    .forGetter(cfg -> cfg.feature),

                            Biome.LIST_CODEC
                                    .optionalFieldOf("biomes")
                                    .forGetter(cfg -> cfg.biomes),

                            Codec.INT
                                    .optionalFieldOf("min_y")
                                    .forGetter(cfg -> cfg.minY),

                            Codec.INT
                                    .optionalFieldOf("max_y")
                                    .forGetter(cfg -> cfg.maxY),

                            Codec.intRange(0, Integer.MAX_VALUE)
                                    .fieldOf("count")
                                    .forGetter(cfg -> cfg.count),

                            BlockPredicate.CODEC
                                    .listOf()
                                    .fieldOf("predicates")
                                    .forGetter(cfg -> cfg.predicates)
                    ).apply(instance, ExactSurfaceFeatureConfig::new)
            );

    public final Holder<ConfiguredFeature<?, ?>> feature;
    public final Optional<HolderSet<Biome>> biomes;
    public final Optional<Integer> minY;
    public final Optional<Integer> maxY;
    public final int count;
    public final List<BlockPredicate> predicates;

    public ExactSurfaceFeatureConfig(
            Holder<ConfiguredFeature<?, ?>> feature,
            Optional<HolderSet<Biome>> biomes,
            Optional<Integer> minY,
            Optional<Integer> maxY,
            int count,
            List<BlockPredicate> predicates
    ) {
        this.feature = feature;
        this.biomes = biomes;
        this.minY = minY;
        this.maxY = maxY;
        this.count = count;
        this.predicates = predicates;
    }
}