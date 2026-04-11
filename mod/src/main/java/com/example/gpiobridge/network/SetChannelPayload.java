package com.example.gpiobridge.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SetChannelPayload(BlockPos pos, int channel) implements CustomPacketPayload {

    public static final Type<SetChannelPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("mp_bridge", "set_channel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetChannelPayload> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,    SetChannelPayload::pos,
                    ByteBufCodecs.VAR_INT,    SetChannelPayload::channel,
                    SetChannelPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
