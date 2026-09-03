package com.simplelogistic;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = SimpleLogistic.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModPayloads {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(SimpleLogistic.MODID);
        
        registrar.playToServer(
            ChangePipeModePayload.TYPE,
            ChangePipeModePayload.STREAM_CODEC,
            ChangePipeModePayload::handle
        );

        registrar.playToServer(
            UpdateOperationPayload.TYPE,
            UpdateOperationPayload.STREAM_CODEC,
            UpdateOperationPayload::handle
        );

        registrar.playToServer(
            ManageOperationPayload.TYPE,
            ManageOperationPayload.STREAM_CODEC,
            ManageOperationPayload::handle
        );

        registrar.playToServer(
            SetFilterSlotPayload.TYPE,
            SetFilterSlotPayload.STREAM_CODEC,
            SetFilterSlotPayload::handle
        );
    }
}
