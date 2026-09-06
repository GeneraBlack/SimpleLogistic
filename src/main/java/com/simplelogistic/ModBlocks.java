package com.simplelogistic;

import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SimpleLogistic.MODID);

    public static final DeferredBlock<PipeBlock> ITEM_PIPE = BLOCKS.registerBlock("item_pipe", props -> new PipeBlock(props, PipeTier.ITEM));

    public static final DeferredBlock<PipeBlock> FLUID_PIPE = BLOCKS.registerBlock("fluid_pipe", props -> new PipeBlock(props, PipeTier.FLUID));

    public static final DeferredBlock<PipeBlock> ENERGY_PIPE = BLOCKS.registerBlock("energy_pipe", props -> new PipeBlock(props, PipeTier.ENERGY));

    public static final DeferredBlock<PipeBlock> UNIVERSAL_PIPE = BLOCKS.registerBlock("universal_pipe", props -> new PipeBlock(props, PipeTier.UNIVERSAL));

    public static final DeferredBlock<DimensionalNodeBlock> DIMENSIONAL_NODE = BLOCKS.registerBlock("dimensional_node", DimensionalNodeBlock::new);
}
