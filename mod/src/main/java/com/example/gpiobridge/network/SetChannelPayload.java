package com.example.gpiobridge.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record SetChannelPayload(BlockPos pos, int channel) implements CustomPayload {

    public static final Id<SetChannelPayload> ID =
            new Id<>(Identifier.of("gpio_bridge", "set_channel"));

    public static final PacketCodec<PacketByteBuf, SetChannelPayload> CODEC =
            PacketCodec.tuple(
                    BlockPos.PACKET_CODEC,    SetChannelPayload::pos,
                    PacketCodecs.VAR_INT,     SetChannelPayload::channel,
                    SetChannelPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
