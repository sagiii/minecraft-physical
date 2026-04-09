package com.example.gpiobridge;

import com.example.gpiobridge.block.ChannelBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntityTypes {

    public static BlockEntityType<ChannelBlockEntity> CHANNEL_BLOCK_ENTITY;

    public static void initialize() {
        CHANNEL_BLOCK_ENTITY = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath("gpio_bridge", "channel_block_entity"),
                FabricBlockEntityTypeBuilder.create(ChannelBlockEntity::new,
                        ModBlocks.CHANNEL_IN,
                        ModBlocks.CHANNEL_OUT
                ).build()
        );
    }
}
