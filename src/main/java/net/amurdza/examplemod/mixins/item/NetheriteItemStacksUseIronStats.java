package net.amurdza.examplemod.mixins.item;

import net.amurdza.examplemod.util.NetheriteIronStats;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class NetheriteItemStacksUseIronStats {
    @Shadow
    public abstract Item getItem();

    @Inject(method = "getMaxDamage", at = @At("HEAD"), cancellable = true)
    private void aoemod$useIronDurability(CallbackInfoReturnable<Integer> cir) {
        Integer maxDamage = NetheriteIronStats.maxDamage((ItemStack) (Object) this);
        if (maxDamage != null) {
            cir.setReturnValue(maxDamage);
        }
    }

    @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
    private void aoemod$useIronMiningSpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
        Float speed = NetheriteIronStats.destroySpeed((ItemStack) (Object) this, state);
        if (speed != null) {
            cir.setReturnValue(speed);
        }
    }
}
