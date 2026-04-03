package com.example.gpiobridge;

import com.example.gpiobridge.block.ChannelBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntityTypes {

    public static BlockEntityType<ChannelBlockEntity> CHANNEL_BLOCK_ENTITY;

    public static void initialize() {
        CHANNEL_BLOCK_ENTITY = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                Identifier.of("gpio_bridge", "channel_block_entity"),
                FabricBlockEntityTypeBuilder.create(ChannelBlockEntity::new,
                        ModBlocks.CHANNEL_IN,
                        ModBlocks.CHANNEL_OUT
                ).build()
        );
    }
}
