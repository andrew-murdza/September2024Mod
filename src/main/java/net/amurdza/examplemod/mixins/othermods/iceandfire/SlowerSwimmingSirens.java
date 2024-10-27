package net.amurdza.examplemod.mixins.othermods.iceandfire;

import com.github.alexthe666.iceandfire.entity.EntitySiren;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(targets = "com.github.alexthe666.iceandfire.entity.EntitySiren$SwimmingMoveHelper")
public class SlowerSwimmingSirens {
    @ModifyConstant(method = "tick",constant = @Constant(floatValue = 1.0F,ordinal = 0))
    private float hi(float constant){
        return 0.2F;
    }
}
