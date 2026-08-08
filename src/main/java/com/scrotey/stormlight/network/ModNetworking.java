package com.scrotey.stormlight.network;

import com.scrotey.stormlight.breathing.StormlightManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ModNetworking {
    private ModNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(
                AbilityInputPayload.TYPE,
                AbilityInputPayload.CODEC
        );

        PayloadTypeRegistry.clientboundPlay().register(
                StormlightStatusPayload.TYPE,
                StormlightStatusPayload.CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(
                AbilityInputPayload.TYPE,
                (payload, context) ->
                        StormlightManager.handleInput(
                                context.player(),
                                payload.ability(),
                                payload.action()
                        )
        );
    }
}
