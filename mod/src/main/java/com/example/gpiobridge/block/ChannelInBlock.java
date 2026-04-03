package com.example.gpiobridge.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Channel IN block: M5Stack → Minecraft
 *
 * When this block's MQTT channel receives a message, it emits redstone power.
 * Right-click to configure the channel number (1-99).
 * Lights up (luminance 15) when powered.
 */
public class ChannelInBlock extends BlockWithEntity {

    public static final MapCodec<ChannelInBlock> CODEC = createCodec(ChannelInBlock::new);
    public static final BooleanProperty POWERED = Properties.POWERED;

    public ChannelInBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(POWERED, false));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() { return CODEC; }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    // ----- redstone emission -----

    @Override
    protected boolean emitsRedstonePower(BlockState state) { return true; }

    @Override
    protected int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return state.get(POWERED) ? 15 : 0;
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
