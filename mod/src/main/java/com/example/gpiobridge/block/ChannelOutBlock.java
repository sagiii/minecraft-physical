package com.example.gpiobridge.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Channel OUT block: Minecraft → M5Stack
 *
 * When redstone power is applied/removed, publishes to the configured MQTT channel.
 * Right-click to configure the channel number (1-99).
 * Lights up (luminance 15) when powered.
 */
public class ChannelOutBlock extends BlockWithEntity {

    public static final MapCodec<ChannelOutBlock> CODEC = createCodec(ChannelOutBlock::new);
    public static final BooleanProperty POWERED = Properties.POWERED;

    public ChannelOutBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(POWERED, false));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() { return CODEC; }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    // ----- redstone input detection -----

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos,
                                   Block sourceBlock, @Nullable net.minecraft.block.WireOrientation wireOrientation,
                                   boolean notify) {
        if (world.isClient) return;
        boolean powered = world.isReceivingRedstonePower(pos);
        if (powered != state.get(POWERED)) {
            world.setBlockState(pos, state.with(POWERED, powered), Block.NOTIFY_ALL);
            if (world.getBlockEntity(pos) instanceof ChannelBlockEntity be) {
                be.sendToMqtt(powered);
            }
        }
    }

    // ----- right-click: open channel GUI -----

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                  PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient) {
            if (world.getBlockEntity(pos) instanceof ChannelBlockEntity be) {
                player.openHandledScreen(be);
            }
        }
        return ActionResult.SUCCESS;
    }

    // ----- block entity -----

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ChannelBlockEntity(pos, state);
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
}
