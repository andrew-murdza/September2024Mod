package net.amurdza.examplemod.mixins.client;

import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnchantmentScreen.class)
public class EnchantmentScreenNoLevelLimit {
    @Redirect(
            method = {
                    "renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V",
                    "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"
            },
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/LocalPlayer;experienceLevel:I")
    )
    private int aoemod$ignoreLevelRequirement(LocalPlayer player) {
        return Integer.MAX_VALUE;
    }

    @Redirect(
            method = {
                    "renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V",
                    "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"
            },
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/EnchantmentMenu;getGoldCount()I")
    )
    private int aoemod$useCustomLapisThresholds(EnchantmentMenu menu) {
        int lapis = menu.getGoldCount();
        if (lapis >= 32) {
            return 3;
        }
        if (lapis >= 8) {
            return 2;
        }
        if (lapis >= 4) {
            return 1;
        }

        return 0;
    }
}
