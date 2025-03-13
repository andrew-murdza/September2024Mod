package net.amurdza.examplemod.mixins.disabled;

import net.minecraft.world.level.block.MossBlock;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MossBlock.class)
public class MossDoesntBlockLightPart2 extends MossDoesntBlockLightPart1 {
//    @Override
//    protected boolean hi(VoxelShape pShape){
//        return false;
//    }
//    @Override
//    protected boolean hi(FluidState instance){
//        return true;
//    }
}
