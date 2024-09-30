package net.amurdza.examplemod.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class AOETreeConfiguration implements FeatureConfiguration {
    public final BlockStateProvider trunkProvider;
    public final BlockStateProvider leafProvider;
    public final boolean hasSporeBlossoms;
    public final boolean hasCaveVines;
    public final boolean hasCocoaBeans;
    public final boolean hasVinesOnTrunk;
    public final BlockStateProvider vineProvider;

    public AOETreeConfiguration(BlockStateProvider trunkProvider, BlockStateProvider leafProvider, boolean hasSporeBlossoms, boolean hasCaveVines, boolean hasCocoaBeans, boolean hasVinesOnTrunk, BlockStateProvider vineProvider) {
        this.trunkProvider = trunkProvider;
        this.leafProvider=leafProvider;
        this.hasSporeBlossoms=hasSporeBlossoms;
        this.hasCaveVines=hasCaveVines;
        this.hasCocoaBeans=hasCocoaBeans;
        this.hasVinesOnTrunk=hasVinesOnTrunk;
        this.vineProvider=vineProvider;
    }
    public static final Codec<AOETreeConfiguration> CODEC= RecordCodecBuilder.create(instance-> instance.group(BlockStateProvider.CODEC.fieldOf("trunk_block_state_provider").forGetter(x->x.trunkProvider),
            BlockStateProvider.CODEC.fieldOf("leaf_block_state_provider").forGetter(x->x.leafProvider),
            Codec.BOOL.fieldOf("has_spore_blossoms").forGetter(x->x.hasSporeBlossoms),
            Codec.BOOL.fieldOf("has_cave_vines").forGetter(x->x.hasSporeBlossoms),
            Codec.BOOL.fieldOf("has_cocoa_beans").forGetter(x->x.hasCocoaBeans),
            Codec.BOOL.fieldOf("has_vines_on_trunk").forGetter(x->x.hasVinesOnTrunk),
            BlockStateProvider.CODEC.fieldOf("vine_block_state_provider").forGetter(x->x.vineProvider)
    ).apply(instance,AOETreeConfiguration::new));
}
