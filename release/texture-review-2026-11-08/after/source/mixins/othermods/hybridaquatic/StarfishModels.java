package net.amurdza.examplemod.mixins.othermods.hybridaquatic;

import dev.hybridlabs.aquatic.client.model.entity.critter.StarfishEntityModel;
import dev.hybridlabs.aquatic.entity.critter.StarfishEntity;
import dev.hybridlabs.aquatic.entity.critter.StarfishEntity.Companion.Type;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A unique resource namespace prevents upstream pack priority from restoring upright planes. */
@Mixin(value = StarfishEntityModel.class, remap = false)
public abstract class StarfishModels {
    @Inject(method = "getModelResource(Ldev/hybridlabs/aquatic/entity/critter/StarfishEntity;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("RETURN"), cancellable = true)
    private void aoemod$flatStarfishModel(StarfishEntity entity, CallbackInfoReturnable<ResourceLocation> cir) {
        var variant = entity.getVariant();
        if (variant == Type.SMALL) {
            // The native small atlas paints the inner cubes of the shared model.
            // Keep those UVs while giving them the medium variant's full footprint.
            cir.setReturnValue(new ResourceLocation("aoemod", "geo/critter/starfish/starfish_small.geo.json"));
        } else if (variant == Type.MEDIUM) {
            cir.setReturnValue(new ResourceLocation("aoemod", "geo/critter/starfish/starfish.geo.json"));
        } else if (variant == Type.CROWN_OF_THORNS) {
            cir.setReturnValue(new ResourceLocation("aoemod", "geo/critter/starfish/starfish_crown_of_thorns.geo.json"));
        }
    }
}
