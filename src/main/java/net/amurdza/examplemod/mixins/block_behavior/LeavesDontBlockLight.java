package net.amurdza.examplemod.mixins.block_behavior;

import net.minecraft.world.level.block.LeavesBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(LeavesBlock.class)
public class LeavesDontBlockLight {
    @ModifyConstant(method = "getLightBlock",constant = @Constant(intValue = 1))
    private int hi(int constant){
        return 0;
    }
}
