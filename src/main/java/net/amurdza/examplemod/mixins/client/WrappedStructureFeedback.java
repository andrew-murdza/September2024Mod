package net.amurdza.examplemod.mixins.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep encounter explanations readable at the player's actual window size and GUI scale. */
@Mixin(ForgeGui.class)
public abstract class WrappedStructureFeedback extends Gui {
    @Shadow(remap = false) public int leftHeight;
    @Shadow(remap = false) public int rightHeight;

    protected WrappedStructureFeedback(Minecraft minecraft, ItemRenderer itemRenderer) {
        super(minecraft, itemRenderer);
    }

    @Inject(method = "renderRecordOverlay", at = @At("HEAD"), cancellable = true, remap = false)
    private void aoemod$wrapProtectionMessage(int width, int height, float partialTick, GuiGraphics graphics,
                                              CallbackInfo callback) {
        if (overlayMessageTime <= 0 || overlayMessageString == null
                || !(overlayMessageString.getContents() instanceof TranslatableContents text)) return;
        String key = text.getKey();
        if (!(key.startsWith("message.aoemod.camp_") || key.startsWith("message.aoemod.mansion_")
                || key.startsWith("message.aoemod.nether_citadel_"))) return;
        var font = getFont();
        var lines = font.split(overlayMessageString, Math.max(1, width - 24));
        if (lines.size() < 2) return;
        callback.cancel();
        float remaining = overlayMessageTime - partialTick;
        int opacity = Mth.clamp((int) (remaining * 255.0F / 20.0F), 0, 255);
        if (opacity <= 8) return;
        int color = (animateOverlayMessageColor ? Mth.hsvToRgb(remaining / 50.0F, 0.7F, 0.6F) : 0xFFFFFF)
                | opacity << 24;
        int yShift = Math.max(68, Math.max(leftHeight, rightHeight) + 9);
        int spacing = font.lineHeight + 2;
        graphics.pose().pushPose();
        graphics.pose().translate(width / 2.0, height - yShift, 0);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (int index = 0; index < lines.size(); index++) {
            var line = lines.get(index);
            int lineWidth = font.width(line);
            int y = -4 - (lines.size() - 1 - index) * spacing;
            drawBackdrop(graphics, font, y, lineWidth, color);
            graphics.drawString(font, line, -lineWidth / 2, y, color);
        }
        RenderSystem.disableBlend();
        graphics.pose().popPose();
    }
}
