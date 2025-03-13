package net.amurdza.examplemod.mixins.othermods.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.AMEntityRegistry;
import net.minecraft.world.entity.SpawnPlacements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AMEntityRegistry.class)
public class NoFloatingEndergradeSpawn {
    @Redirect(method = "initializeAttributes",at= @At(value = "FIELD", target = "Lnet/minecraft/world/entity/SpawnPlacements$Type;NO_RESTRICTIONS:Lnet/minecraft/world/entity/SpawnPlacements$Type;",ordinal = 1))
    private static SpawnPlacements.Type hi(){
        return SpawnPlacements.Type.ON_GROUND;
    }
}
