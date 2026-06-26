package net.amurdza.examplemod.worldgen.feature.configs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;

import java.util.List;
import java.util.Optional;

public record RainforestTreeFeatureConfig(
        ResourceLocation canopyTemplate,
        BlockStateProvider logProvider,
        BlockStateProvider leavesProvider,
        IntProvider heightProvider,
        Optional<Integer> topY,
        int trunkWidth,
        boolean placeWaterRing,
        boolean includeWeepingVines,
        BlockPredicate validGround,
        List<TreeDecorator> decorators
) implements FeatureConfiguration {

    public static final Codec<RainforestTreeFeatureConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("canopy_template").forGetter(RainforestTreeFeatureConfig::canopyTemplate),
                    BlockStateProvider.CODEC.fieldOf("log_provider").forGetter(RainforestTreeFeatureConfig::logProvider),
                    BlockStateProvider.CODEC.fieldOf("leaves_provider").forGetter(RainforestTreeFeatureConfig::leavesProvider),
                    IntProvider.CODEC.fieldOf("height_provider").forGetter(RainforestTreeFeatureConfig::heightProvider),
                    Codec.INT.optionalFieldOf("top_y").forGetter(RainforestTreeFeatureConfig::topY),
                    Codec.intRange(1, 2).optionalFieldOf("trunk_width", 2).forGetter(RainforestTreeFeatureConfig::trunkWidth),
                    Codec.BOOL.optionalFieldOf("place_water_ring", true).forGetter(RainforestTreeFeatureConfig::placeWaterRing),
                    Codec.BOOL.optionalFieldOf("include_weeping_vines", true).forGetter(RainforestTreeFeatureConfig::includeWeepingVines),
                    BlockPredicate.CODEC.optionalFieldOf("valid_ground", BlockPredicate.matchesTag(BlockTags.DIRT)).forGetter(RainforestTreeFeatureConfig::validGround),
                    TreeDecorator.CODEC.listOf().optionalFieldOf("decorators", List.of()).forGetter(RainforestTreeFeatureConfig::decorators)
            ).apply(instance, RainforestTreeFeatureConfig::new)
    );
}
