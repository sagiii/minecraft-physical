package com.example.gpiobridge.screen;

import com.example.gpiobridge.ModScreenHandlers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class CameraIdScreenHandler extends AbstractContainerMenu {

    public final int entityId;
    public final int initialCamId;

    /** Server-side constructor. */
    public CameraIdScreenHandler(int syncId, Inventory inv, int entityId, int camId) {
        super(ModScreenHandlers.CAMERA_ID_SCREEN, syncId);
        this.entityId    = entityId;
        this.initialCamId = camId;
    }

    /** Client-side constructor — called by ExtendedScreenHandlerType factory. */
    public CameraIdScreenHandler(int syncId, Inventory inv, CameraIdScreenData data) {
        this(syncId, inv, data.entityId(), data.camId());
    }

    @Override
    public boolean stillValid(Player player) { return true; }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
}
