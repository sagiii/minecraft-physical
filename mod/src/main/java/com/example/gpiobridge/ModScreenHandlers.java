package com.example.gpiobridge;

import com.example.gpiobridge.screen.ChannelScreenData;
import com.example.gpiobridge.screen.ChannelScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;

public class ModScreenHandlers {

    public static MenuType<ChannelScreenHandler> CHANNEL_SCREEN;

    public static void initialize() {
        CHANNEL_SCREEN = Registry.register(
                BuiltInRegistries.MENU,
                ResourceLocation.fromNamespaceAndPath("gpio_bridge", "channel_screen"),
                new ExtendedScreenHandlerType<>(
                        ChannelScreenHandler::new,
                        ChannelScreenData.CODEC
                )
        );
    }
}
