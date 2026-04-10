package com.example.gpiobridge.block;

import com.example.gpiobridge.ModBlockEntityTypes;
import com.example.gpiobridge.network.MqttBridgeClient;
import com.example.gpiobridge.screen.ChannelScreenData;
import com.example.gpiobridge.screen.ChannelScreenHandler;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ChannelBlockEntity extends BlockEntity implements ExtendedMenuProvider<ChannelScreenData> {

    private int channel = 0; // 0 = unset, 1-99 = active

    private final ContainerData propertyDelegate = new ContainerData() {
        @Override public int get(int index)             { return index == 0 ? channel : 0; }
        @Override public void set(int index, int value) { if (index == 0) applyChannel(value); }
        @Override public int getCount()                 { return 1; }
    };

    public ChannelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.CHANNEL_BLOCK_ENTITY, pos, state);
    }

    // ----- channel management -----

    public int getChannel() { return channel; }

    /** Called both from screen (via ContainerData) and from server packet handler. */
    public void setChannel(int newChannel) {
        applyChannel(newChannel);
    }

    private void applyChannel(int value) {
        int clamped = Math.max(0, Math.min(99, value));
        if (clamped == channel) return;
        int old = channel;
        channel = clamped;
        if (level != null && !level.isClientSide()) {
            if (old > 0) MqttBridgeClient.INSTANCE.unregister(old, worldPosition);
            registerWithMqtt();
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (!level.isClientSide() && channel > 0) registerWithMqtt();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (channel > 0) MqttBridgeClient.INSTANCE.unregister(channel, worldPosition);
    }

    private void registerWithMqtt() {
        if (channel <= 0) return;
        Block block = getBlockState().getBlock();
        if (block instanceof ChannelInBlock) {
            MqttBridgeClient.INSTANCE.registerIn(channel, worldPosition);
        } else if (block instanceof ChannelOutBlock) {
            MqttBridgeClient.INSTANCE.registerOut(channel, worldPosition);
        }
    }

    // ----- called by MqttBridgeClient (server thread) -----

    public void updateFromMqtt(boolean value, Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof ChannelInBlock)) return;
        if (state.getValue(ChannelInBlock.POWERED) != value) {
            world.setBlock(pos, state.setValue(ChannelInBlock.POWERED, value), 3);
        }
    }

    // ----- called by ChannelOutBlock when redstone changes -----

    public void sendToMqtt(boolean powered) {
        if (channel <= 0) return;
        MqttBridgeClient.INSTANCE.publish(channel, powered);
    }

    // ----- persistence -----

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("channel", channel);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        channel = input.getIntOr("channel", 0);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    // ----- screen factory -----

    @Override
    public ChannelScreenData getScreenOpeningData(ServerPlayer player) {
        return new ChannelScreenData(worldPosition, channel);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new ChannelScreenHandler(syncId, playerInventory, propertyDelegate, worldPosition);
    }
}
