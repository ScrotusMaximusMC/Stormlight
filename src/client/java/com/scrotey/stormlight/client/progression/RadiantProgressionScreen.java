package com.scrotey.stormlight.client.progression;

import com.scrotey.stormlight.network.ChooseRadiantOrderPayload;
import com.scrotey.stormlight.network.OpenRadiantProgressionPayload;
import com.scrotey.stormlight.progression.RadiantOrder;
import com.scrotey.stormlight.progression.RadiantOrderRegistry;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class RadiantProgressionScreen extends Screen {
    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 210;

    private static final int BACKDROP = 0xD806101B;
    private static final int PANEL_EDGE = 0xFF17384A;
    private static final int PANEL_INNER = 0xF00A1E2C;
    private static final int CYAN = 0xFF8DEBFF;
    private static final int WHITE = 0xFFF1FCFF;
    private static final int MUTED = 0xFF8DA8B5;
    private static final int LOCKED = 0xFF40515A;

    private final OpenRadiantProgressionPayload state;
    private boolean choosing;

    public RadiantProgressionScreen(
            OpenRadiantProgressionPayload state
    ) {
        super(Component.translatable("screen.stormlight.radiant.title"));
        this.state = state;
    }

    @Override
    protected void init() {
        int centreX = width / 2;
        int panelTop = (height - PANEL_HEIGHT) / 2;

        if (state.orderNetworkId()
                == RadiantOrderRegistry.NO_ORDER_NETWORK_ID) {
            addRenderableWidget(
                    Button.builder(
                            Component.translatable(
                                    "button.stormlight.choose_windrunner"
                            ),
                            button -> chooseWindrunner(button)
                    ).bounds(
                            centreX - 80,
                            panelTop + 122,
                            160,
                            20
                    ).build()
            );
        }

        addRenderableWidget(
                Button.builder(
                        Component.translatable("gui.done"),
                        button -> onClose()
                ).bounds(
                        centreX - 40,
                        panelTop + PANEL_HEIGHT - 28,
                        80,
                        20
                ).build()
        );
    }

    private void chooseWindrunner(Button button) {
        if (choosing) {
            return;
        }

        choosing = true;
        button.active = false;
        button.setMessage(
                Component.translatable("button.stormlight.choosing")
        );

        ClientPlayNetworking.send(
                new ChooseRadiantOrderPayload(
                        state.lecternPos(),
                        RadiantOrderRegistry.WINDRUNNER.networkId()
                )
        );
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;

        graphics.fill(0, 0, width, height, BACKDROP);
        graphics.fill(
                left - 2,
                top - 2,
                left + PANEL_WIDTH + 2,
                top + PANEL_HEIGHT + 2,
                0xFF02080D
        );
        graphics.fill(
                left,
                top,
                left + PANEL_WIDTH,
                top + PANEL_HEIGHT,
                PANEL_EDGE
        );
        graphics.fill(
                left + 3,
                top + 3,
                left + PANEL_WIDTH - 3,
                top + PANEL_HEIGHT - 3,
                PANEL_INNER
        );

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        drawCentered(
                graphics,
                Component.translatable("screen.stormlight.radiant.title"),
                width / 2,
                top + 13,
                WHITE
        );

        graphics.fill(
                left + 42,
                top + 29,
                left + PANEL_WIDTH - 42,
                top + 30,
                CYAN
        );

        if (state.orderNetworkId()
                == RadiantOrderRegistry.NO_ORDER_NETWORK_ID) {
            drawOrderSelection(graphics, left, top);
        } else {
            drawSkillTree(graphics, left, top);
        }
    }

    private void drawOrderSelection(
            GuiGraphicsExtractor graphics,
            int left,
            int top
    ) {
        drawCentered(
                graphics,
                Component.translatable(
                        "screen.stormlight.radiant.choose_order"
                ),
                width / 2,
                top + 43,
                CYAN
        );
        drawCentered(
                graphics,
                Component.translatable(
                        "screen.stormlight.radiant.choice_warning"
                ),
                width / 2,
                top + 61,
                MUTED
        );

        int cardLeft = left + 74;
        int cardTop = top + 78;
        int cardRight = left + PANEL_WIDTH - 74;
        int cardBottom = top + 150;

        graphics.fill(
                cardLeft,
                cardTop,
                cardRight,
                cardBottom,
                0xFF071722
        );
        graphics.fill(
                cardLeft + 1,
                cardTop + 1,
                cardRight - 1,
                cardTop + 3,
                CYAN
        );

        drawCentered(
                graphics,
                Component.translatable("order.stormlight.windrunner"),
                width / 2,
                cardTop + 14,
                WHITE
        );
        drawCentered(
                graphics,
                Component.translatable(
                        "screen.stormlight.radiant.windrunner_preview"
                ),
                width / 2,
                cardTop + 30,
                MUTED
        );
    }

    private void drawSkillTree(
            GuiGraphicsExtractor graphics,
            int left,
            int top
    ) {
        RadiantOrder order = RadiantOrderRegistry
                .byNetworkId(state.orderNetworkId())
                .orElse(RadiantOrderRegistry.WINDRUNNER);

        drawCentered(
                graphics,
                Component.translatable(order.translationKey()),
                width / 2,
                top + 41,
                order.colour()
        );

        int lineY = top + 94;
        graphics.fill(left + 66, lineY, left + 254, lineY + 2, LOCKED);
        graphics.fill(left + 66, lineY, left + 160, lineY + 2, CYAN);

        drawNode(graphics, left + 63, top + 77, true, "1");
        drawNode(graphics, left + 151, top + 77, false, "2");
        drawNode(graphics, left + 239, top + 77, false, "3");

        drawCentered(
                graphics,
                Component.translatable(
                        "screen.stormlight.radiant.level",
                        state.level()
                ),
                width / 2,
                top + 114,
                WHITE
        );
        drawCentered(
                graphics,
                Component.translatable(
                        "screen.stormlight.radiant.level_one_bonuses"
                ),
                width / 2,
                top + 132,
                CYAN
        );
        drawCentered(
                graphics,
                Component.translatable(
                        "screen.stormlight.radiant.future_levels"
                ),
                width / 2,
                top + 150,
                MUTED
        );
    }

    private void drawNode(
            GuiGraphicsExtractor graphics,
            int centreX,
            int centreY,
            boolean unlocked,
            String label
    ) {
        int outer = unlocked ? CYAN : LOCKED;
        int inner = unlocked ? 0xFF17465D : 0xFF152029;

        graphics.fill(
                centreX - 13,
                centreY - 13,
                centreX + 13,
                centreY + 13,
                outer
        );
        graphics.fill(
                centreX - 10,
                centreY - 10,
                centreX + 10,
                centreY + 10,
                inner
        );

        drawCentered(
                graphics,
                Component.literal(label),
                centreX,
                centreY - 4,
                unlocked ? WHITE : MUTED
        );
    }

    private void drawCentered(
            GuiGraphicsExtractor graphics,
            Component text,
            int centreX,
            int y,
            int colour
    ) {
        graphics.text(
                font,
                text,
                centreX - font.width(text) / 2,
                y,
                colour,
                true
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
