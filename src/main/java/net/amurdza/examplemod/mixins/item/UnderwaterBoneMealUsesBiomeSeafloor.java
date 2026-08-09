package net.amurdza.examplemod.mixins.item;

import net.amurdza.examplemod.event_handlers.BoneMeal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BoneMealItem.class)
public class UnderwaterBoneMealUsesBiomeSeafloor {
    @Inject(method = "growWaterPlant", at = @At("HEAD"), cancellable = true)
    private static void aoemod$growBiomeSeafloor(
            ItemStack stack,
            Level level,
            BlockPos pos,
            Direction clickedSide,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (clickedSide == Direction.DOWN || !BoneMeal.canGrowBiomeLiquidSurface(level, pos)) {
            return;
        }

        if (level instanceof ServerLevel serverLevel) {
            BoneMeal.growBiomeLiquidSurface(serverLevel, pos);
            stack.shrink(1);
        }

        cir.setReturnValue(true);
    }
}
