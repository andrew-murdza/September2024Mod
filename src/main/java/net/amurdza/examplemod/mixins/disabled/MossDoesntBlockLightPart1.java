package net.amurdza.examplemod.mixins.disabled;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static net.minecraft.world.level.block.Block.isShapeFullBlock;

@Mixin(Block.class)
public class MossDoesntBlockLightPart1 {
//    @Redirect(method = "propagatesSkylightDown",at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;isShapeFullBlock(Lnet/minecraft/world/phys/shapes/VoxelShape;)Z"))
//    protected boolean hi(VoxelShape pShape){
//        return isShapeFullBlock(pShape);
//    }
//    @Redirect(method = "propagatesSkylightDown",at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;isEmpty()Z"))
//    protected boolean hi(FluidState instance){
//        return instance.isEmpty();
//    }
}
