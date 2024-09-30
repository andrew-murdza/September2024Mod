package net.amurdza.examplemod.mixins.othermods.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.EntityGiantSquid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityGiantSquid.class)
public class GiantSquidNoInk {
    @Redirect(method = "hurt", at= @At(value = "INVOKE", target = "Lcom/github/alexthe666/alexsmobs/entity/EntityGiantSquid;spawnInk()V"))
    private void hi(EntityGiantSquid instance){

    }
}
