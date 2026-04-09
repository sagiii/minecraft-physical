package com.example.gpiobridge.screen;

import com.example.gpiobridge.network.SetChannelPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Simple GUI for setting the channel number on a Channel IN / OUT block.
 * Sends a SetChannelPayload packet to the server when the player confirms.
 */
public class ChannelScreen extends AbstractContainerScreen<ChannelScreenHandler> {

    private EditBox channelField;
    private static final int BG_W = 200;
    private static final int BG_H = 90;

    public ChannelScreen(ChannelScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        imageWidth  = BG_W;
        imageHeight = BG_H;
        inventoryLabelY = BG_H + 4; // hide vanilla inventory label
    }

    @Override
    protected void init() {
        super.init();

        channelField = new EditBox(
                font,
                leftPos + 10, topPos + 38,
                100, 20,
                Component.translatable("gui.gpio_bridge.channel_label")
        );
        channelField.setMaxLength(2);
        channelField.setValue(String.valueOf(handler.initialChannel == 0 ? 1 : handler.initialChannel));
        channelField.setFocused(true);
        addRenderableWidget(channelField);

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), btn -> confirm())
                .bounds(leftPos + 120, topPos + 38, 70, 20)
                .build());
    }

    private void confirm() {
        int ch = 0;
        try {
            ch = Integer.parseInt(channelField.getValue().trim());
        } catch (NumberFormatException ignored) {}
        ch = Math.max(1, Math.min(99, ch));
        ClientPlayNetworking.send(new SetChannelPayload(handler.blockPos, ch));
        onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enter key confirms
        if (keyCode == 257 /* GLFW_KEY_ENTER */ || keyCode == 335 /* GLFW_KEY_KP_ENTER */) {
            confirm();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
        // Dark semi-transparent panel
        context.fill(leftPos, topPos, leftPos + BG_W, topPos + BG_H, 0xD0101010);
        context.fill(leftPos + 1, topPos + 1, leftPos + BG_W - 1, topPos + BG_H - 1, 0xC0202020);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        // Title
        context.drawString(font, getTitle(), leftPos + 10, topPos + 10, 0xFFFFFF, true);
        // Label
        context.drawString(font,
                Component.translatable("gui.gpio_bridge.channel_label"),
                leftPos + 10, topPos + 28, 0xAAAAAA, true);
    }

    // Hide vanilla slot rendering — this screen has no inventory slots
    @Override
    protected void renderLabels(GuiGraphics context, int mouseX, int mouseY) {}
}
