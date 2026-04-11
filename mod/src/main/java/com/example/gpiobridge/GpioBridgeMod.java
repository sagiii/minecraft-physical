package com.example.gpiobridge;

import com.example.gpiobridge.entity.CameraEntity;
import com.example.gpiobridge.network.MqttBridgeClient;
import com.example.gpiobridge.network.SetCameraIdPayload;
import com.example.gpiobridge.network.SetChannelPayload;
import com.example.gpiobridge.block.ChannelBlockEntity;
import com.example.gpiobridge.ModEntities;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;

public class GpioBridgeMod implements ModInitializer {

    @Override
    public void onInitialize() {
        // Registration order matters: blocks → block entities → screen handlers → entities
        ModBlocks.initialize();
        ModBlockEntityTypes.initialize();
        ModScreenHandlers.initialize();
        ModEntities.initialize();

        // Register the C2S packet for setting channel number from GUI
        PayloadTypeRegistry.serverboundPlay().register(SetChannelPayload.TYPE, SetChannelPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SetChannelPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerLevel world = (ServerLevel) context.player().level();
                if (world.getBlockEntity(payload.pos()) instanceof ChannelBlockEntity be) {
                    be.setChannel(payload.channel());
                }
            });
        });

        // Register the C2S packet for changing camera ID from GUI
        PayloadTypeRegistry.serverboundPlay().register(SetCameraIdPayload.TYPE, SetCameraIdPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SetCameraIdPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerLevel world = (ServerLevel) context.player().level();
                Entity entity = world.getEntity(payload.entityId());
                if (entity instanceof CameraEntity cam) {
                    cam.applyNewCamId(payload.newCamId());
                }
            });
        });

        // Right-click a CameraEntity to open the camera ID screen
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClientSide() && hand == InteractionHand.MAIN_HAND
                    && entity instanceof CameraEntity cam) {
                cam.openIdScreen((ServerPlayer) player);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });

        // Start / stop MQTT client with the server lifecycle
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                MqttBridgeClient.INSTANCE.start(server));
        ServerLifecycleEvents.SERVER_STOPPING.register(server ->
                MqttBridgeClient.INSTANCE.stop());
    }
}
