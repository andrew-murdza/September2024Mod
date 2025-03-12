package net.amurdza.examplemod.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public class CaveVineConfig implements FeatureConfiguration {
    public static final Codec<CaveVineConfig> CODEC = RecordCodecBuilder.create((p_191222_) ->
            p_191222_.group(IntProvider.CODEC.fieldOf("height_provider").forGetter(p->p.heightProvider),
                    Codec.FLOAT.fieldOf("chance").forGetter(p->p.chance)).apply(p_191222_, CaveVineConfig::new));
    public final IntProvider heightProvider;
    public final float chance;
    public CaveVineConfig(IntProvider heightProvider, float chance){
        this.heightProvider=heightProvider;
        this.chance=chance;
    }
}
