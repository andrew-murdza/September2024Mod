package net.amurdza.examplemod.worldgen.feature.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import java.util.List;

public class RegionalGridChoiceGroup {

    public static final Codec<RegionalGridChoiceGroup> CODEC =
            RecordCodecBuilder.create(inst -> inst.group(
                    ConfiguredFeature.CODEC.listOf().fieldOf("features")
                            .forGetter(c -> c.features),

                    Codec.INT.fieldOf("count")
                            .forGetter(c -> c.count),

                    Codec.INT.optionalFieldOf("extra_count", 0)
                            .forGetter(c -> c.extraCount),

                    Codec.INT.optionalFieldOf("extra_feature_count", 0)
                            .forGetter(c -> c.extraFeatureCount)
            ).apply(inst, RegionalGridChoiceGroup::new));

    public final List<Holder<ConfiguredFeature<?, ?>>> features;
    public final int count;
    public final int extraCount;
    public final int extraFeatureCount;

    public RegionalGridChoiceGroup(
            List<Holder<ConfiguredFeature<?, ?>>> features,
            int count,
            int extraCount,
            int extraFeatureCount
    ) {
        this.features = features;
        this.count = Math.max(0, count);
        this.extraCount = extraCount;
        this.extraFeatureCount = Math.max(0, extraFeatureCount);
    }
}
