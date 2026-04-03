package com.example.gpiobridge.screen;

import com.example.gpiobridge.network.SetChannelPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

/**
 * Simple GUI for setting the channel number on a Channel IN / OUT block.
 * Sends a SetChannelPayload packet to the server when the player confirms.
 */
public class ChannelScreen extends HandledScreen<ChannelScreenHandler> {

    private TextFieldWidget channelField;
    private static final int BG_W = 200;
    private static final int BG_H = 90;

    public ChannelScreen(ChannelScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth  = BG_W;
        backgroundHeight = BG_H;
        playerInventoryTitleY = BG_H + 4; // hide vanilla inventory label
    }

    @Override
    protected void init() {
        super.init();

        channelField = new TextFieldWidget(
                textRenderer,
                x + 10, y + 38,
                100, 20,
                Text.translatable("gui.gpio_bridge.channel_label")
        );
        channelField.setMaxLength(2);
        channelField.setText(String.valueOf(handler.initialChannel == 0 ? 1 : handler.initialChannel));
        channelField.setFocused(true);
        addDrawableChild(channelField);

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), btn -> confirm())
                .dimensions(x + 120, y + 38, 70, 20)
                .build());
    }

    private void confirm() {
        int ch = 0;
        try {
            ch = Integer.parseInt(channelField.getText().trim());
        } catch (NumberFormatException ignored) {}
        ch = Math.max(1, Math.min(99, ch));
        ClientPlayNetworking.send(new SetChannelPayload(handler.blockPos, ch));
        close();
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
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        // Dark semi-transparent panel
        context.fill(x, y, x + BG_W, y + BG_H, 0xD0101010);
        context.fill(x + 1, y + 1, x + BG_W - 1, y + BG_H - 1, 0xC0202020);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        // Title
        context.drawTextWithShadow(textRenderer,
                getTitle(), x + 10, y + 10, 0xFFFFFF);
        // Label
        context.drawTextWithShadow(textRenderer,
                Text.translatable("gui.gpio_bridge.channel_label"),
                x + 10, y + 28, 0xAAAAAA);
    }

    // Hide vanilla slot rendering — this screen has no inventory slots
    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {}
}
