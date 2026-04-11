package com.example.gpiobridge;

import com.example.gpiobridge.camera.OffscreenCameraRenderer;
import com.example.gpiobridge.renderer.CameraEntityRenderer;
import com.example.gpiobridge.screen.CameraIdScreen;
import com.example.gpiobridge.screen.ChannelScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;

public class GpioBridgeModClient implements ClientModInitializer {

    @Override
    @SuppressWarnings("deprecation")
    public void onInitializeClient() {
        MenuScreens.register(ModScreenHandlers.CHANNEL_SCREEN, ChannelScreen::new);
        MenuScreens.register(ModScreenHandlers.CAMERA_ID_SCREEN, CameraIdScreen::new);

        // Register camera entity renderer (visible block-shaped entity)
        EntityRendererRegistry.register(ModEntities.CAMERA, CameraEntityRenderer::new);

        // Start the camera frame capture & publish pipeline
        new OffscreenCameraRenderer().initialize();
    }
}
