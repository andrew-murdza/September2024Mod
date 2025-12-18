package net.amurdza.examplemod.worldgen.surface_rule;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public record NoiseThreshold3DConditionSource(
        ResourceKey<NormalNoise.NoiseParameters> noise,
        double minThreshold,
        double maxThreshold
) implements SurfaceRules.ConditionSource {

    public static final KeyDispatchDataCodec<NoiseThreshold3DConditionSource> CODEC =
            KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec(inst -> inst.group(
                    ResourceKey.codec(Registries.NOISE).fieldOf("noise").forGetter(NoiseThreshold3DConditionSource::noise),
                    Codec.DOUBLE.fieldOf("min_threshold").forGetter(NoiseThreshold3DConditionSource::minThreshold),
                    Codec.DOUBLE.fieldOf("max_threshold").forGetter(NoiseThreshold3DConditionSource::maxThreshold)
            ).apply(inst, NoiseThreshold3DConditionSource::new)));

    @Override
    public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() { return CODEC; }

    @Override
    public SurfaceRules.Condition apply(final SurfaceRules.Context ctx) {
        final NormalNoise n = ctx.randomState.getOrCreateNoise(this.noise);

        return () -> {
            double v = n.getValue(ctx.blockX, ctx.blockY, ctx.blockZ);
            return v >= minThreshold && v <= maxThreshold;
        };
    }
}

