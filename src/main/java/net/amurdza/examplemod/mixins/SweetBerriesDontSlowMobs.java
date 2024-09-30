package net.amurdza.examplemod.mixins;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(SweetBerryBushBlock.class)
public class SweetBerriesDontSlowMobs {
    @Redirect(method = "entityInside",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;makeStuckInBlock(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/Vec3;)V"))
    private void cancelSlow(Entity instance, BlockState pState, Vec3 pMotionMultiplier){}
}
