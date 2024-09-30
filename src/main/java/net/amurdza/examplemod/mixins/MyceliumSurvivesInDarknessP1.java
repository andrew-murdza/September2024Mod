package net.amurdza.examplemod.mixins;


import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.SpreadingSnowyDirtBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(SpreadingSnowyDirtBlock.class)
public class MyceliumSurvivesInDarknessP1 {
    @Redirect(method = "randomTick",
            at = @At(value = "INVOKE",
                    target ="Lnet/minecraft/server/level/ServerLevel;getMaxLocalRawBrightness(Lnet/minecraft/core/BlockPos;)I"))
    protected int getLight(ServerLevel instance, BlockPos blockPos){
        return instance.getMaxLocalRawBrightness(blockPos);
    }
}
