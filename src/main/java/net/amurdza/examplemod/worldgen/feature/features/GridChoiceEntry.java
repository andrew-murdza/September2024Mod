package net.amurdza.examplemod.worldgen.feature.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class GridChoiceEntry {

    public static final Codec<GridChoiceEntry> CODEC =
            RecordCodecBuilder.create(inst -> inst.group(
                    ConfiguredFeature.CODEC.fieldOf("feature")
                            .forGetter(c -> c.feature),

                    Codec.INT.optionalFieldOf("count", 1)
                            .forGetter(c -> c.count)
            ).apply(inst, GridChoiceEntry::new));

    public final Holder<ConfiguredFeature<?, ?>> feature;
    public final int count;

    public GridChoiceEntry(
            Holder<ConfiguredFeature<?, ?>> feature,
            int count
    ) {
        this.feature = feature;
        this.count = Math.max(0, count);
    }
}