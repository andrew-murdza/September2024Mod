package net.amurdza.examplemod.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.level.BlockAndTintGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MushroomCow.class)
public class MooshroomSpawnInDarkness {
    @Redirect(method = "checkMushroomSpawnRules",at=@At(value = "INVOKE",target = "Lnet/minecraft/world/entity/animal/MushroomCow;isBrightEnoughToSpawn(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)Z"))
    private static boolean hi(BlockAndTintGetter blockAndTintGetter, BlockPos blockPos){
        return true;
    }
}
