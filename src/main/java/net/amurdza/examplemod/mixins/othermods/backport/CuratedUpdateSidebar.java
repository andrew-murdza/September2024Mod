package net.amurdza.examplemod.mixins.othermods.backport;

import net.amurdza.examplemod.util.RetainedContent;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** The native update selector must honor the same exclusions as its parent tab. */
@Mixin(targets = "com.blackgear.vanillabackport.client.api.bundled_tabs.BundledTabs", remap = false)
public abstract class CuratedUpdateSidebar {
    @Shadow @Final private List<ItemStack> displayItems;

    @Inject(method = "populate", at = @At("TAIL"))
    private void aoemod$filterUpdateItems(HolderLookup.Provider registries, CallbackInfo ci) {
        displayItems.removeIf(stack -> !RetainedContent.isKeptItem(stack));
    }
}
