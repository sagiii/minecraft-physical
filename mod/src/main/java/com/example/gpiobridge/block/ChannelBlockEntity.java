package com.example.gpiobridge.block;

import com.example.gpiobridge.ModBlockEntityTypes;
import com.example.gpiobridge.network.MqttBridgeClient;
import com.example.gpiobridge.screen.ChannelScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ChannelBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory<BlockPos> {

    private int channel = 0; // 0 = unset, 1-99 = active

    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override public int get(int index)             { return index == 0 ? channel : 0; }
        @Override public void set(int index, int value) { if (index == 0) applyChannel(value); }
        @Override public int size()                     { return 1; }
    };

    public ChannelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.CHANNEL_BLOCK_ENTITY, pos, state);
    }

    // ----- channel management -----

    public int getChannel() { return channel; }

    /** Called both from screen (via PropertyDelegate) and from server packet handler. */
    public void setChannel(int newChannel) {
        applyChannel(newChannel);
    }

    private void applyChannel(int value) {
        int clamped = Math.max(0, Math.min(99, value));
        if (clamped == channel) return;
        int old = channel;
        channel = clamped;
        if (world != null && !world.isClient) {
            if (old > 0) MqttBridgeClient.INSTANCE.unregister(old, pos);
            registerWithMqtt();
            markDirty();
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    @Override
    public void setWorld(World world) {
        super.setWorld(world);
        if (!world.isClient && channel > 0) registerWithMqtt();
    }

    @Override
    public void markRemoved() {
        super.markRemoved();
        if (channel > 0) MqttBridgeClient.INSTANCE.unregister(channel, pos);
    }

    private void registerWithMqtt() {
        if (channel <= 0) return;
        Block block = getCachedState().getBlock();
        if (block instanceof ChannelInBlock) {
            MqttBridgeClient.INSTANCE.registerIn(channel, pos);
        } else if (block instanceof ChannelOutBlock) {
            MqttBridgeClient.INSTANCE.registerOut(channel, pos);
        }
    }

    // ----- called by MqttBridgeClient (server thread) -----

    public void updateFromMqtt(boolean value, World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof ChannelInBlock)) return;
        if (state.get(ChannelInBlock.POWERED) != value) {
            world.setBlockState(pos, state.with(ChannelInBlock.POWERED, value));
        }
    }

    // ----- called by ChannelOutBlock when redstone changes -----

    public void sendToMqtt(boolean powered) {
        if (channel <= 0) return;
        MqttBridgeClient.INSTANCE.publish(channel, powered);
    }

    // ----- persistence -----

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        nbt.putInt("channel", channel);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        channel = nbt.getInt("channel");
    }

    @Override
    public @Nullable Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        return createNbt(registries);
    }

    // ----- screen factory -----

    @Override
    public BlockPos getScreenOpeningData(ServerPlayerEntity player) { return pos; }

    @Override
    public Text getDisplayName() {
        return Text.translatable(getCachedState().getBlock().getTranslationKey());
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new ChannelScreenHandler(syncId, playerInventory, propertyDelegate, pos);
    }
}
