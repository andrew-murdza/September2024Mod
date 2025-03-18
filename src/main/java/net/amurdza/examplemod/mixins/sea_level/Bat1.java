package net.amurdza.examplemod.mixins.sea_level;

import net.amurdza.examplemod.worldgen.WorldGenUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Bat.class)
public class Bat1 {
    @Redirect(method = "checkBatSpawnRules",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelAccessor;getSeaLevel()I"))
    private static int hi(LevelAccessor instance, EntityType<Bat> pBat, LevelAccessor pLevel, MobSpawnType pSpawnType,
                          BlockPos pPos, RandomSource pRandom){
        return WorldGenUtils.getSeaLevelAtPos(pPos);
    }
}
