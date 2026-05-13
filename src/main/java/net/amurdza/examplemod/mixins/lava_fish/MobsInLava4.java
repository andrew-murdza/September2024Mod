package net.amurdza.examplemod.mixins.lava_fish;

import net.amurdza.examplemod.lava_fish.LavaMobs;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GroundPathNavigation.class)
public abstract class MobsInLava4 extends PathNavigation {
    public MobsInLava4(Mob pMob, Level pLevel) {
        super(pMob, pLevel);
    }

    /**
     * Vanilla GroundPathNavigation rejects both WATER and LAVA.
     * For your lava mobs, allow LAVA but still reject WATER.
     */
    @Inject(
            method = "hasValidPathType",
            at = @At("HEAD"),
            cancellable = true
    )
    private void aoemod$allowLavaPathType(BlockPathTypes pathType, CallbackInfoReturnable<Boolean> cir) {
        if (LavaMobs.isLavaMob(this.mob)) {
            if (pathType == BlockPathTypes.WATER) {
                cir.setReturnValue(false);
            } else if (pathType == BlockPathTypes.LAVA) {
                cir.setReturnValue(true);
            }
        }
    }
    @Redirect(method = "getSurfaceY",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;isInWater()Z"))
    private boolean hi(Mob instance){
        if(LavaMobs.isLavaMob(instance)){
            return instance.isInLava();
        }
        return instance.isInWater();
    }
    @Redirect(method = "getSurfaceY",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
    private boolean hi(BlockState instance, Block block){
        if(LavaMobs.isLavaMob(mob)){
            return instance.is(Blocks.LAVA);
        }
        return instance.is(block);
    }
}
