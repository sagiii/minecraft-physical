package com.example.gpiobridge;

import com.example.gpiobridge.block.ChannelInBlock;
import com.example.gpiobridge.block.ChannelOutBlock;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.function.Function;
import java.util.function.UnaryOperator;

public class ModBlocks {

    public static final Block CHANNEL_IN = register("channel_in",
            settings -> new ChannelInBlock(settings
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .lightLevel(state -> state.getValue(ChannelInBlock.POWERED) ? 15 : 0)
                    .noOcclusion()));

    public static final Block CHANNEL_OUT = register("channel_out",
            settings -> new ChannelOutBlock(settings
                    .mapColor(MapColor.COLOR_ORANGE)
                    .lightLevel(state -> state.getValue(ChannelOutBlock.POWERED) ? 15 : 0)
                    .noOcclusion()));

    /**
     * Display-only block used by CameraEntityRenderer.
     * Not craftable, not placed in the world as a real block.
     */
    public static final Block CAMERA_DISPLAY = registerDisplayOnly("camera",
            settings -> new Block(settings
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(-1f, 3600000f) // indestructible
                    .noOcclusion()));

    /** Register a block without a BlockItem (not obtainable, display-only model use). */
    private static Block registerDisplayOnly(String name, Function<BlockBehaviour.Properties, Block> factory) {
        Identifier id = Identifier.fromNamespaceAndPath("mp_bridge", name);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        BlockBehaviour.Properties base = BlockBehaviour.Properties.of().setId(blockKey);
        Block block = factory.apply(base);
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
        return block;
    }

    private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory) {
        Identifier id = Identifier.fromNamespaceAndPath("mp_bridge", name);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        ResourceKey<Item>  itemKey  = ResourceKey.create(Registries.ITEM,  id);

        BlockBehaviour.Properties base = BlockBehaviour.Properties.of()
                .strength(1.5f)
                .sound(SoundType.STONE)
                .setId(blockKey);

        Block block = factory.apply(base);
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
        Registry.register(BuiltInRegistries.ITEM,  itemKey,
                new BlockItem(block, new Item.Properties().setId(itemKey)));
        return block;
    }

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(entries -> {
            entries.accept(CHANNEL_IN);
            entries.accept(CHANNEL_OUT);
        });
    }
}
