package com.example.gpiobridge;

import com.example.gpiobridge.screen.ChannelScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class GpioBridgeModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MenuScreens.register(ModScreenHandlers.CHANNEL_SCREEN, ChannelScreen::new);
    }
}
