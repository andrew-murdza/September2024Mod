package net.amurdza.examplemod.mixins.othermods.legumedelight;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.pawjwp.legumedelight.item.LegumeDelightItems", remap = false)
public abstract class PeanutSeeds {
    // The pinned Legume Delight version registers its peanuts through this supplier.
    @Inject(method = "lambda$static$3", at = @At("HEAD"), cancellable = true)
    private static void aoe$registerPlantingSeeds(CallbackInfoReturnable<Item> cir) {
        cir.setReturnValue(new ItemNameBlockItem(net.ribs.vintagedelight.block.ModBlocks.PEANUT_CROP.get(),
                new Item.Properties()));
    }
}
