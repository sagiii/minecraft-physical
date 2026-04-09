package com.example.gpiobridge;

import com.example.gpiobridge.network.MqttBridgeClient;
import com.example.gpiobridge.network.SetChannelPayload;
import com.example.gpiobridge.block.ChannelBlockEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;

public class GpioBridgeMod implements ModInitializer {

    @Override
    public void onInitialize() {
        // Registration order matters: blocks → block entities → screen handlers
        ModBlocks.initialize();
        ModBlockEntityTypes.initialize();
        ModScreenHandlers.initialize();

        // Register the C2S packet for setting channel number from GUI
        PayloadTypeRegistry.playC2S().register(SetChannelPayload.TYPE, SetChannelPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SetChannelPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerLevel world = context.player().serverLevel();
                if (world.getBlockEntity(payload.pos()) instanceof ChannelBlockEntity be) {
                    be.setChannel(payload.channel());
                }
            });
        });

        // Start / stop MQTT client with the server lifecycle
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                MqttBridgeClient.INSTANCE.start(server));
        ServerLifecycleEvents.SERVER_STOPPING.register(server ->
                MqttBridgeClient.INSTANCE.stop());
    }
}
