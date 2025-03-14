package net.amurdza.examplemod.mixins.special_biome.mob_spawning;

import net.amurdza.examplemod.util.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.GlowSquid;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.text.ParsePosition;

@Mixin(GlowSquid.class)
public class GlowSquidCanSpawnInLight {
    @Redirect(method = "checkGlowSquideSpawnRules",at=@At(value = "INVOKE",target = "Lnet/minecraft/world/level/ServerLevelAccessor;getRawBrightness(Lnet/minecraft/core/BlockPos;I)I"))
    private static int hi(ServerLevelAccessor instance, BlockPos blockPos, int i){
        return Helper.isSpecialBiome(instance,blockPos)?0:instance.getRawBrightness(blockPos,i);
    }
    @Redirect(method = "checkGlowSquideSpawnRules",at=@At(value = "INVOKE",target = "Lnet/minecraft/world/level/ServerLevelAccessor;getSeaLevel()I"))
    private static int hi1(ServerLevelAccessor instance, EntityType<? extends LivingEntity> pType,
                           ServerLevelAccessor pLevel, MobSpawnType pSpawnType, BlockPos pPos, RandomSource pRandom){
        return Helper.isSpecialBiome(instance,pPos)?1000:instance.getSeaLevel();
    }
}
