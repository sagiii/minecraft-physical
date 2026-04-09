package com.example.gpiobridge.network;

import com.example.gpiobridge.block.ChannelBlockEntity;
import com.example.gpiobridge.config.GpioBridgeConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton MQTT client that bridges Minecraft channel blocks to M5Stack devices.
 *
 * Topic convention (prefix: mp/bridge):
 *   mp/bridge/m/sig/{id}  — OUT block → device   (we publish)
 *   mp/bridge/p/sig/{id}  — device → IN block    (we subscribe)
 *
 *   m = Minecraft world, p = Physical world
 * IN and OUT channels are independent — the same ID can be used for both.
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
            client.subscribe("mp/bridge/p/sig/+", 0);
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
        String topic   = "mp/bridge/m/sig/" + channel;
        String payload = value ? "1" : "0";
        try {
            client.publish(topic, new MqttMessage(payload.getBytes()));
        } catch (MqttException e) {
            System.err.println("[GPIO Bridge] Publish failed: " + e.getMessage());
        }
    }

    // ----- incoming messages -----

    private void onMessage(String topic, String payload) {
        // Expected topic: mp/bridge/p/sig/{id}
        String[] parts = topic.split("/");
        if (parts.length != 5) return;
        try {
            int channel = Integer.parseInt(parts[4]);
            boolean value = "1".equals(payload) || "true".equalsIgnoreCase(payload.trim());
            dispatchToInBlock(channel, value);
        } catch (NumberFormatException ignored) {}
    }

    private void dispatchToInBlock(int channel, boolean value) {
        BlockPos pos = inChannels.get(channel);
        if (pos == null || server == null) return;
        // Minecraft world updates must happen on the server thread
        server.execute(() -> {
            ServerLevel world = server.overworld();
            if (world.getBlockEntity(pos) instanceof ChannelBlockEntity be) {
                be.updateFromMqtt(value, world, pos);
            }
        });
    }
}
