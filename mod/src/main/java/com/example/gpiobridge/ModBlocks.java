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
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class ModBlocks {

    public static final Block CHANNEL_IN = register("channel_in",
            new ChannelInBlock(AbstractBlock.Settings.create()
                    .strength(1.5f)
                    .sounds(BlockSoundGroup.STONE)
                    .mapColor(MapColor.LIGHT_BLUE)
                    .luminance(state -> state.get(ChannelInBlock.POWERED) ? 15 : 0)
                    .nonOpaque()));

    public static final Block CHANNEL_OUT = register("channel_out",
            new ChannelOutBlock(AbstractBlock.Settings.create()
                    .strength(1.5f)
                    .sounds(BlockSoundGroup.STONE)
                    .mapColor(MapColor.ORANGE)
                    .luminance(state -> state.get(ChannelOutBlock.POWERED) ? 15 : 0)
                    .nonOpaque()));

    private static Block register(String name, Block block) {
        Identifier id = Identifier.of("gpio_bridge", name);
        Registry.register(Registries.BLOCK, id, block);
        Registry.register(Registries.ITEM,  id, new BlockItem(block, new Item.Settings()));
        return block;
    }

    public static void initialize() {
        // Add to the Redstone Blocks tab for discoverability
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register(entries -> {
            entries.add(CHANNEL_IN);
            entries.add(CHANNEL_OUT);
        });
    }
}
