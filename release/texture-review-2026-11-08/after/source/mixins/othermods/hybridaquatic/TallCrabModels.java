package net.amurdza.examplemod.mixins.othermods.hybridaquatic;

import dev.hybridlabs.hapi.client.model.entity.aquatic.BaseCrustaceanEntityModel;
import dev.hybridlabs.hapi.entity.base.aquatic.BaseCrustaceanEntity;
import net.amurdza.examplemod.util.AquaticMobSizes;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep native textures while selecting the raised model and grounded gait. */
@Mixin(value = BaseCrustaceanEntityModel.class, remap = false)
public abstract class TallCrabModels {
    @Inject(method = "getModelResource(Ldev/hybridlabs/hapi/entity/base/aquatic/BaseCrustaceanEntity;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("HEAD"), cancellable = true)
    private void aoemod$tallModel(BaseCrustaceanEntity crab, CallbackInfoReturnable<ResourceLocation> cir) {
        if (!AquaticMobSizes.isTallCrab(crab)) return;
        String id = ForgeRegistries.ENTITY_TYPES.getKey(crab.getType()).getPath();
        cir.setReturnValue(new ResourceLocation("aoemod", "geo/crustacean/" + id + "/" + id + ".geo.json"));
    }

    @Inject(method = "getAnimationResource(Ldev/hybridlabs/hapi/entity/base/aquatic/BaseCrustaceanEntity;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("HEAD"), cancellable = true)
    private void aoemod$groundedGait(BaseCrustaceanEntity crab, CallbackInfoReturnable<ResourceLocation> cir) {
        if (!AquaticMobSizes.isTallCrab(crab)) return;
        String id = ForgeRegistries.ENTITY_TYPES.getKey(crab.getType()).getPath();
        cir.setReturnValue(new ResourceLocation("aoemod", "animations/entity/crustacean/" + id + "/" + id + ".animation.json"));
    }
}
