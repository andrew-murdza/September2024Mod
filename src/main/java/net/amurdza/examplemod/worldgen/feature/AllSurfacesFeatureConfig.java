package net.amurdza.examplemod.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class AllSurfacesFeatureConfig implements FeatureConfiguration {
    public static final Codec<AllSurfacesFeatureConfig> CODEC = RecordCodecBuilder.create((p_67866_)
            -> p_67866_.group(PlacedFeature.CODEC.fieldOf("feature").forGetter((p_161045_)
            -> p_161045_.feature),Codec.BOOL.fieldOf("water").forGetter(p_161046_-> p_161046_.water),
                    BlockPredicate.CODEC.fieldOf("predicate").forGetter((p_191579_) -> p_191579_.predicate),
                    Codec.BOOL.fieldOf("all_layers").forGetter(p_161046_-> p_161046_.allLayers))
            .apply(p_67866_, AllSurfacesFeatureConfig::new));
    public final Holder<PlacedFeature> feature;
    public final boolean water;
    public final boolean allLayers;
    public final BlockPredicate predicate;

    public AllSurfacesFeatureConfig(Holder<PlacedFeature> p_67862_, boolean water, BlockPredicate predicate, boolean allLayers) {
        this.feature = p_67862_;
        this.water=water;
        this.predicate=predicate;
        this.allLayers=allLayers;
    }
}
