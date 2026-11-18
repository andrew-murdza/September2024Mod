package net.amurdza.examplemod.mixins.othermods.hybridaquatic;

import dev.hybridlabs.aquatic.client.model.entity.cephalopod.UmbrellaOctopusEntityModel;
import dev.hybridlabs.aquatic.entity.cephalopod.UmbrellaOctopusEntity;
import net.amurdza.examplemod.util.TropicalUmbrellaVariants;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = UmbrellaOctopusEntityModel.class, remap = false)
public abstract class UmbrellaOctopusColors {
    @Inject(method = "getTextureResource(Ldev/hybridlabs/aquatic/entity/cephalopod/UmbrellaOctopusEntity;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("HEAD"), cancellable = true)
    private void aoemod$coloredOctopus(UmbrellaOctopusEntity entity, CallbackInfoReturnable<ResourceLocation> cir) {
        String color = TropicalUmbrellaVariants.color(entity);
        cir.setReturnValue(new ResourceLocation("hybrid_aquatic",
                "textures/entity/cephalopod/umbrella_octopus/umbrella_octopus_" + color + ".png"));
    }
}
