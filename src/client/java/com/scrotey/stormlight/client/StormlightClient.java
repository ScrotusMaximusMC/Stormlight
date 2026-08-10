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
import com.scrotey.stormlight.network.SpherePouchSlotPayload;
import com.scrotey.stormlight.screen.SpherePouchInventoryLayout;
import com.scrotey.stormlight.client.highstorm.ApproachingStormfrontEffects;
import com.scrotey.stormlight.client.highstorm.ClientHighstormState;
import com.scrotey.stormlight.client.highstorm.ClientHighstormWind;
import com.scrotey.stormlight.client.highstorm.HighstormLeafParticle;
import com.scrotey.stormlight.network.HighstormVisualPayload;
import com.scrotey.stormlight.client.highstorm.StormfrontCloudRenderer;

import java.util.function.Supplier;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
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
    private static final int SLOT_DARK_EDGE = 0xFF06131F;
    private static final int SLOT_BACKGROUND = 0xFF102B3D;
    private static final int SLOT_LIGHT_EDGE = 0xFF6DE8FF;
    private static final int SLOT_HOVER = 0x506DE8FF;

    private static final int EMPTY_POUCH_OUTLINE = 0xFF78E8FF;

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

        ParticleProviderRegistry.getInstance().register(
                ModParticles.HIGHSTORM_LEAF,
                HighstormLeafParticle.Provider::new
        );

        MenuScreens.register(
                ModMenuTypes.SPHERE_JAR,
                SphereJarScreen::new
        );

        registerSpherePouchInventoryControls();

        ClientPlayNetworking.registerGlobalReceiver(
                StormlightStatusPayload.TYPE,
                (payload, context) -> StormlightHud.update(payload)
        );

        ClientPlayNetworking.registerGlobalReceiver(
                HighstormVisualPayload.TYPE,
                (payload, context) ->
                        ClientHighstormState.update(payload)
        );

        ClientTickEvents.END_CLIENT_TICK.register(StormlightClient::tick);

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Stormlight.id("stormlight_bar"),
                StormlightHud::render
        );

        StormfrontCloudRenderer.register();
    }

    private static void tick(net.minecraft.client.Minecraft client) {
        ClientHighstormState.tick(client);
        ApproachingStormfrontEffects.tick(client);
        ClientHighstormWind.tick(client);

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

                    SpherePouchSlotWidget equipmentSlot =
                            new SpherePouchSlotWidget(
                                    accessor.stormlight$getLeftPos()
                                            + 76,
                                    accessor.stormlight$getTopPos()
                                            + 43,
                                    () ->
                                            ClientPlayNetworking.send(
                                                    SpherePouchSlotPayload.INSTANCE
                                            ),
                                    () -> {
                                        if (client.player == null) {
                                            return ItemStack.EMPTY;
                                        }

                                        return ModAttachments.getEquippedPouch(
                                                client.player
                                        );
                                    }
                            );

                    Screens.getWidgets(screen).add(
                            equipmentSlot
                    );

                    ScreenEvents.beforeExtract(screen)
                            .register(
                                    (
                                            ignoredScreen,
                                            graphics,
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
                                                left + 76,
                                                top + 43
                                        );
                                        
                                    }
                            );
                }
        );
    }

    private static final class SpherePouchSlotWidget
            extends AbstractWidget {

        private final Runnable onPress;
        private final Supplier<ItemStack> pouchSupplier;

        private SpherePouchSlotWidget(
                int x,
                int y,
                Runnable onPress,
                Supplier<ItemStack> pouchSupplier
        ) {
            super(
                    x,
                    y,
                    18,
                    18,
                    Component.translatable(
                            "item.stormlight.sphere_pouch"
                    )
            );

            this.onPress = onPress;
            this.pouchSupplier = pouchSupplier;
        }

        @Override
        protected void extractWidgetRenderState(
                GuiGraphicsExtractor graphics,
                int mouseX,
                int mouseY,
                float delta
        ) {
            int x = getX();
            int y = getY();

            // Recessed slot background.
            graphics.fill(
                    x,
                    y,
                    x + 18,
                    y + 18,
                    SLOT_BACKGROUND
            );

            // Dark top and left edges.
            graphics.fill(
                    x,
                    y,
                    x + 18,
                    y + 1,
                    SLOT_DARK_EDGE
            );

            graphics.fill(
                    x,
                    y,
                    x + 1,
                    y + 18,
                    SLOT_DARK_EDGE
            );

            // Light bottom and right edges.
            graphics.fill(
                    x + 17,
                    y + 1,
                    x + 18,
                    y + 18,
                    SLOT_LIGHT_EDGE
            );

            graphics.fill(
                    x + 1,
                    y + 17,
                    x + 17,
                    y + 18,
                    SLOT_LIGHT_EDGE
            );

            ItemStack pouch = pouchSupplier.get();

            if (pouch.isEmpty()) {
                drawEmptyPouchOutline(
                        graphics,
                        x + 1,
                        y + 1
                );
            } else {
                graphics.item(
                        pouch,
                        x + 1,
                        y + 1
                );
            }

            if (isHovered()) {
                graphics.fill(
                        x + 1,
                        y + 1,
                        x + 17,
                        y + 17,
                        SLOT_HOVER
                );
            }
        }

        private void drawEmptyPouchOutline(
                GuiGraphicsExtractor graphics,
                int x,
                int y
        ) {
            // Tied neck of the pouch.
            graphics.fill(
                    x + 6,
                    y + 2,
                    x + 10,
                    y + 3,
                    EMPTY_POUCH_OUTLINE
            );

            graphics.fill(
                    x + 5,
                    y + 3,
                    x + 6,
                    y + 5,
                    EMPTY_POUCH_OUTLINE
            );

            graphics.fill(
                    x + 10,
                    y + 3,
                    x + 11,
                    y + 5,
                    EMPTY_POUCH_OUTLINE
            );

            // Top of the pouch.
            graphics.fill(
                    x + 5,
                    y + 5,
                    x + 11,
                    y + 6,
                    EMPTY_POUCH_OUTLINE
            );

            // Upper sides.
            graphics.fill(
                    x + 4,
                    y + 6,
                    x + 5,
                    y + 8,
                    EMPTY_POUCH_OUTLINE
            );

            graphics.fill(
                    x + 11,
                    y + 6,
                    x + 12,
                    y + 8,
                    EMPTY_POUCH_OUTLINE
            );

            // Wide middle.
            graphics.fill(
                    x + 3,
                    y + 8,
                    x + 4,
                    y + 12,
                    EMPTY_POUCH_OUTLINE
            );

            graphics.fill(
                    x + 12,
                    y + 8,
                    x + 13,
                    y + 12,
                    EMPTY_POUCH_OUTLINE
            );

            // Lower sides.
            graphics.fill(
                    x + 4,
                    y + 12,
                    x + 5,
                    y + 14,
                    EMPTY_POUCH_OUTLINE
            );

            graphics.fill(
                    x + 11,
                    y + 12,
                    x + 12,
                    y + 14,
                    EMPTY_POUCH_OUTLINE
            );

            // Bottom.
            graphics.fill(
                    x + 5,
                    y + 14,
                    x + 11,
                    y + 15,
                    EMPTY_POUCH_OUTLINE
            );
        }

        @Override
        public void onClick(
                MouseButtonEvent event,
                boolean doubleClick
        ) {
            onPress.run();
        }

        @Override
        protected void updateWidgetNarration(
                NarrationElementOutput output
        ) {
            defaultButtonNarrationText(output);
        }
    }

    private static void drawPouchInventoryPanel(
            GuiGraphicsExtractor graphics,
            net.minecraft.client.Minecraft client,
            int left,
            int top
    ) {
        int panelLeft = left
                + SpherePouchInventoryLayout.PANEL_X;

        int panelTop = top
                + SpherePouchInventoryLayout.PANEL_Y;

        StormlightGuiStyle.drawSpherePanel(
                graphics,
                client.font,
                Component.translatable(
                        "container.stormlight.sphere_pouch"
                ),
                panelLeft,
                panelTop,
                SpherePouchInventoryLayout.PANEL_WIDTH,
                SpherePouchInventoryLayout.PANEL_HEIGHT,
                left + SpherePouchInventoryLayout.SLOT_START_X,
                top + SpherePouchInventoryLayout.SLOT_START_Y,
                4,
                4
        );
    }

    private static void send(AbilityId ability, AbilityInputPayload.Action action) {
        ClientPlayNetworking.send(new AbilityInputPayload(ability, action));
    }
}
