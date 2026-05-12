package net.amurdza.examplemod.mixins.othermods.unusualfishmod;

import codyhuh.unusualfishmod.common.entity.util.base.BreedableWaterAnimal;
import codyhuh.unusualfishmod.common.entity.util.movement.SquidMoveControl;
import net.amurdza.examplemod.lava_fish.LavaMobs;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = SquidMoveControl.class,remap = false)
public class CrimsonshellInLava2 {
//    @Redirect(method = "tick",at= @At(value = "INVOKE", target = "Lcodyhuh/unusualfishmod/common/entity/util/base/BreedableWaterAnimal;isEyeInFluid(Lnet/minecraft/tags/TagKey;)Z"))
//    private boolean hi(BreedableWaterAnimal instance, TagKey tagKey){
//        return LavaMobs.isLavaMob(instance)?instance.isEyeInFluid(FluidTags.LAVA):instance.isEyeInFluid(FluidTags.WATER);
//    }
}
