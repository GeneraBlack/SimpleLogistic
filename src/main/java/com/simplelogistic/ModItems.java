package com.simplelogistic;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SimpleLogistic.MODID);

    public static final DeferredItem<BlockItem> ITEM_PIPE = ITEMS.registerSimpleBlockItem(ModBlocks.ITEM_PIPE);
    public static final DeferredItem<BlockItem> FLUID_PIPE = ITEMS.registerSimpleBlockItem(ModBlocks.FLUID_PIPE);
    public static final DeferredItem<BlockItem> ENERGY_PIPE = ITEMS.registerSimpleBlockItem(ModBlocks.ENERGY_PIPE);
    public static final DeferredItem<BlockItem> UNIVERSAL_PIPE = ITEMS.registerSimpleBlockItem(ModBlocks.UNIVERSAL_PIPE);
    public static final DeferredItem<BlockItem> DIMENSIONAL_NODE = ITEMS.registerSimpleBlockItem(ModBlocks.DIMENSIONAL_NODE);
    public static final DeferredItem<PipeWrenchItem> WRENCH = ITEMS.registerItem("wrench", PipeWrenchItem::new, p -> p.stacksTo(1));
}
