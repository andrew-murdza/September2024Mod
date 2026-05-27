package net.amurdza.examplemod.mixins.othermods.wooden_bucket;

import cech12.bucketlib.api.item.UniversalBucketItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = UniversalBucketItem.class,remap = false)
public abstract class WoodenBucketMixin {

    @Unique
    private static final ResourceLocation WOODEN_BUCKET_ID =
            new ResourceLocation("woodenbucket", "wooden_bucket");

    @Unique
    private boolean aoemod$isWoodenBucket() {
        Item self = (Item) (Object) this;
        return WOODEN_BUCKET_ID.equals(ForgeRegistries.ITEMS.getKey(self));
    }

    /**
     * Makes the wooden bucket last forever by making BucketLib think it has 0 durability.
     * UniversalBucketItem#isDamageable checks getDurability() > 0,
     * so returning 0 here disables normal durability behavior.
     */
    @Inject(method = "getDurability", at = @At("HEAD"), cancellable = true, remap = false)
    private void aoemod$removeWoodenBucketDurability(CallbackInfoReturnable<Integer> cir) {
        if (aoemod$isWoodenBucket()) {
            cir.setReturnValue(0);
        }
    }

    /**
     * Extra safety: force the item to be non-damageable even if something checks this directly.
     */
    @Inject(method = "isDamageable", at = @At("HEAD"), cancellable = true)
    private void aoemod$woodenBucketIsNotDamageable(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (aoemod$isWoodenBucket()) {
            cir.setReturnValue(false);
        }
    }

    /**
     * Extra safety: force max damage to 0.
     */
    @Inject(method = "getMaxDamage", at = @At("HEAD"), cancellable = true)
    private void aoemod$woodenBucketMaxDamage(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (aoemod$isWoodenBucket()) {
            cir.setReturnValue(0);
        }
    }

    /**
     * Prevents the wooden bucket from picking up lava.
     * Fluid pickup eventually checks UniversalBucketItem#canHoldFluid.
     * Reject both source lava and flowing lava just in case.
     */
    @Inject(method = "canHoldFluid", at = @At("HEAD"), cancellable = true, remap = false)
    private void aoemod$woodenBucketCannotHoldLava(Fluid fluid, CallbackInfoReturnable<Boolean> cir) {
        if (aoemod$isWoodenBucket() && (fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA)) {
            cir.setReturnValue(false);
        }
    }
}