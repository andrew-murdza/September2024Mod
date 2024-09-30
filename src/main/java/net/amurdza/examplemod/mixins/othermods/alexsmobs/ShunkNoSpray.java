package net.amurdza.examplemod.mixins.othermods.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.EntitySkunk;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntitySkunk.class)
public class ShunkNoSpray {
    @Redirect(method = "tick", at= @At(value = "FIELD", target = "Lnet/minecraft/world/level/Level;isClientSide:Z"))
    private boolean hi(Level instance){
        return false;
    }
}
