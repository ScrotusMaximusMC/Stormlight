package com.scrotey.stormlight.network;

import com.scrotey.stormlight.breathing.StormlightBreathingManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ModNetworking {
    private ModNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(
                SetBreathingPayload.TYPE,
                SetBreathingPayload.CODEC
        );

        PayloadTypeRegistry.clientboundPlay().register(
                StormlightStatusPayload.TYPE,
                StormlightStatusPayload.CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(
                SetBreathingPayload.TYPE,
                (payload, context) ->
                        StormlightBreathingManager.setBreathing(
                                context.player(),
                                payload.breathing()
                        )
        );
    }
}
