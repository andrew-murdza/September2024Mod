package net.amurdza.examplemod.client;

import net.amurdza.examplemod.portal.PortalDestination;
import net.amurdza.examplemod.portal.PortalNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public final class PortalSelectionScreen extends Screen {
    private final UUID token;
    private final int destinations;
    private int top;

    public PortalSelectionScreen(UUID token, int destinations) {
        super(Component.translatable("portal.aoemod.choose_destination"));
        this.token = token;
        this.destinations = destinations;
    }

    @Override
    protected void init() {
        int count = Integer.bitCount(destinations & 127);
        top = Math.max(32, (height - count * 25 - 60) / 2);
        int y = top + 28;
        for (PortalDestination destination : PortalDestination.values()) {
            if ((destinations & (1 << destination.ordinal())) == 0) {
                continue;
            }
            addRenderableWidget(Button.builder(Component.translatable(destination.translationKey())
                    .withStyle(style -> style.withColor(destination.color())), button -> {
                PortalNetwork.select(token, destination);
                onClose();
            }).bounds(width / 2 - 100, y, 200, 20).build());
            y += 25;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(width / 2 - 100, y + 6, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, top + 8, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
