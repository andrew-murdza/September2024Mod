package net.amurdza.examplemod.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public class CaveVineConfig implements FeatureConfiguration {
    public static final Codec<CaveVineConfig> CODEC = RecordCodecBuilder.create((p_191222_) ->
            p_191222_.group(IntProvider.CODEC.fieldOf("height_provider").forGetter(p->p.heightProvider),
                    Codec.FLOAT.fieldOf("chance").forGetter(p->p.chance),
                    Codec.FLOAT.fieldOf("weeping_vines_chance").forGetter(p->p.weepingVinesChance),
                            Codec.FLOAT.fieldOf("spore_blossom_chance").forGetter(p->p.weepingVinesChance))
                    .apply(p_191222_, CaveVineConfig::new));
    public final IntProvider heightProvider;
    public final float chance;
    public final float weepingVinesChance;
    public final float sporeBlossomChance;
    public CaveVineConfig(IntProvider heightProvider, float chance, float weepingVinesChance,float sporeBlossomChance){
        this.heightProvider=heightProvider;
        this.chance=chance;
        this.weepingVinesChance=weepingVinesChance;
        this.sporeBlossomChance=sporeBlossomChance;
    }
}
