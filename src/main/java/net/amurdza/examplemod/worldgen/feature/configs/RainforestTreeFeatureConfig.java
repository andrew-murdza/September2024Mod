package net.amurdza.examplemod.worldgen.feature.configs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;

import java.util.List;

public record RainforestTreeFeatureConfig(
        ResourceLocation canopyTemplate,
        BlockStateProvider logProvider,
        BlockStateProvider leavesProvider,
        IntProvider height,
        BlockPredicate validGround,
        List<TreeDecorator> decorators
) implements FeatureConfiguration {

    public static final Codec<RainforestTreeFeatureConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("canopy_template").forGetter(RainforestTreeFeatureConfig::canopyTemplate),
                    BlockStateProvider.CODEC.fieldOf("log_provider").forGetter(RainforestTreeFeatureConfig::logProvider),
                    BlockStateProvider.CODEC.fieldOf("leaves_provider").forGetter(RainforestTreeFeatureConfig::leavesProvider),
                    IntProvider.CODEC.fieldOf("height").forGetter(RainforestTreeFeatureConfig::height),
                    BlockPredicate.CODEC.fieldOf("valid_ground").forGetter(RainforestTreeFeatureConfig::validGround),
                    TreeDecorator.CODEC.listOf().optionalFieldOf("decorators", List.of()).forGetter(RainforestTreeFeatureConfig::decorators)
            ).apply(instance, RainforestTreeFeatureConfig::new)
    );
}