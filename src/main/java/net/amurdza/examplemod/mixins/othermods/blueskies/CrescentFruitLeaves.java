package net.amurdza.examplemod.mixins.othermods.blueskies;

import com.legacy.blue_skies.blocks.natural.FruitBlock;
import com.legacy.blue_skies.blocks.natural.HangingFruitLeavesBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HangingFruitLeavesBlock.class)
public class CrescentFruitLeaves {
    @Redirect(method = "randomTick",at= @At(value = "INVOKE", target = "Lcom/legacy/blue_skies/blocks/natural/FruitBlock;isOvercrowded(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean hi(FruitBlock instance, ServerLevel z, BlockPos x){
        return false;
    }
}
