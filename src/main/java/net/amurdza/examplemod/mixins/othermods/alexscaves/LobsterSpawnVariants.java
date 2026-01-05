package net.amurdza.examplemod.mixins.othermods.alexscaves;

import com.github.alexthe666.alexsmobs.entity.EntityLobster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(EntityLobster.class)
public class LobsterSpawnVariants {
    @ModifyArg(method = "finalizeSpawn", at= @At(value = "INVOKE", target = "Lcom/github/alexthe666/alexsmobs/entity/EntityLobster;setVariant(I)V",remap = false),remap = false)
    private int hi(int variant){
        return 0;
    }
}
