package com.example.gpiobridge.screen;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record CameraIdScreenData(int entityId, int camId) {

    public static final StreamCodec<RegistryFriendlyByteBuf, CameraIdScreenData> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, CameraIdScreenData::entityId,
                    ByteBufCodecs.VAR_INT, CameraIdScreenData::camId,
                    CameraIdScreenData::new
            );
}
