package com.example.gpiobridge.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Channel IN block: M5Stack → Minecraft
 *
 * When this block's MQTT channel receives a message, it emits redstone power.
 * Right-click to configure the channel number (1-99).
 * Lights up (luminance 15) when powered.
 */
public class ChannelInBlock extends BaseEntityBlock {

    public static final MapCodec<ChannelInBlock> CODEC = simpleCodec(ChannelInBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public ChannelInBlock(BlockBehaviour.Properties settings) {
        super(settings);
        registerDefaultState(this.stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    // ----- redstone emission -----

    @Override
    protected boolean isSignalSource(BlockState state) { return true; }

    @Override
    protected int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) ? 15 : 0;
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
