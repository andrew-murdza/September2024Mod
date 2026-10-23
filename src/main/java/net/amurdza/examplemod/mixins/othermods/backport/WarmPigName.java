package net.amurdza.examplemod.mixins.othermods.backport;

import com.blackgear.vanillabackport.common.api.modules.mob_variant.VariantDataHolder;
import com.blackgear.vanillabackport.common.api.modules.mob_variant.VariantUtils;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.pig.PigVariant;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.pig.PigVariants;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Pig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class WarmPigName {
    @Inject(method = "getTypeName", at = @At("HEAD"), cancellable = true)
    private void aoemod$warmPigsAreBoars(CallbackInfoReturnable<Component> cir) {
        if ((Object) this instanceof Pig pig && VariantDataHolder.<PigVariant>getHolder(pig)
                .flatMap(VariantDataHolder::getVariantData)
                .filter(variant -> VariantUtils.matches(PigVariants.REGISTRIES, variant, PigVariants.WARM))
                .isPresent()) {
            cir.setReturnValue(Component.translatable("entity.aoemod.boar"));
        }
    }
}
