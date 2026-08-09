package net.amurdza.examplemod.mixins.block_placement;

import net.amurdza.examplemod.util.BiomePlacementRestrictions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(BlockItem.class)
public abstract class BlockItemBiomeRestrictions {
    @Shadow
    public abstract Block getBlock();

    @Shadow
    @Nullable
    protected abstract BlockState getPlacementState(BlockPlaceContext context);

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void aoemod$preventWrongBiomePlacement(
            BlockPlaceContext context,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        Level level = context.getLevel();
        BlockState state = this.getPlacementState(context);
        if (state == null) {
            state = this.getBlock().defaultBlockState();
        }

        Optional<Component> failure = BiomePlacementRestrictions.placementFailureMessage(
                level,
                context.getClickedPos(),
                state
        );

        if (failure.isEmpty()) {
            return;
        }

        Player player = context.getPlayer();
        if (!level.isClientSide && player != null) {
            player.displayClientMessage(failure.get(), true);
        }

        cir.setReturnValue(InteractionResult.FAIL);
    }
}
