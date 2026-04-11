package com.example.gpiobridge.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SetCameraIdPayload(int entityId, int newCamId) implements CustomPacketPayload {

    public static final Type<SetCameraIdPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("mp_bridge", "set_camera_id"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetCameraIdPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SetCameraIdPayload::entityId,
                    ByteBufCodecs.VAR_INT, SetCameraIdPayload::newCamId,
                    SetCameraIdPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
