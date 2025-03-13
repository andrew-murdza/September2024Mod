package net.amurdza.examplemod.mixins.mob_spawning;

import net.amurdza.examplemod.util.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Animal.class)
public abstract class MobsSpawnUnderTreesInRainforest {
    @Shadow
    protected static boolean isBrightEnoughToSpawn(BlockAndTintGetter pLevel, BlockPos pPos) {
        return pLevel.getRawBrightness(pPos, 0) > 8;
    }

    @Redirect(method = "checkAnimalSpawnRules",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Animal;isBrightEnoughToSpawn(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)Z"))
    private static boolean hi(BlockAndTintGetter pLevel, BlockPos pPos, EntityType<? extends Animal> pAnimal, LevelAccessor pLevel1, MobSpawnType pSpawnType, BlockPos pPos1, RandomSource pRandom){
        return isBrightEnoughToSpawn(pLevel,pPos)|| Helper.isSpecialBiome(pLevel1,pPos1);
    }
}
