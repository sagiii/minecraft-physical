package com.example.gpiobridge.screen;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ChannelScreenData(BlockPos pos, int channel) {

    public static final StreamCodec<FriendlyByteBuf, ChannelScreenData> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,  ChannelScreenData::pos,
                    ByteBufCodecs.VAR_INT,  ChannelScreenData::channel,
                    ChannelScreenData::new
            );
}
