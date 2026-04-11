package com.example.gpiobridge.screen;

import com.example.gpiobridge.network.SetCameraIdPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * GUI for changing the Camera ID by right-clicking a CameraEntity.
 * Sends a SetCameraIdPayload packet to the server when confirmed.
 */
public class CameraIdScreen extends AbstractContainerScreen<CameraIdScreenHandler> {

    private EditBox idField;
    private static final int BG_W = 200;
    private static final int BG_H = 90;

    public CameraIdScreen(CameraIdScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, BG_W, BG_H);
        inventoryLabelY = BG_H + 4;
    }

    @Override
    protected void init() {
        super.init();

        idField = new EditBox(
                font,
                leftPos + 10, topPos + 38,
                100, 20,
                Component.translatable("gui.gpio_bridge.camera_id_label")
        );
        idField.setMaxLength(2);
        idField.setValue(String.valueOf(menu.initialCamId == 0 ? 1 : menu.initialCamId));
        idField.setFocused(true);
        addRenderableWidget(idField);

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), btn -> confirm())
                .bounds(leftPos + 120, topPos + 38, 70, 20)
                .build());
    }

    private void confirm() {
        int id = 1;
        try {
            id = Integer.parseInt(idField.getValue().trim());
        } catch (NumberFormatException ignored) {}
        id = Math.max(1, Math.min(99, id));
        ClientPlayNetworking.send(new SetCameraIdPayload(menu.entityId, id));
        onClose();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 257 || event.key() == 335) {
            confirm();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(leftPos, topPos, leftPos + BG_W, topPos + BG_H, 0xD0101010);
        context.fill(leftPos + 1, topPos + 1, leftPos + BG_W - 1, topPos + BG_H - 1, 0xC0202020);
        super.extractContents(context, mouseX, mouseY, delta);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.text(font, getTitle(), leftPos + 10, topPos + 10, 0xFFFFFFFF, true);
        context.text(font,
                Component.translatable("gui.gpio_bridge.camera_id_label"),
                leftPos + 10, topPos + 28, 0xFFAAAAAA, true);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {}
}
