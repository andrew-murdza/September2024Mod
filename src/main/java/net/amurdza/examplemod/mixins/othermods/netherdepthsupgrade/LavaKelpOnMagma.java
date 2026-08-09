package net.amurdza.examplemod.mixins.othermods.netherdepthsupgrade;

import com.scouter.netherdepthsupgrade.blocks.CrimsonKelpBlock;
import com.scouter.netherdepthsupgrade.blocks.WarpedKelpBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {CrimsonKelpBlock.class, WarpedKelpBlock.class}, remap = false)
public class LavaKelpOnMagma {
    @Inject(method = "canAttachTo", at = @At("HEAD"), cancellable = true)
    private void aoemod$allowMagma(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (state.is(Blocks.MAGMA_BLOCK)) {
            cir.setReturnValue(true);
        }
    }
}
