package com.example.gpiobridge.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Channel OUT block: Minecraft → M5Stack
 *
 * When redstone power is applied/removed, publishes to the configured MQTT channel.
 * Right-click to configure the channel number (1-99).
 * Lights up (luminance 15) when powered.
 */
public class ChannelOutBlock extends BaseEntityBlock {

    public static final MapCodec<ChannelOutBlock> CODEC = simpleCodec(ChannelOutBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public ChannelOutBlock(BlockBehaviour.Properties settings) {
        super(settings);
        registerDefaultState(this.stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    // ----- redstone input detection -----

    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos,
                                    Block sourceBlock, @Nullable Orientation wireOrientation,
                                    boolean notify) {
        if (world.isClientSide()) return;
        boolean powered = world.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            world.setBlock(pos, state.setValue(POWERED, powered), 3);
            if (world.getBlockEntity(pos) instanceof ChannelBlockEntity be) {
                be.sendToMqtt(powered);
            }
        }
    }

    // ----- right-click: open channel GUI -----

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (!world.isClientSide()) {
            if (world.getBlockEntity(pos) instanceof ChannelBlockEntity be) {
                ((ServerPlayer) player).openMenu(be);
            }
        }
        return InteractionResult.SUCCESS;
    }

    // ----- block entity -----

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChannelBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
