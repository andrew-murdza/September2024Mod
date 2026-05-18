package net.amurdza.examplemod.mixins.mob_spawning;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Turtle.class)
public class TurtlesSpawnInWater {

    @Redirect(method = "checkTurtleSpawnRules",at= @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;getY()I"))
    private static int hi(BlockPos instance){
        return -128;
    }
    @Redirect(method = "checkTurtleSpawnRules",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/TurtleEggBlock;onSand(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z"))
    private static boolean hi1(BlockGetter pLevel, BlockPos pPos){
        int numBelow=4;
        if(!pLevel.getFluidState(pPos.above()).is(Fluids.WATER)){
            return false;
        }
        for(int i=0;i<numBelow;i++){
            if(!pLevel.getFluidState(pPos.below(i)).is(Fluids.WATER)){
                return false;
            }
        }
        return true;
    }
}
