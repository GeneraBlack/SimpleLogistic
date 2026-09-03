package com.simplelogistic;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleLogistic.MODID);

    public static final Supplier<BlockEntityType<PipeBlockEntity>> PIPE_BE = BLOCK_ENTITIES.register("pipe_be", 
        () -> BlockEntityType.Builder.of((pos, state) -> {
            PipeBlock block = (PipeBlock) state.getBlock();
            return new PipeBlockEntity(pos, state, block.getTier());
        }, ModBlocks.ITEM_PIPE.get(), ModBlocks.FLUID_PIPE.get(), ModBlocks.ENERGY_PIPE.get(), ModBlocks.UNIVERSAL_PIPE.get()).build(null));

    public static final Supplier<BlockEntityType<DimensionalNodeBlockEntity>> DIMENSIONAL_NODE_BE = BLOCK_ENTITIES.register("dimensional_node_be",
        () -> BlockEntityType.Builder.of(DimensionalNodeBlockEntity::new, ModBlocks.DIMENSIONAL_NODE.get()).build(null));
}
