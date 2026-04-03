package com.example.gpiobridge;

import com.example.gpiobridge.screen.ChannelScreenData;
import com.example.gpiobridge.screen.ChannelScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ModScreenHandlers {

    public static ScreenHandlerType<ChannelScreenHandler> CHANNEL_SCREEN;

    public static void initialize() {
        CHANNEL_SCREEN = Registry.register(
                Registries.SCREEN_HANDLER,
                Identifier.of("gpio_bridge", "channel_screen"),
                new ExtendedScreenHandlerType<>(
                        ChannelScreenHandler::new,
                        ChannelScreenData.CODEC
                )
        );
    }
}
