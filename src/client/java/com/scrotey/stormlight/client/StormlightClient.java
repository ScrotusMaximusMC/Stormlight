package com.scrotey.stormlight.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.scrotey.stormlight.Stormlight;
import com.scrotey.stormlight.network.SetBreathingPayload;
import com.scrotey.stormlight.network.StormlightStatusPayload;
import com.scrotey.stormlight.screen.ModMenuTypes;
import com.scrotey.stormlight.screen.SphereJarScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;

public class StormlightClient implements ClientModInitializer {
    private static final KeyMapping.Category STORMLIGHT_CATEGORY =
            KeyMapping.Category.register(
                    Stormlight.id("controls")
            );

    private static final KeyMapping BREATHE_STORMLIGHT_KEY =
            KeyMappingHelper.registerKeyMapping(
                    new KeyMapping(
                            "key.stormlight.breathe",
                            InputConstants.Type.KEYSYM,
                            InputConstants.KEY_B,
                            STORMLIGHT_CATEGORY
                    )
            );

    @Override
    public void onInitializeClient() {
        MenuScreens.register(
                ModMenuTypes.SPHERE_JAR,
                SphereJarScreen::new
        );

        ClientPlayNetworking.registerGlobalReceiver(
                StormlightStatusPayload.TYPE,
                (payload, context) -> StormlightHud.update(payload)
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (BREATHE_STORMLIGHT_KEY.consumeClick()) {
                if (client.player != null) {
                    ClientPlayNetworking.send(
                            new SetBreathingPayload(
                                    !StormlightHud.isBreathing()
                            )
                    );
                }
            }
        });

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Stormlight.id("stormlight_bar"),
                StormlightHud::render
        );
    }
}
