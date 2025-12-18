package net.amurdza.examplemod.worldgen.feature;

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

public class AllSurfacesFeatureConfig implements FeatureConfiguration {

    public enum Target implements net.minecraft.util.StringRepresentable {
        AIR("air"),
        WATER("water"),
        LAVA("lava");

        private final String name;
        Target(String name) { this.name = name; }
        @Override public @NotNull String getSerializedName() { return name; }

        public static final Codec<Target> CODEC =
                net.minecraft.util.StringRepresentable.fromEnum(Target::values);
    }

    public static final Codec<TagKey<Biome>> BIOME_TAG_CODEC =
            TagKey.codec(Registries.BIOME);

    public static final Codec<AllSurfacesFeatureConfig> CODEC =
            RecordCodecBuilder.create(inst -> inst.group(
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(c -> c.feature),
                    Target.CODEC.fieldOf("target").forGetter(c -> c.target),          // ✅ NEW
                    Codec.BOOL.fieldOf("deep").forGetter(c -> c.deep),
                    BlockPredicate.CODEC.fieldOf("predicate").forGetter(c -> c.predicate),
                    Codec.BOOL.fieldOf("all_layers").forGetter(c -> c.allLayers),
                    BIOME_TAG_CODEC.optionalFieldOf("biomes")
                            .forGetter(c -> java.util.Optional.ofNullable(c.biomes))
            ).apply(inst, (feature, target, deep, predicate, allLayers, biomes) ->
                    new AllSurfacesFeatureConfig(
                            feature, target, deep, predicate, allLayers,
                            biomes.orElse(null)
                    )
            ));

    public final Holder<PlacedFeature> feature;
    public final Target target;          // ✅ NEW
    public final boolean deep;
    public final boolean allLayers;
    public final BlockPredicate predicate;
    public final TagKey<Biome> biomes;   // nullable

    public AllSurfacesFeatureConfig(
            Holder<PlacedFeature> feature,
            Target target,
            boolean deep,
            BlockPredicate predicate,
            boolean allLayers,
            TagKey<Biome> biomes
    ) {
        this.feature = feature;
        this.target = target;
        this.deep = deep;
        this.predicate = predicate;
        this.allLayers = allLayers;
        this.biomes = biomes;
    }
}
