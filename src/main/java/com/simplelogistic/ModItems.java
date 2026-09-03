package com.simplelogistic;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SimpleLogistic.MODID);

    public static final DeferredItem<Item> ITEM_PIPE = ITEMS.register("item_pipe", () -> new BlockItem(ModBlocks.ITEM_PIPE.get(), new Item.Properties()));
    public static final DeferredItem<Item> FLUID_PIPE = ITEMS.register("fluid_pipe", () -> new BlockItem(ModBlocks.FLUID_PIPE.get(), new Item.Properties()));
    public static final DeferredItem<Item> ENERGY_PIPE = ITEMS.register("energy_pipe", () -> new BlockItem(ModBlocks.ENERGY_PIPE.get(), new Item.Properties()));
    public static final DeferredItem<Item> UNIVERSAL_PIPE = ITEMS.register("universal_pipe", () -> new BlockItem(ModBlocks.UNIVERSAL_PIPE.get(), new Item.Properties()));
    public static final DeferredItem<Item> DIMENSIONAL_NODE = ITEMS.register("dimensional_node", () -> new BlockItem(ModBlocks.DIMENSIONAL_NODE.get(), new Item.Properties()));
    public static final DeferredItem<Item> WRENCH = ITEMS.register("wrench", () -> new PipeWrenchItem(new Item.Properties().stacksTo(1)));
}
