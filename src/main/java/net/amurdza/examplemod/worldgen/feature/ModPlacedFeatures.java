package net.amurdza.examplemod.worldgen.feature;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.util.valueproviders.WeightedListInt;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.*;
import org.checkerframework.checker.units.qual.A;

import java.util.List;

public class ModPlacedFeatures {
    public static final ResourceKey<PlacedFeature> RAINFOREST_SEAFLOOR_KEY = registerKey("rainforest_seafloor");
    public static final ResourceKey<PlacedFeature> RAINFOREST_FLOOR_KEY = registerKey("rainforest_floor");
    public static final ResourceKey<PlacedFeature> RAINFOREST_TREES_KEY = registerKey("rainforest_trees");
    public static final ResourceKey<PlacedFeature> LILY_PADS_KEY = registerKey("lilypads");
    public static PlacedFeature rainforestSeaFloor;
    public static PlacedFeature rainforestFloor;
    public static PlacedFeature rainforestTrees;
    public static PlacedFeature lilyPads;

    public static void bootstrap(BootstapContext<PlacedFeature> context) {
        rainforestSeaFloor=register(context,RAINFOREST_SEAFLOOR_KEY,ModConfiguredFeatures.rainforestSeafloor, BiomeFilter.biome());
        rainforestFloor=register(context,RAINFOREST_FLOOR_KEY,ModConfiguredFeatures.rainforestFloor, BiomeFilter.biome());
        rainforestTrees=register(context,RAINFOREST_TREES_KEY,ModConfiguredFeatures.rainforestTrees, BiomeFilter.biome());
        lilyPads=buildLilyPads(context);
    }

    private static PlacedFeature buildLilyPads(BootstapContext<PlacedFeature> context) {
        SimpleWeightedRandomList.Builder<IntProvider> list= SimpleWeightedRandomList.builder();
        list.add(ConstantInt.of(0),3);
        list.add(ConstantInt.of(4),1);
        IntProvider count=new WeightedListInt(list.build());
        return register(context,LILY_PADS_KEY,ModConfiguredFeatures.lilypad,BiomeFilter.biome(),
                InSquarePlacement.spread(), HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG),CountPlacement.of(count));
    }

    private static ResourceKey<PlacedFeature> registerKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, new ResourceLocation(AOEMod.MOD_ID, name));
    }

    private static PlacedFeature register(BootstapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key, ConfiguredFeature<?, ?> configuration,
                                 PlacementModifier... modifiers) {
        PlacedFeature feature=new PlacedFeature(Holder.direct(configuration), List.of(modifiers));
        context.register(key, feature);
        return feature;
    }
}
