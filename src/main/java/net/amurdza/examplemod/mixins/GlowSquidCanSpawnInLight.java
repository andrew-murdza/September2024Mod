package net.amurdza.examplemod.mixins;

import net.amurdza.examplemod.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.GlowSquid;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(GlowSquid.class)
public class GlowSquidCanSpawnInLight {
    @Redirect(method = "checkGlowSquideSpawnRules",at=@At(value = "INVOKE",target = "Lnet/minecraft/world/level/ServerLevelAccessor;getRawBrightness(Lnet/minecraft/core/BlockPos;I)I"))
    private static int hi(ServerLevelAccessor instance, BlockPos blockPos, int i){
        return Helper.isSpecialBiome(instance,blockPos)?0:instance.getRawBrightness(blockPos,i);
    }
}
