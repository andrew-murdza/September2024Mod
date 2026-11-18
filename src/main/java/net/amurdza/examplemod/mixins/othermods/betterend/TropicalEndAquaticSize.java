package net.amurdza.examplemod.mixins.othermods.betterend;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** These fish encode their individual scale as a random byte from zero through fifteen. */
@Mixin(value = {net.amurdza.examplemod.entity.EndFishEntity.class,
        net.amurdza.examplemod.entity.CubozoaEntity.class}, remap = false)
public abstract class TropicalEndAquaticSize {
    @Inject(method = "getByteScale", at = @At("HEAD"), cancellable = true)
    private void aoemod$maximumScale(CallbackInfoReturnable<Byte> cir) {
        if (GameplayDimensions.is(((Entity) (Object) this).level(), GameplayDimensions.TROPICAL)) cir.setReturnValue((byte) 15);
    }

    @Inject(method = {"getBucketItemStack", "m_28282_"}, at = @At("RETURN"))
    private void aoemod$maximumBucketScale(CallbackInfoReturnable<ItemStack> cir) {
        if (GameplayDimensions.is(((Entity) (Object) this).level(), GameplayDimensions.TROPICAL)) {
            var tag = cir.getReturnValue().getOrCreateTag();
            tag.putByte(tag.contains("scale") ? "scale" : "Scale", (byte) 15);
        }
    }
}
