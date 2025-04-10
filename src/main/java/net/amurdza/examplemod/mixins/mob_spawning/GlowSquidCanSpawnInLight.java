package net.amurdza.examplemod.mixins.mob_spawning;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.GlowSquid;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GlowSquid.class)
public class GlowSquidCanSpawnInLight {
    @Redirect(method = "checkGlowSquideSpawnRules",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerLevelAccessor;getRawBrightness(Lnet/minecraft/core/BlockPos;I)I"))
    private static int hi(ServerLevelAccessor instance, BlockPos blockPos, int i){
        return 0;
    }
}
