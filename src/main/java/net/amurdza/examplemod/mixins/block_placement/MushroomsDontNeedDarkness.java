package net.amurdza.examplemod.mixins.block_placement;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.MushroomBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MushroomBlock.class)
public class MushroomsDontNeedDarkness {
    @Redirect(method = "canSurvive",
            at = @At(value = "INVOKE",
                    target ="Lnet/minecraft/world/level/LevelReader;getRawBrightness(Lnet/minecraft/core/BlockPos;I)I"))
    private int canPlaceAt(LevelReader instance, BlockPos blockPos, int i){
        return 0;//instance.is(tag)||instance.is(Blocks.MOSS_BLOCK);
    }
}
