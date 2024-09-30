package net.amurdza.examplemod.mixins.othermods.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.EntityBananaSlug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityBananaSlug.class)
public class NoBananaSlugGriefing {
    @Redirect(method = "tick",at= @At(value = "INVOKE", target = "Lcom/github/alexthe666/alexsmobs/entity/EntityBananaSlug;isBaby()Z"))
    private boolean hi(EntityBananaSlug instance){
        return true;
    }
}
