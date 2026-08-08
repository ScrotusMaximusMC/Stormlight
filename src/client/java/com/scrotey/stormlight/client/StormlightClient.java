package com.scrotey.stormlight.client;

import com.mojang.blaze3d.platform.InputConstants;

import com.scrotey.stormlight.Stormlight;
import com.scrotey.stormlight.breathing.AbilityId;
import com.scrotey.stormlight.network.AbilityInputPayload;
import com.scrotey.stormlight.network.StormlightStatusPayload;
import com.scrotey.stormlight.screen.ModMenuTypes;
import com.scrotey.stormlight.screen.SphereJarScreen;
import com.scrotey.stormlight.particle.ModParticles;
import com.scrotey.stormlight.attachment.ModAttachments;
import com.scrotey.stormlight.client.mixin.AbstractContainerScreenAccessor;
import com.scrotey.stormlight.network.OpenSpherePouchPayload;
import com.scrotey.stormlight.network.SpherePouchSlotPayload;
import com.scrotey.stormlight.screen.SpherePouchScreen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.particle.EndRodParticle;
import net.minecraft.client.particle.FireflyParticle;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;

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

        MenuScreens.register(
                ModMenuTypes.SPHERE_POUCH,
                SpherePouchScreen::new
        );

        registerSpherePouchInventoryControls();

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

    private static void registerSpherePouchInventoryControls() {
        ScreenEvents.AFTER_INIT.register(
                (
                        client,
                        screen,
                        scaledWidth,
                        scaledHeight
                ) -> {
                    if (!(screen
                            instanceof InventoryScreen)) {
                        return;
                    }

                    AbstractContainerScreenAccessor accessor =
                            (AbstractContainerScreenAccessor)
                                    screen;

                    Button equipmentSlot =
                            Button.builder(
                                    Component.literal("+"),
                                    button ->
                                            ClientPlayNetworking.send(
                                                    SpherePouchSlotPayload
                                                            .INSTANCE
                                            )
                            ).bounds(
                                    accessor.stormlight$getLeftPos()
                                            + 77,
                                    accessor.stormlight$getTopPos()
                                            + 43,
                                    18,
                                    18
                            ).build();

                    Button pouchTab =
                            Button.builder(
                                    Component.translatable(
                                            "button.stormlight."
                                                    + "sphere_pouch"
                                    ),
                                    button ->
                                            ClientPlayNetworking.send(
                                                    OpenSpherePouchPayload
                                                            .INSTANCE
                                            )
                            ).bounds(
                                    accessor.stormlight$getLeftPos()
                                            + 128,
                                    accessor.stormlight$getTopPos()
                                            + 61,
                                    44,
                                    18
                            ).build();

                    Screens.getWidgets(screen).add(
                            equipmentSlot
                    );

                    Screens.getWidgets(screen).add(
                            pouchTab
                    );

                    ScreenEvents.beforeExtract(screen)
                            .register(
                                    (
                                            ignoredScreen,
                                            ignoredGraphics,
                                            ignoredMouseX,
                                            ignoredMouseY,
                                            ignoredDelta
                                    ) -> {
                                        int left =
                                                accessor
                                                        .stormlight$getLeftPos();

                                        int top =
                                                accessor
                                                        .stormlight$getTopPos();

                                        equipmentSlot.setPosition(
                                                left + 77,
                                                top + 43
                                        );

                                        pouchTab.setPosition(
                                                left + 128,
                                                top + 61
                                        );

                                        pouchTab.active =
                                                client.player != null
                                                        && ModAttachments
                                                        .hasEquippedPouch(
                                                                client.player
                                                        );
                                    }
                            );

                    ScreenEvents.afterExtract(screen)
                            .register(
                                    (
                                            ignoredScreen,
                                            graphics,
                                            mouseX,
                                            mouseY,
                                            delta
                                    ) -> {
                                        if (client.player == null) {
                                            return;
                                        }

                                        ItemStack pouch =
                                                ModAttachments
                                                        .getEquippedPouch(
                                                                client.player
                                                        );

                                        if (!pouch.isEmpty()) {
                                            graphics.item(
                                                    pouch,
                                                    equipmentSlot.getX()
                                                            + 1,
                                                    equipmentSlot.getY()
                                                            + 1
                                            );
                                        }
                                    }
                            );
                }
        );
    }

    private static void send(AbilityId ability, AbilityInputPayload.Action action) {
        ClientPlayNetworking.send(new AbilityInputPayload(ability, action));
    }
}
