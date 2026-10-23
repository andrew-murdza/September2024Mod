package net.amurdza.examplemod.mixins.worldgen;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.amurdza.examplemod.worldgen.AtomicTreeGrowth;
import net.minecraft.world.level.levelgen.feature.AbstractHugeMushroomFeature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.HugeFungusFeature;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({TreeFeature.class, HugeFungusFeature.class, AbstractHugeMushroomFeature.class})
public class AtomicTreeAndFungusGrowth {
    @WrapMethod(method = "place")
    private <C extends FeatureConfiguration> boolean aoemod$validateWholeGrowth(
            FeaturePlaceContext<C> context, Operation<Boolean> original) {
        return AtomicTreeGrowth.place(context, staged -> original.call(staged));
    }
}
