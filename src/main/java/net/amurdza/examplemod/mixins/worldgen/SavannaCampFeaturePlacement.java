package net.amurdza.examplemod.mixins.worldgen;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.amurdza.examplemod.worldgen.SavannaCampVegetation;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PlacedFeature.class)
public abstract class SavannaCampFeaturePlacement {
    @WrapMethod(method = "placeWithBiomeCheck")
    private boolean aoemod$protectAuthoredCampGardens(WorldGenLevel level, ChunkGenerator generator,
            RandomSource random, BlockPos origin, Operation<Boolean> original) {
        if (!SavannaCampVegetation.isVegetationFeature(level, (PlacedFeature) (Object) this)) {
            return original.call(level, generator, random, origin);
        }
        return SavannaCampVegetation.natural(level, () -> original.call(level, generator, random, origin));
    }
}
