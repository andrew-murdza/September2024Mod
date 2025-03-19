package net.amurdza.examplemod.worldgen.feature;

import com.crypticmushroom.minecraft.midnight.common.block.plant.NightReedBlock;
import com.crypticmushroom.minecraft.midnight.common.registry.MnBlocks;
import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.*;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.material.Fluids;
import net.potionstudios.biomeswevegone.world.level.block.BWGBlocks;
import net.potionstudios.biomeswevegone.world.level.levelgen.feature.configured.ConfiguredFeaturesUtil;
import net.potionstudios.biomeswevegone.world.level.levelgen.feature.placed.PlacedFeaturesUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ModConfiguredFeatures {
    public static final ResourceKey<ConfiguredFeature<?,?>> RAINFOREST_FLOOR_KEY=
            ResourceKey.create(Registries.CONFIGURED_FEATURE,
                    new ResourceLocation(AOEMod.MOD_ID,"rainforest_water_plants_full"));
    public static final ResourceKey<ConfiguredFeature<?,?>> RAINFOREST_SEAFLOOR_KEY=
            ResourceKey.create(Registries.CONFIGURED_FEATURE,
                    new ResourceLocation(AOEMod.MOD_ID,"rainforest_water_plants_full"));
//    public static final ResourceKey<ConfiguredFeature<?, ?>> BIG_DRIPLEAF_KEY = registerKey("big_dripleaf");
//    public static final ResourceKey<ConfiguredFeature<?, ?>> CACTUS_KEY = registerKey("cactus");
//    public static final ResourceKey<ConfiguredFeature<?, ?>> LILY_PAD_KEY = registerKey("lilypad");
//    public static final ResourceKey<ConfiguredFeature<?, ?>> NIGHT_REED_KEY = registerKey("night_reeds");
//    public static final ResourceKey<ConfiguredFeature<?, ?>> PITAYA_KEY = registerKey("pitaya");
//    public static final ResourceKey<ConfiguredFeature<?, ?>> RAINFOREST_FLOWERS_KEY = registerKey("rainforest_flowers");
//    public static final ResourceKey<ConfiguredFeature<?, ?>> RAINFOREST_WATER_PLANTS_KEY = registerKey("rainforest_water_plants");
//    public static final ResourceKey<ConfiguredFeature<?, ?>> KELP_KEY = registerKey("kelp");
//    public static final ResourceKey<ConfiguredFeature<?, ?>> SEAGRASS_KEY = registerKey("seagrass");
//    public static final ResourceKey<ConfiguredFeature<?, ?>> RAINFOREST_GRASS_KEY = registerKey("rainforest_grass");
//    public static final ResourceKey<ConfiguredFeature<?, ?>> SMALL_DRIPLEAF_KEY = registerKey("small_dripleaf");
//    public static final ResourceKey<ConfiguredFeature<?, ?>> RAINFOREST_FLOOR_KEY = registerKey("rainforest_floor");
//    public static final ResourceKey<ConfiguredFeature<?, ?>> RAINFOREST_TREES_KEY = registerKey("rainforest_trees");
//    public static final ResourceKey<ConfiguredFeature<?, ?>> RAINFOREST_FLOOR_FULL_KEY = registerKey("rainforest_floor_full");
//    private static final ResourceKey<ConfiguredFeature<?,?>> SUGAR_CANE_KEY = registerKey("sugar_cane");
//    public static final ResourceKey<ConfiguredFeature<?, ?>> RAINFOREST_WATER_PLANTS_FULL_KEY = registerKey("rainforest_water_plants_full");
//    public static final ResourceKey<ConfiguredFeature<?, ?>> RAINFOREST_SEAFLOOR_KEY = registerKey("rainforest_seafloor");
//    public static ConfiguredFeature<?,?> bigDripLeaf;
//    public static ConfiguredFeature<?,?> cactus;
//    public static ConfiguredFeature<?,?> lilypad;
//    public static ConfiguredFeature<?,?> nightreed;
//    public static ConfiguredFeature<?,?> pitaya;
//    public static ConfiguredFeature<?,?> rainforestFlowers;
//    public static ConfiguredFeature<?,?> rainforestGrass;
//    public static ConfiguredFeature<?,?> seagrass;
//    public static ConfiguredFeature<?,?> kelp;
//    public static ConfiguredFeature<?,?> rainforestWaterPlants;
//    public static ConfiguredFeature<?,?> smallDripLeaf;
//    public static ConfiguredFeature<?,?> sugarCane;
//    public static ConfiguredFeature<?,?> rainforestFloor;
//    public static ConfiguredFeature<?,?> rainforestFloorFull;
//    public static ConfiguredFeature<?,?> rainforestWaterPlantsFull;
//    public static ConfiguredFeature<?,?> rainforestTrees;
//    public static ConfiguredFeature<?,?> rainforestSeafloor;
//    public static BlockPredicateFilter hasSturdyBottom=BlockPredicateFilter.forPredicate(BlockPredicate.hasSturdyFace
//            (new Vec3i(0,-1,0),Direction.UP));
//    public static BlockPredicate mossBelow=BlockPredicate.matchesBlocks(Blocks.MOSS_BLOCK);
//    public static BlockPredicate coralBelow=BlockPredicate.matchesTag(BlockTags.CORAL_BLOCKS);
//    public static BlockPredicate mossOrCoralBelow=BlockPredicate.anyOf(mossBelow,coralBelow);
//    public static BlockPredicateFilter mossBelowFilter=BlockPredicateFilter.
//            forPredicate(BlockPredicate.matchesBlocks(new Vec3i(0,-1,0),Blocks.MOSS_BLOCK));
//    public static Supplier<List<BlockState>> rainforestWaterPlantsList=()->List.of(Blocks.HORN_CORAL.
//            defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,true));
//    public static Supplier<List<BlockState>> rainforestFlowersList=()->List.of(Blocks.HORN_CORAL.
//            defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,true));
//    public static Supplier<List<ConfiguredFeature<?,?>>> treesList=()->List.of(sugarCane);
//
//
//    public static final BlockPredicate IS_AIR_PREDICATE=BlockPredicate.matchesBlocks(List.of(Blocks.AIR,
//            Blocks.CAVE_AIR, Blocks.VOID_AIR));
//
//    public static void bootstrap(BootstapContext<ConfiguredFeature<?, ?>> context) {
//        bigDripLeaf=buildBigDripLeaf(context);
//        cactus=buildCactus(context);
//        lilypad=buildLilyPad(context);
//        nightreed=buildNightReed(context);
//        pitaya=register(context, PITAYA_KEY, ModFeatures.PITAYA.get(), new NoneFeatureConfiguration());
//        rainforestFlowers=buildRainforestFlowers(context);
//        rainforestGrass=buildRainforestGrass(context);
//        kelp=register(context, KELP_KEY, ModFeatures.KELP.get(), new NoneFeatureConfiguration());
//        seagrass=register(context, SEAGRASS_KEY, ModFeatures.SEAGRASS.get(), new ProbabilityFeatureConfiguration(0.5F));
//        rainforestWaterPlants=buildRainforestWaterPlants(context);
//        smallDripLeaf=buildSmallDripLeaf(context);
//        sugarCane=buildSugarCane(context);
//        rainforestFloor=buildRainforestFloor(context);
//        rainforestFloorFull=buildFullRainforestFloor(context);
//        rainforestWaterPlantsFull=buildFullRainforestWaterPlants(context);
//        rainforestTrees=buildRainforestTrees(context);
//        rainforestSeafloor=buildRainforestSeafloor(context);
//    }
//
//    private static ConfiguredFeature<?,?> buildRainforestSeafloor(BootstapContext<ConfiguredFeature<?,?>> context) {
//        return register(context, RAINFOREST_SEAFLOOR_KEY, ModFeatures.ALL_SURFACE.get(),
//                new AllSurfacesFeatureConfig(Holder.direct(simplePlacedFeature(rainforestWaterPlantsFull)),
//                        true,mossOrCoralBelow,true));
//    }
//
//    private static ConfiguredFeature<?,?> buildRainforestTrees(BootstapContext<ConfiguredFeature<?,?>> context) {
//        List<WeightedPlacedFeature> list=new ArrayList<>();
//        for(ConfiguredFeature<?,?> tree:treesList.get()){
//            addWeightedFeature(list,simplePlacedFeature(tree),1);
//        }
//        return register(context, RAINFOREST_TREES_KEY, ModFeatures.RANDOM_SELECTION.get(),
//                new RandomSelectionFeatureConfig(list));
//    }
//
//    private static ConfiguredFeature<?,?> buildFullRainforestWaterPlants(BootstapContext<ConfiguredFeature<?,?>> context) {
//        List<WeightedPlacedFeature> list=new ArrayList<>();
//        addWeightedFeature(list,simplePlacedFeature(rainforestWaterPlants,mossBelowFilter),0.3F);
//        addWeightedFeature(list,simplePlacedFeature(seagrass,mossBelowFilter),0.62F);
//        addWeightedFeature(list,simplePlacedFeature(kelp,mossBelowFilter),0.08F);
//        return register(context, RAINFOREST_WATER_PLANTS_FULL_KEY, ModFeatures.RANDOM_SELECTION.get(),
//                new RandomSelectionFeatureConfig(list));
//    }
//
//    private static ConfiguredFeature<?,?> buildFullRainforestFloor(BootstapContext<ConfiguredFeature<?,?>> context) {
//        return register(context, RAINFOREST_FLOOR_FULL_KEY, ModFeatures.ALL_SURFACE.get(),
//                new AllSurfacesFeatureConfig(Holder.direct(simplePlacedFeature(rainforestFloor)),
//                        false,mossBelow,true));
//    }
//
//    public static PlacedFeature simplePlacedFeature(ConfiguredFeature<?,?> feature,PlacementModifier... modifier){
//        return new PlacedFeature(Holder.direct(feature),List.of(modifier));
//    }
//
//    public static ConfiguredFeature<?,?> buildRainforestFloor(BootstapContext<ConfiguredFeature<?,?>> context) {
//        List<WeightedPlacedFeature> list=new ArrayList<>();
//        addWeightedFeature(list,simplePlacedFeature(rainforestGrass,mossBelowFilter),0.8F);
//        int length=rainforestFlowersList.get().size();
//        List<ConfiguredFeature<?,?>> otherFeatures=List.of(cactus,smallDripLeaf,bigDripLeaf,nightreed,sugarCane,pitaya);
//        addWeightedFeature(list,simplePlacedFeature(rainforestFlowers,mossBelowFilter),0.2F/(length+otherFeatures.size())*length);
//        for(ConfiguredFeature<?,?> feature:otherFeatures){
//            addWeightedFeature(list,simplePlacedFeature(feature,mossBelowFilter),0.2F/(length+otherFeatures.size()));
//        }
//        return register(context, RAINFOREST_FLOOR_KEY, ModFeatures.RANDOM_SELECTION.get(), new RandomSelectionFeatureConfig(list));
//    }
//
//    public static ConfiguredFeature<?,?> buildSugarCane(BootstapContext<ConfiguredFeature<?,?>> context) {
//        BlockStateProvider stateProvider=BlockStateProvider.simple(Blocks.SUGAR_CANE.defaultBlockState());
//        IntProvider heightProvider=ConstantInt.of(3);
//        List<BlockColumnConfiguration.Layer> layers=List.of(new BlockColumnConfiguration.Layer(heightProvider,
//                stateProvider));
//        return register(context, SUGAR_CANE_KEY, Feature.BLOCK_COLUMN,
//                new BlockColumnConfiguration(layers, Direction.UP,IS_AIR_PREDICATE,false));
//    }
//
//    public static ConfiguredFeature<?,?> buildSmallDripLeaf(BootstapContext<ConfiguredFeature<?,?>> context) {
//        BlockStateProvider stateProvider=equalChanceHorizotanlFacing(Blocks.SMALL_DRIPLEAF.defaultBlockState());
//        return register(context, SMALL_DRIPLEAF_KEY, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(stateProvider));
//    }
//
//    public static ConfiguredFeature<?,?> buildRainforestGrass(BootstapContext<ConfiguredFeature<?, ?>> context) {
//        SimpleWeightedRandomList.Builder<BlockState> list=SimpleWeightedRandomList.builder();
//        list.add(Blocks.GRASS.defaultBlockState(),50);
//        list.add(Blocks.FERN.defaultBlockState(),12);
//        list.add(Blocks.TALL_GRASS.defaultBlockState(),6);
//        list.add(Blocks.LARGE_FERN.defaultBlockState(),10);
//        return register(context, RAINFOREST_GRASS_KEY, Feature.SIMPLE_BLOCK,
//                new SimpleBlockConfiguration(new WeightedStateProvider(list)));
//    }
//
//    public static ConfiguredFeature<?,?> buildRainforestFlowers(BootstapContext<ConfiguredFeature<?,?>> context) {
//        return register(context, RAINFOREST_FLOWERS_KEY, Feature.SIMPLE_BLOCK,
//                new SimpleBlockConfiguration(equalChance(rainforestFlowersList.get())));
//    }
//    public static ConfiguredFeature<?,?> buildRainforestWaterPlants(BootstapContext<ConfiguredFeature<?,?>> context) {
//        return register(context, RAINFOREST_WATER_PLANTS_KEY, Feature.SIMPLE_BLOCK,
//                new SimpleBlockConfiguration(equalChance(rainforestWaterPlantsList.get())));
//    }
//
//    public static ConfiguredFeature<?,?> buildBigDripLeaf(BootstapContext<ConfiguredFeature<?, ?>> context){
//        BlockStateProvider stateProvider=equalChanceHorizotanlFacing(Blocks.BIG_DRIPLEAF.defaultBlockState());
//        return register(context, BIG_DRIPLEAF_KEY, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(stateProvider));
//    }
//
//    public static ConfiguredFeature<?,?> buildCactus(BootstapContext<ConfiguredFeature<?, ?>> context){
//        BlockStateProvider stateProvider=BlockStateProvider.simple(Blocks.CACTUS.defaultBlockState());
//        IntProvider heightProvider=ConstantInt.of(3);
//        List<BlockColumnConfiguration.Layer> layers=List.of(new BlockColumnConfiguration.Layer(heightProvider,
//                stateProvider));
//        return register(context, CACTUS_KEY, Feature.BLOCK_COLUMN,
//                new BlockColumnConfiguration(layers, Direction.UP,IS_AIR_PREDICATE,false));
//    }
//
//    public static ConfiguredFeature<?,?> buildNightReed(BootstapContext<ConfiguredFeature<?, ?>> context){
//        BlockState state1=MnBlocks.NIGHT_REED.get().defaultBlockState().setValue(NightReedBlock.AGE,0)
//                .setValue(NightReedBlock.END,false).setValue(NightReedBlock.ROOT,false);
//        BlockState state2=state1.setValue(NightReedBlock.END,true);
//        BlockStateProvider stateProvider1=BlockStateProvider.simple(state1);
//        BlockStateProvider stateProvider2=BlockStateProvider.simple(state2);
//        IntProvider heightProvider1=ConstantInt.of(3);
//        IntProvider heightProvider2=ConstantInt.of(1);
//        List<BlockColumnConfiguration.Layer> layers=List.of(new BlockColumnConfiguration.Layer(heightProvider1,
//                stateProvider1),new BlockColumnConfiguration.Layer(heightProvider2,
//                stateProvider2));
//        return register(context, NIGHT_REED_KEY, Feature.BLOCK_COLUMN,
//                new BlockColumnConfiguration(layers, Direction.UP,IS_AIR_PREDICATE,true));
//    }
//
//    public static ConfiguredFeature<?,?> buildLilyPad(BootstapContext<ConfiguredFeature<?, ?>> context){
//        List<WeightedPlacedFeature> list=new ArrayList<>();
//        addWeightedFeature(list,createLilyPadFeature(BWGBlocks.TINY_LILY_PADS.get()),2);
//        addWeightedFeature(list,createLilyPadFeature(BWGBlocks.FLOWERING_TINY_LILY_PADS.get()),2);
//        addWeightedFeature(list,createLilyPadFeature(Blocks.LILY_PAD),1);
//        return register(context, LILY_PAD_KEY, ModFeatures.RANDOM_SELECTION.get(), new RandomSelectionFeatureConfig(list));
//    }
//    private static void addWeightedFeature(List<WeightedPlacedFeature> list, PlacedFeature feature, float weight){
//        list.add(new WeightedPlacedFeature(Holder.direct(feature),weight));
//    }
//    private static PlacedFeature createLilyPadFeature(Block block){
//        BlockPredicateFilter waterBelow=BlockPredicateFilter.forPredicate(BlockPredicate.matchesFluids
//                (new Vec3i(0,-1,0),Fluids.WATER));
//        ConfiguredFeature<?,?> feature=new ConfiguredFeature<>(Feature.RANDOM_PATCH,
//                new RandomPatchConfiguration(10, 7, 3,
//                        PlacedFeaturesUtil.createPlacedFeatureDirect(
//                                ConfiguredFeaturesUtil.createConfiguredFeature(
//                                        Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(
//                                                block))),
//                                BlockPredicateFilter.forPredicate(BlockPredicate.replaceable()))));
//        return new PlacedFeature(Holder.direct(feature),List.of(waterBelow));
//    }
//
//    private static BlockStateProvider equalChanceHorizotanlFacing(BlockState state){
//        List<BlockState> states=new ArrayList<>();
//        BlockStateProperties.HORIZONTAL_FACING.getAllValues().forEach(p->states
//                .add(state.setValue(BlockStateProperties.HORIZONTAL_FACING,p.value())));
//        return equalChance(states);
//    }
//    private static BlockStateProvider equalChance(List<BlockState> states){
//        SimpleWeightedRandomList.Builder<BlockState> list= SimpleWeightedRandomList.builder();
//        for(BlockState state:states){
//            list.add(state,1);
//        }
//        return new WeightedStateProvider(list);
//    }
//
//    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
//        return ResourceKey.create(Registries.CONFIGURED_FEATURE, new ResourceLocation(AOEMod.MOD_ID, name));
//    }
//    private static <FC extends FeatureConfiguration, F extends Feature<FC>> ConfiguredFeature<?,?> register
//            (BootstapContext<ConfiguredFeature<?, ?>> context,
//             ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
//        ConfiguredFeature<?,?> configuredFeature=new ConfiguredFeature<>(feature, configuration);
//        context.register(key, configuredFeature);
//        return configuredFeature;
//    }
}
