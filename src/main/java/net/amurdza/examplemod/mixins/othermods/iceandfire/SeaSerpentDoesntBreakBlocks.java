package net.amurdza.examplemod.mixins.othermods.iceandfire;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntitySeaSerpent.class)
public class SeaSerpentDoesntBreakBlocks {
    @Redirect(method = "aiStep",at= @At(value = "INVOKE", target = "Lcom/github/alexthe666/iceandfire/entity/EntitySeaSerpent;breakBlock()V"))
    private void hi(EntitySeaSerpent instance){

    }
}
