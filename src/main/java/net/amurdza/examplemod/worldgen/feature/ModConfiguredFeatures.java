package net.amurdza.examplemod.worldgen.feature;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;

import java.util.ArrayList;
import java.util.List;

public class ModConfiguredFeatures {
    public static final ResourceLocation MOSS_FOREST_FLOOR_LOC=
            ResourceKey.create(Registries.CONFIGURED_FEATURE,
            new ResourceLocation(AOEMod.MOD_ID,"rainforest_forest_floor")).location();
    public static final ResourceLocation MOSS_SEAFLOOR_LOC=
            ResourceKey.create(Registries.CONFIGURED_FEATURE,
                    new ResourceLocation(AOEMod.MOD_ID,"rainforest_water_plants_full")).location();

    public static final ResourceKey<ConfiguredFeature<?, ?>> BIG_DRIPLEAF_KEY = registerKey("big_dripleaf");

    public static void bootstrap(BootstapContext<ConfiguredFeature<?, ?>> context) {
        BlockStateProvider dripLeafProvider=equalChanceHorizotanlFacing(Blocks.BIG_DRIPLEAF.defaultBlockState());
        register(context, BIG_DRIPLEAF_KEY, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(dripLeafProvider));
    }

    private static BlockStateProvider equalChanceHorizotanlFacing(BlockState state){
        List<BlockState> states=new ArrayList<>();
        BlockStateProperties.HORIZONTAL_FACING.getAllValues().forEach(p->states
                .add(state.setValue(BlockStateProperties.HORIZONTAL_FACING,p.value())));
        return equalChance(states);
    }
    private static BlockStateProvider equalChance(List<BlockState> states){
        SimpleWeightedRandomList<BlockState> list= SimpleWeightedRandomList.<BlockState>builder().build();
        return new WeightedStateProvider(list);
    }

    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, new ResourceLocation(AOEMod.MOD_ID, name));
    }
    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register
            (BootstapContext<ConfiguredFeature<?, ?>> context,
             ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}
