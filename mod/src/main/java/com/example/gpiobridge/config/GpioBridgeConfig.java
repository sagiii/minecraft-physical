package com.example.gpiobridge.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;

public class GpioBridgeConfig {
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("mp_bridge.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Data instance;

    public static Data get() {
        if (instance == null) load();
        return instance;
    }

    private static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader r = Files.newBufferedReader(CONFIG_PATH)) {
                instance = GSON.fromJson(r, Data.class);
                if (instance == null) instance = new Data();
            } catch (Exception e) {
                instance = new Data();
            }
        } else {
            instance = new Data();
            save();
        }
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(instance, w);
        } catch (IOException e) {
            System.err.println("[GPIO Bridge] Failed to save config: " + e.getMessage());
        }
    }

    public static class Data {
        public String brokerHost      = "localhost";
        public int    brokerPort      = 1883;
        public String clientId        = "minecraft-mod";
        public int    reconnectDelay  = 5000;
    }
}
