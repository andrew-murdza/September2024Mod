package net.amurdza.examplemod.mixins.lava_and_water;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public class FluidsNotPlaceableInWrongBiomes {
    @Inject(method = "emptyContents(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void aoemod$dimensionFluids(Player player, Level level, BlockPos pos, BlockHitResult hit,
                                        ItemStack container, CallbackInfoReturnable<Boolean> cir) {
        if (!GameplayDimensions.isCustom(level)) return;
        Fluid content = ((BucketItem) (Object) this).getFluid();
        if ((content.isSame(Fluids.LAVA) && !GameplayDimensions.is(level, GameplayDimensions.DESERT)
                && !GameplayDimensions.is(level, GameplayDimensions.NETHER) && !GameplayDimensions.is(level, GameplayDimensions.SNOWY))
                || (content.isSame(Fluids.WATER) && GameplayDimensions.is(level, GameplayDimensions.NETHER))) {
            cir.setReturnValue(false);
        }
    }
}
