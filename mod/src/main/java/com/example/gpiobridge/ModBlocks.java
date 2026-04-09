package com.example.gpiobridge;

import com.example.gpiobridge.block.ChannelInBlock;
import com.example.gpiobridge.block.ChannelOutBlock;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.function.Function;

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

    private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("gpio_bridge", name);
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
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(entries -> {
            entries.add(CHANNEL_IN);
            entries.add(CHANNEL_OUT);
        });
    }
}
