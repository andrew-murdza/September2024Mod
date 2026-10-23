package net.amurdza.examplemod.mixins.othermods.backport;

import net.amurdza.examplemod.future.FutureContentPolicy;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class MigrateFutureItemStacks {
    @Inject(method = "of", at = @At("RETURN"), cancellable = true)
    private static void aoe$migrateExistingSpearsAndCopper(CompoundTag tag, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack original = cir.getReturnValue();
        ItemStack migrated = FutureContentPolicy.canonicalStack(original);
        if (migrated != original) cir.setReturnValue(migrated);
    }
}
