package net.amurdza.examplemod.mixins.mob_spawning;

import net.amurdza.examplemod.util.Helper;
import net.amurdza.examplemod.worldgen.WorldGenUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.GlowSquid;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GlowSquid.class)
public class GlowSquidCanSpawnInLight {
    @Redirect(method = "checkGlowSquideSpawnRules",at=@At(value = "INVOKE",target = "Lnet/minecraft/world/level/ServerLevelAccessor;getSeaLevel()I"))
    private static int hi1(ServerLevelAccessor instance, EntityType<? extends LivingEntity> pType,
                           ServerLevelAccessor pLevel, MobSpawnType pSpawnType, BlockPos pPos, RandomSource pRandom){
        return Helper.isSpecialBiome(instance,pPos)?1000: WorldGenUtils.getSeaLevelAtPos(pPos);
    }
}
