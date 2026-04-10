package com.example.gpiobridge.screen;

import com.example.gpiobridge.network.SetChannelPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
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
        super(handler, inventory, title, BG_W, BG_H);
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
        channelField.setValue(String.valueOf(menu.initialChannel == 0 ? 1 : menu.initialChannel));
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
        ClientPlayNetworking.send(new SetChannelPayload(menu.blockPos, ch));
        onClose();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // Enter key confirms
        if (event.key() == 257 /* GLFW_KEY_ENTER */ || event.key() == 335 /* GLFW_KEY_KP_ENTER */) {
            confirm();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Dark semi-transparent panel (drawn first, behind widgets)
        context.fill(leftPos, topPos, leftPos + BG_W, topPos + BG_H, 0xD0101010);
        context.fill(leftPos + 1, topPos + 1, leftPos + BG_W - 1, topPos + BG_H - 1, 0xC0202020);
        // Super renders widgets (EditBox, Button) via Screen.extractRenderState
        super.extractContents(context, mouseX, mouseY, delta);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        // Title — ARGB format: 0xFF prefix for full opacity
        context.text(font, getTitle(), leftPos + 10, topPos + 10, 0xFFFFFFFF, true);
        // Label
        context.text(font,
                Component.translatable("gui.gpio_bridge.channel_label"),
                leftPos + 10, topPos + 28, 0xFFAAAAAA, true);
    }

    // Hide vanilla slot rendering — this screen has no inventory slots
    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {}
}
