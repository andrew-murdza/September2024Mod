package net.amurdza.examplemod.mixins.othermods.nourishednether;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = net.mcreator.nourishednether.block.SoulWeedsBlock.class, remap = false)
public abstract class SoulWeedsNotFlammable {
    @Inject(method = "getFlammability", at = @At("HEAD"), cancellable = true, remap = false)
    private void aoemod$noFlammability(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction direction,
            CallbackInfoReturnable<Integer> cir
    ) {
        cir.setReturnValue(0);
    }

    @Inject(method = "getFireSpreadSpeed", at = @At("HEAD"), cancellable = true, remap = false)
    private void aoemod$noFireSpreadSpeed(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction direction,
            CallbackInfoReturnable<Integer> cir
    ) {
        cir.setReturnValue(0);
    }
}
