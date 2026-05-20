package net.amurdza.examplemod.mixins.block_placement;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.MushroomBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MushroomBlock.class)
public class MushroomsSurviveInDaylight {
    @WrapOperation(method = "canSurvive",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelReader;getRawBrightness(Lnet/minecraft/core/BlockPos;I)I"))
    private int hi(LevelReader instance, BlockPos blockPos, int i, Operation<Integer> original){
        return 0;
    }
}
