package com.example.gpiobridge.network;

import com.example.gpiobridge.block.ChannelBlockEntity;
import com.example.gpiobridge.config.GpioBridgeConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton MQTT client that bridges Minecraft channel blocks to M5Stack devices.
 *
 * Topic convention:
 *   minecraft/ch/{n}/state  — OUT block → M5Stack  (we publish)
 *   minecraft/ch/{n}/input  — M5Stack → IN block   (we subscribe)
 *
 * IN and OUT channels are independent — the same channel number can be used for both.
 */
public class MqttBridgeClient {
    public static final MqttBridgeClient INSTANCE = new MqttBridgeClient();

    private MqttClient client;
    // channel → block position in the overworld
    private final Map<Integer, BlockPos> inChannels  = new ConcurrentHashMap<>();
    private final Map<Integer, BlockPos> outChannels = new ConcurrentHashMap<>();
    private volatile MinecraftServer server;

    private MqttBridgeClient() {}

    // ----- lifecycle -----

    public void start(MinecraftServer server) {
        this.server = server;
        connect();
    }

    public void stop() {
        server = null;
        inChannels.clear();
        outChannels.clear();
        try {
            if (client != null && client.isConnected()) client.disconnect();
        } catch (MqttException ignored) {}
    }

    private void connect() {
        GpioBridgeConfig.Data cfg = GpioBridgeConfig.get();
        String uri = "tcp://" + cfg.brokerHost + ":" + cfg.brokerPort;
        try {
            client = new MqttClient(uri, cfg.clientId, new MemoryPersistence());
            MqttConnectOptions opts = new MqttConnectOptions();
            opts.setAutomaticReconnect(true);
            opts.setCleanSession(true);
            opts.setConnectionTimeout(10);

            client.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    System.out.println("[GPIO Bridge] Connected to MQTT broker at " + serverURI);
                    subscribeToInputs();
                }
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    onMessage(topic, new String(message.getPayload()));
                }
                @Override
                public void connectionLost(Throwable cause) {
                    System.err.println("[GPIO Bridge] MQTT disconnected: " + cause.getMessage());
                }
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });

            client.connect(opts);
        } catch (MqttException e) {
            System.err.println("[GPIO Bridge] MQTT connect failed: " + e.getMessage());
        }
    }

    private void subscribeToInputs() {
        try {
            // Wildcard subscription — handles all channels at once
            client.subscribe("minecraft/ch/+/input", 0);
        } catch (MqttException e) {
            System.err.println("[GPIO Bridge] Subscribe failed: " + e.getMessage());
        }
    }

    // ----- channel registration -----

    public void registerIn(int channel, BlockPos pos) {
        inChannels.put(channel, pos);
    }

    public void registerOut(int channel, BlockPos pos) {
        outChannels.put(channel, pos);
    }

    public void unregister(int channel, BlockPos pos) {
        inChannels.remove(channel, pos);
        outChannels.remove(channel, pos);
    }

    // ----- publishing -----

    public void publish(int channel, boolean value) {
        if (client == null || !client.isConnected()) return;
        String topic   = "minecraft/ch/" + channel + "/state";
        String payload = value ? "1" : "0";
        try {
            client.publish(topic, new MqttMessage(payload.getBytes()));
        } catch (MqttException e) {
            System.err.println("[GPIO Bridge] Publish failed: " + e.getMessage());
        }
    }

    // ----- incoming messages -----

    private void onMessage(String topic, String payload) {
        // Expected topic: minecraft/ch/{n}/input
        String[] parts = topic.split("/");
        if (parts.length != 4) return;
        try {
            int channel = Integer.parseInt(parts[2]);
            boolean value = "1".equals(payload) || "true".equalsIgnoreCase(payload.trim());
            dispatchToInBlock(channel, value);
        } catch (NumberFormatException ignored) {}
    }

    private void dispatchToInBlock(int channel, boolean value) {
        BlockPos pos = inChannels.get(channel);
        if (pos == null || server == null) return;
        // Minecraft world updates must happen on the server thread
        server.execute(() -> {
            ServerWorld world = server.getOverworld();
            if (world.getBlockEntity(pos) instanceof ChannelBlockEntity be) {
                be.updateFromMqtt(value, world, pos);
            }
        });
    }
}
