package net.amurdza.examplemod.worldgen.feature.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import java.util.List;

public class RegionalGridChoiceCellRule {

    public static final Codec<RegionalGridChoiceCellRule> CODEC =
            RecordCodecBuilder.create(inst -> inst.group(
                    Codec.INT.listOf().optionalFieldOf("local_x", List.of())
                            .forGetter(c -> c.localX),

                    Codec.INT.listOf().optionalFieldOf("local_z", List.of())
                            .forGetter(c -> c.localZ),

                    ConfiguredFeature.CODEC.listOf().fieldOf("allowed_features")
                            .forGetter(c -> c.allowedFeatures)
            ).apply(inst, RegionalGridChoiceCellRule::new));

    public final List<Integer> localX;
    public final List<Integer> localZ;
    public final List<Holder<ConfiguredFeature<?, ?>>> allowedFeatures;

    public RegionalGridChoiceCellRule(
            List<Integer> localX,
            List<Integer> localZ,
            List<Holder<ConfiguredFeature<?, ?>>> allowedFeatures
    ) {
        this.localX = localX;
        this.localZ = localZ;
        this.allowedFeatures = allowedFeatures;
    }

    public boolean matches(int x, int z) {
        boolean xMatches = this.localX.isEmpty() || this.localX.contains(x);
        boolean zMatches = this.localZ.isEmpty() || this.localZ.contains(z);

        return xMatches && zMatches;
    }
}
