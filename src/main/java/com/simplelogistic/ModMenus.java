package com.simplelogistic;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import java.util.function.Supplier;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(BuiltInRegistries.MENU, SimpleLogistic.MODID);

    public static final Supplier<MenuType<PipeMenu>> PIPE_MENU = MENUS.register("pipe_menu", 
        () -> IMenuTypeExtension.create(PipeMenu::new));
}
