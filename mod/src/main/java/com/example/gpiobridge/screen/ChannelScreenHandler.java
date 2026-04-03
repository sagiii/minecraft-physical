package com.example.gpiobridge.screen;

import com.example.gpiobridge.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;

public class ChannelScreenHandler extends ScreenHandler {

    public final BlockPos blockPos;
    private final PropertyDelegate delegate;
    /** Channel value available immediately at init() time on the client. */
    public final int initialChannel;

    private ChannelScreenHandler(int syncId, PlayerInventory inv,
                                  PropertyDelegate delegate, BlockPos pos, int initialChannel) {
        super(ModScreenHandlers.CHANNEL_SCREEN, syncId);
        this.blockPos = pos;
        this.delegate = delegate;
        this.initialChannel = initialChannel;
        addProperties(delegate); // syncs channel value server→client after open
    }

    /** Server-side constructor — called from ChannelBlockEntity.createMenu(). */
    public ChannelScreenHandler(int syncId, PlayerInventory inv,
                                 PropertyDelegate delegate, BlockPos pos) {
        this(syncId, inv, delegate, pos, delegate.get(0));
    }

    /** Client-side constructor — called by ExtendedScreenHandlerType factory. */
    public ChannelScreenHandler(int syncId, PlayerInventory inv, ChannelScreenData data) {
        this(syncId, inv, new ArrayPropertyDelegate(1), data.pos(), data.channel());
    }

    public int getChannel() { return delegate.get(0); }

    @Override
    public boolean canUse(PlayerEntity player) { return true; }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }
}
