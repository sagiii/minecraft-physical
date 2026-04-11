package com.example.gpiobridge;

import com.example.gpiobridge.screen.CameraIdScreenData;
import com.example.gpiobridge.screen.CameraIdScreenHandler;
import com.example.gpiobridge.screen.ChannelScreenData;
import com.example.gpiobridge.screen.ChannelScreenHandler;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;

public class ModScreenHandlers {

    public static MenuType<ChannelScreenHandler> CHANNEL_SCREEN;
    public static MenuType<CameraIdScreenHandler> CAMERA_ID_SCREEN;

    public static void initialize() {
        CHANNEL_SCREEN = Registry.register(
                BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath("mp_bridge", "channel_screen"),
                new ExtendedMenuType<>(
                        ChannelScreenHandler::new,
                        ChannelScreenData.CODEC
                )
        );
        CAMERA_ID_SCREEN = Registry.register(
                BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath("mp_bridge", "camera_id_screen"),
                new ExtendedMenuType<>(
                        CameraIdScreenHandler::new,
                        CameraIdScreenData.CODEC
                )
        );
    }
}
