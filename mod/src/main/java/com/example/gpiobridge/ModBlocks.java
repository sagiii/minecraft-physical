package com.example.gpiobridge;

import com.example.gpiobridge.block.ChannelInBlock;
import com.example.gpiobridge.block.ChannelOutBlock;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class ModBlocks {

    // Minecraft 1.21.4: Block.Settings に registryKey() を事前にセットする必要がある
    public static final Block CHANNEL_IN = register("channel_in",
            settings -> new ChannelInBlock(settings
                    .mapColor(MapColor.LIGHT_BLUE)
                    .luminance(state -> state.get(ChannelInBlock.POWERED) ? 15 : 0)
                    .nonOpaque()));

    public static final Block CHANNEL_OUT = register("channel_out",
            settings -> new ChannelOutBlock(settings
                    .mapColor(MapColor.ORANGE)
                    .luminance(state -> state.get(ChannelOutBlock.POWERED) ? 15 : 0)
                    .nonOpaque()));

    private static Block register(String name, Function<AbstractBlock.Settings, Block> factory) {
        Identifier id = Identifier.of("gpio_bridge", name);
        RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, id);
        RegistryKey<Item>  itemKey  = RegistryKey.of(RegistryKeys.ITEM,  id);

        // registryKey() をセットしてからブロックを生成する
        AbstractBlock.Settings base = AbstractBlock.Settings.create()
                .strength(1.5f)
                .sounds(BlockSoundGroup.STONE)
                .registryKey(blockKey);

        Block block = factory.apply(base);
        Registry.register(Registries.BLOCK, blockKey, block);
        Registry.register(Registries.ITEM,  itemKey,
                new BlockItem(block, new Item.Settings().registryKey(itemKey)));
        return block;
    }

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register(entries -> {
            entries.add(CHANNEL_IN);
            entries.add(CHANNEL_OUT);
        });
    }
}
