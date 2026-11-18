package net.amurdza.examplemod.mixins.othermods.mynethersdelight;

import com.soytutta.mynethersdelight.common.block.PowderyCaneBlock;
import com.soytutta.mynethersdelight.common.block.PowderyCannonBlock;
import com.soytutta.mynethersdelight.common.registry.MNDBlocks;
import net.amurdza.examplemod.worldgen.PowderCaneShape;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PowderyCaneBlock.class, PowderyCannonBlock.class})
public abstract class PowderCaneSeedPlacement {
    @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
    private void aoemod$plantPepper(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        BlockState result = cir.getReturnValue();
        if (result == null || PowderCaneShape.isPlant(context.getLevel().getBlockState(context.getClickedPos().below()))) return;
        BlockState pepper = MNDBlocks.BULLET_PEPPER.get().defaultBlockState();
        if (pepper.canSurvive(context.getLevel(), context.getClickedPos())) cir.setReturnValue(pepper);
    }
}
