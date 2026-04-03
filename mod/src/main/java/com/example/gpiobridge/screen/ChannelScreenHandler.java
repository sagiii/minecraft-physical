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

    /** Server-side constructor — called from ChannelBlockEntity.createMenu(). */
    public ChannelScreenHandler(int syncId, PlayerInventory inv,
                                 PropertyDelegate delegate, BlockPos pos) {
        super(ModScreenHandlers.CHANNEL_SCREEN, syncId);
        this.blockPos = pos;
        this.delegate = delegate;
        addProperties(delegate); // syncs channel value to client automatically
    }

    /** Client-side constructor — called by ExtendedScreenHandlerType factory. */
    public ChannelScreenHandler(int syncId, PlayerInventory inv, BlockPos pos) {
        this(syncId, inv, new ArrayPropertyDelegate(1), pos);
    }

    public int getChannel() { return delegate.get(0); }

    @Override
    public boolean canUse(PlayerEntity player) { return true; }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }
}
