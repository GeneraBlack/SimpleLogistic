package com.simplelogistic;

import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SimpleLogistic.MODID);

    public static final DeferredBlock<Block> ITEM_PIPE = BLOCKS.register("item_pipe", () -> new PipeBlock(PipeTier.ITEM));
    public static final DeferredBlock<Block> FLUID_PIPE = BLOCKS.register("fluid_pipe", () -> new PipeBlock(PipeTier.FLUID));
    public static final DeferredBlock<Block> ENERGY_PIPE = BLOCKS.register("energy_pipe", () -> new PipeBlock(PipeTier.ENERGY));
    public static final DeferredBlock<Block> UNIVERSAL_PIPE = BLOCKS.register("universal_pipe", () -> new PipeBlock(PipeTier.UNIVERSAL));
    public static final DeferredBlock<Block> DIMENSIONAL_NODE = BLOCKS.register("dimensional_node", DimensionalNodeBlock::new);
}
