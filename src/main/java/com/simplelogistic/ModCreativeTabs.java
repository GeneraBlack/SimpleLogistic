package com.simplelogistic;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SimpleLogistic.MODID);

    public static final Supplier<CreativeModeTab> TAB = CREATIVE_MODE_TABS.register("simplelogistic_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.simplelogistic"))
            .icon(() -> new ItemStack(ModItems.UNIVERSAL_PIPE.get()))
            .displayItems((parameters, output) -> {
                output.accept(ModItems.ITEM_PIPE.get());
                output.accept(ModItems.FLUID_PIPE.get());
                output.accept(ModItems.ENERGY_PIPE.get());
                output.accept(ModItems.UNIVERSAL_PIPE.get());
                output.accept(ModItems.DIMENSIONAL_NODE.get());
                output.accept(ModItems.WRENCH.get());
            })
            .build());
}
