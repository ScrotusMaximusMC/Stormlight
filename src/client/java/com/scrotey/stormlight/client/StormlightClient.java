package com.scrotey.stormlight.client;

import com.mojang.blaze3d.platform.InputConstants;

import com.scrotey.stormlight.Stormlight;
import com.scrotey.stormlight.breathing.AbilityId;
import com.scrotey.stormlight.network.AbilityInputPayload;
import com.scrotey.stormlight.network.StormlightStatusPayload;
import com.scrotey.stormlight.screen.ModMenuTypes;
import com.scrotey.stormlight.screen.SphereJarScreen;
import com.scrotey.stormlight.particle.ModParticles;

import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.particle.EndRodParticle;
import net.minecraft.client.particle.FireflyParticle;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;

public class StormlightClient implements ClientModInitializer {
    private static final int HOLD_THRESHOLD_TICKS = 8;

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

    private static int heldTicks = 0;
    private static boolean holdActivated = false;

    @Override
    public void onInitializeClient() {
        ParticleProviderRegistry.getInstance().register(
                ModParticles.WINDSPREN,
                EndRodParticle.Provider::new
        );

        ParticleProviderRegistry.getInstance().register(
                ModParticles.SURGE_LIGHT,
                FireflyParticle.FireflyProvider::new
        );

        MenuScreens.register(
                ModMenuTypes.SPHERE_JAR,
                SphereJarScreen::new
        );

        ClientPlayNetworking.registerGlobalReceiver(
                StormlightStatusPayload.TYPE,
                (payload, context) -> StormlightHud.update(payload)
        );

        ClientTickEvents.END_CLIENT_TICK.register(StormlightClient::tick);

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Stormlight.id("stormlight_bar"),
                StormlightHud::render
        );
    }

    private static void tick(net.minecraft.client.Minecraft client) {
        if (client.player == null) {
            heldTicks = 0;
            holdActivated = false;
            return;
        }

        boolean down = BREATHE_STORMLIGHT_KEY.isDown() && client.gui.screen() == null;

        if (down) {
            heldTicks++;

            if (!holdActivated && heldTicks == HOLD_THRESHOLD_TICKS) {
                holdActivated = true;
                send(AbilityId.EMERGENCY_HEAL, AbilityInputPayload.Action.START);
            }
        } else {
            if (heldTicks > 0) {
                if (holdActivated) {
                    send(AbilityId.EMERGENCY_HEAL, AbilityInputPayload.Action.STOP);
                } else if (heldTicks < HOLD_THRESHOLD_TICKS) {
                    send(AbilityId.STRENGTH_SURGE, AbilityInputPayload.Action.TOGGLE);
                }
            }

            heldTicks = 0;
            holdActivated = false;
        }

        while (BREATHE_STORMLIGHT_KEY.consumeClick()) {
            // Drain the click queue defensively; tap/hold logic above
            // uses isDown() polling, not consumeClick().
        }
    }

    private static void send(AbilityId ability, AbilityInputPayload.Action action) {
        ClientPlayNetworking.send(new AbilityInputPayload(ability, action));
    }
}
