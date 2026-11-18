package net.amurdza.examplemod.mixins.mob_behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Turtle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Search the nearby shore when ready to lay, regardless of the remembered birthplace. */
@Mixin(targets = "net.minecraft.world.entity.animal.Turtle$TurtleLayEggGoal")
public abstract class TurtleLaysNearbyEggs {
    @Redirect(method = {"canUse", "canContinueToUse"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/Turtle;getHomePos()Lnet/minecraft/core/BlockPos;"))
    private BlockPos aoemod$useCurrentShore(Turtle turtle) {
        return turtle.blockPosition();
    }

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 16))
    private static int aoemod$nearbyShoreSearch(int original) {
        return 48;
    }
}
