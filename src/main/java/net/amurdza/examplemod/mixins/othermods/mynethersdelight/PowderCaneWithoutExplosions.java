package net.amurdza.examplemod.mixins.othermods.mynethersdelight;

import com.soytutta.mynethersdelight.common.block.PowderyCaneBlock;
import com.soytutta.mynethersdelight.common.block.PowderyCannonBlock;
import com.soytutta.mynethersdelight.common.block.PowderyFlowerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The cane, tall cannon, and naturally grown flower top each define this detonation entry point. */
@Mixin({PowderyCaneBlock.class, PowderyCannonBlock.class, PowderyFlowerBlock.class})
public abstract class PowderCaneWithoutExplosions {
    @Inject(method = "explodeAndReset", at = @At("HEAD"), cancellable = true, remap = false)
    private void aoemod$disableDetonation(Level level, BlockPos pos, BlockState state, CallbackInfo ci) {
        // Covers walking into a mature stalk, stepping on it, harvesting it,
        // and scheduled pressure ticks, including plants saved before this fix.
        ci.cancel();
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void aoemod$harvestCaneThroughItsLootTable(BlockState state, Level level, BlockPos pos,
                                                      Player player, InteractionHand hand, BlockHitResult hit,
                                                      CallbackInfoReturnable<InteractionResult> cir) {
        // Native shears/knife interactions bypass the block loot table and spawn
        // peppers directly. Block loot controls the cane and mature-top pepper drops.
        cir.setReturnValue(InteractionResult.PASS);
    }
}
