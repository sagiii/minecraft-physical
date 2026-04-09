package com.example.gpiobridge.screen;

import com.example.gpiobridge.ModScreenHandlers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public class ChannelScreenHandler extends AbstractContainerMenu {

    public final BlockPos blockPos;
    private final ContainerData delegate;
    /** Channel value available immediately at init() time on the client. */
    public final int initialChannel;

    private ChannelScreenHandler(int syncId, Inventory inv,
                                  ContainerData delegate, BlockPos pos, int initialChannel) {
        super(ModScreenHandlers.CHANNEL_SCREEN, syncId);
        this.blockPos = pos;
        this.delegate = delegate;
        this.initialChannel = initialChannel;
        addDataSlots(delegate); // syncs channel value server→client after open
    }

    /** Server-side constructor — called from ChannelBlockEntity.createMenu(). */
    public ChannelScreenHandler(int syncId, Inventory inv,
                                 ContainerData delegate, BlockPos pos) {
        this(syncId, inv, delegate, pos, delegate.get(0));
    }

    /** Client-side constructor — called by ExtendedScreenHandlerType factory. */
    public ChannelScreenHandler(int syncId, Inventory inv, ChannelScreenData data) {
        this(syncId, inv, new SimpleContainerData(1), data.pos(), data.channel());
    }

    public int getChannel() { return delegate.get(0); }

    @Override
    public boolean stillValid(Player player) { return true; }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
}
