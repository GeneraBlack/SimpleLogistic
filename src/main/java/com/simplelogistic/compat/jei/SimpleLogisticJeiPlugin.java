package com.simplelogistic.compat.jei;

import com.simplelogistic.PipeScreen;
import com.simplelogistic.SetFilterSlotPayload;
import com.simplelogistic.SimpleLogistic;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class SimpleLogisticJeiPlugin implements IModPlugin {

    public static final Identifier PLUGIN_ID = Identifier.fromNamespaceAndPath(SimpleLogistic.MODID, "jei_plugin");

    @Override
    public Identifier getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(PipeScreen.class, new PipeGhostIngredientHandler());
    }

    public static class PipeGhostIngredientHandler implements IGhostIngredientHandler<PipeScreen> {
        @Override
        public <I> List<Target<I>> getTargetsTyped(PipeScreen gui, ITypedIngredient<I> ingredient, boolean doStart) {
            List<Target<I>> targets = new ArrayList<>();

            // 3x3 Filter Slots im rechten Panel der GUI
            int startX = gui.getGuiLeft() + 188;
            int startY = gui.getGuiTop() + 44;

            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    int slotIndex = row * 3 + col;
                    Rect2i area = new Rect2i(startX + col * 18, startY + row * 18, 16, 16);

                    targets.add(new Target<I>() {
                        @Override
                        public Rect2i getArea() {
                            return area;
                        }

                        @Override
                        public void accept(I ing) {
                            if (ing instanceof ItemStack stack) {
                                ClientPacketDistributor.sendToServer(new SetFilterSlotPayload(
                                        gui.getMenu().getPipePos(),
                                        gui.getMenu().getSide(),
                                        gui.getSelectedOpIndex(),
                                        slotIndex,
                                        stack
                                ));
                            }
                        }
                    });
                }
            }
            return targets;
        }

        @Override
        public void onComplete() {
        }
    }
}
