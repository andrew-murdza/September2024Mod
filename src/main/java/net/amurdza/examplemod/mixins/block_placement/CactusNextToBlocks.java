package net.amurdza.examplemod.mixins.block_placement;

import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CactusBlock.class)
public class CactusNextToBlocks {
    @Redirect(method = "canSurvive",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isSolid()Z"))
    private boolean hi(BlockState instance){
        return false;
    }
}
