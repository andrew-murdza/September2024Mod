package net.amurdza.examplemod.mixins.othermods.twilightforest;

import net.amurdza.examplemod.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import twilightforest.item.MagicBeansItem;

@Mixin(MagicBeansItem.class)
public class TallerBeanStalkPart2 {
    @Redirect(method = "useOn",at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I"))
    private int hi(int a, int b){
        return Config.MAX_BEANSTALK_Y;
    }

}
