package net.amurdza.examplemod.mixins.othermods.netherdepthsupgrade;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.scouter.netherdepthsupgrade.entity.LavaAnimal;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LavaAnimal.class,remap = false)
public class LavaFishSpawning {
    @WrapOperation(method = "checkSurfaceLavaAnimalSpawnRules",at= @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;getY()I"))
    private static int hi(BlockPos instance, Operation<Integer> original){
        return 20;
    }
}
