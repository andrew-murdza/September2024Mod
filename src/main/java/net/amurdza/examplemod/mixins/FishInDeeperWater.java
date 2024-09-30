package net.amurdza.examplemod.mixins;

import net.amurdza.examplemod.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(WaterAnimal.class)
public class FishInDeeperWater {
    @Redirect(method = "checkSurfaceWaterAnimalSpawnRules",at=@At(value = "INVOKE",target = "Lnet/minecraft/core/BlockPos;getY()I",ordinal = 0))
    private static int hi(BlockPos instance, EntityType<? extends WaterAnimal> pWaterAnimal, LevelAccessor pLevel, MobSpawnType pSpawnType, BlockPos pPos, RandomSource pRandom){
        return -aOEMod1_20_1V2$findInt(pLevel,pPos, instance);
    }
    @Redirect(method = "checkSurfaceWaterAnimalSpawnRules",at=@At(value = "INVOKE",target = "Lnet/minecraft/core/BlockPos;getY()I",ordinal = 1))
    private static int hi1(BlockPos instance, EntityType<? extends WaterAnimal> pWaterAnimal, LevelAccessor pLevel, MobSpawnType pSpawnType, BlockPos pPos, RandomSource pRandom){
        return aOEMod1_20_1V2$findInt(pLevel,pPos, instance);
    }

    @Unique
    private static int aOEMod1_20_1V2$findInt(LevelAccessor pLevel, BlockPos pPos, BlockPos instance){
        return Helper.isSpecialBiome(pLevel,pPos)?(pLevel.getRawBrightness(pPos,0)<6?10000:-10000):instance.getY();
    }
}
