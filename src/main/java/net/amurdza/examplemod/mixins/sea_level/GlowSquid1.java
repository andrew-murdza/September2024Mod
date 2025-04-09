package net.amurdza.examplemod.mixins.sea_level;

import net.amurdza.examplemod.util.Helper;
import net.amurdza.examplemod.worldgen.WorldGenUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.GlowSquid;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GlowSquid.class)
public class GlowSquid1 {
    @Redirect(method = "checkGlowSquideSpawnRules",at=@At(value = "INVOKE",target = "Lnet/minecraft/world/level/ServerLevelAccessor;getSeaLevel()I"))
    private static int hi1(ServerLevelAccessor instance, EntityType<? extends LivingEntity> pType,
                           ServerLevelAccessor pLevel, MobSpawnType pSpawnType, BlockPos pPos, RandomSource pRandom){
        int h=WorldGenUtils.getTotalWaterAbove(pPos,pLevel);
        return h>=10?10000:-10000;
    }
}