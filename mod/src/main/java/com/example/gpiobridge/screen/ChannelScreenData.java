package com.example.gpiobridge.screen;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.math.BlockPos;

public record ChannelScreenData(BlockPos pos, int channel) {

    public static final PacketCodec<PacketByteBuf, ChannelScreenData> CODEC =
            PacketCodec.tuple(
                    BlockPos.PACKET_CODEC,  ChannelScreenData::pos,
                    PacketCodecs.VAR_INT,   ChannelScreenData::channel,
                    ChannelScreenData::new
            );
}
