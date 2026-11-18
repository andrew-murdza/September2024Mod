package net.amurdza.examplemod.mixins.client;

import net.amurdza.examplemod.util.CreativeCatalog;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.common.CreativeModeTabRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CompactCreativeTabs {
    @Redirect(method = "init", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/common/CreativeModeTabRegistry;getSortedCreativeModeTabs()Ljava/util/List;", remap = false))
    private List<CreativeModeTab> aoe$packVisibleTabsBeforePagination() {
        return CreativeCatalog.visibleTabs(CreativeModeTabRegistry.getSortedCreativeModeTabs());
    }
}
