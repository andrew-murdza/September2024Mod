package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.util.RandomCollection;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.RandomSelectorFeature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RandomSelectorFeature.class)
public class RandomCollectionMixin {
    @Redirect(method = "place",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/feature/WeightedPlacedFeature;place(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z"))
    public boolean hi(WeightedPlacedFeature instance, WorldGenLevel pLevel, ChunkGenerator pChunkGenerator, RandomSource pRandom, BlockPos pPos, FeaturePlaceContext<RandomFeatureConfiguration> context){
        RandomFeatureConfiguration randomfeatureconfiguration = context.config();
        RandomCollection<WeightedPlacedFeature> features=new RandomCollection<>();
        for(WeightedPlacedFeature feature:randomfeatureconfiguration.features){
            features.add(feature.chance,feature);
        }
        return features.next(pRandom).place(pLevel,pChunkGenerator,pRandom,pPos);
    }
}
