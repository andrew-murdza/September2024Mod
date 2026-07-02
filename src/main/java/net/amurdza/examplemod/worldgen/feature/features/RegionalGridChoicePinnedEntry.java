package net.amurdza.examplemod.worldgen.feature.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import java.util.List;

public class RegionalGridChoicePinnedEntry {

    public static final Codec<RegionalGridChoicePinnedEntry> CODEC =
            RecordCodecBuilder.create(inst -> inst.group(
                    ConfiguredFeature.CODEC.fieldOf("feature")
                            .forGetter(c -> c.feature),

                    Codec.INT.optionalFieldOf("count", 1)
                            .forGetter(c -> c.count),

                    Codec.INT.listOf().optionalFieldOf("local_x", List.of())
                            .forGetter(c -> c.localX),

                    Codec.INT.listOf().optionalFieldOf("local_z", List.of())
                            .forGetter(c -> c.localZ),

                    Codec.INT.listOf().optionalFieldOf("region_x", List.of())
                            .forGetter(c -> c.regionX),

                    Codec.INT.listOf().optionalFieldOf("region_z", List.of())
                            .forGetter(c -> c.regionZ)
            ).apply(inst, RegionalGridChoicePinnedEntry::new));

    public final Holder<ConfiguredFeature<?, ?>> feature;
    public final int count;
    public final List<Integer> localX;
    public final List<Integer> localZ;
    public final List<Integer> regionX;
    public final List<Integer> regionZ;

    public RegionalGridChoicePinnedEntry(
            Holder<ConfiguredFeature<?, ?>> feature,
            int count,
            List<Integer> localX,
            List<Integer> localZ,
            List<Integer> regionX,
            List<Integer> regionZ
    ) {
        this.feature = feature;
        this.count = Math.max(0, count);
        this.localX = localX;
        this.localZ = localZ;
        this.regionX = regionX;
        this.regionZ = regionZ;
    }

    public boolean matches(int x, int z, int regionX, int regionZ) {
        boolean xMatches = this.localX.isEmpty() || this.localX.contains(x);
        boolean zMatches = this.localZ.isEmpty() || this.localZ.contains(z);
        boolean regionXMatches = this.regionX.isEmpty() || this.regionX.contains(regionX);
        boolean regionZMatches = this.regionZ.isEmpty() || this.regionZ.contains(regionZ);

        return xMatches && zMatches && regionXMatches && regionZMatches;
    }
}
