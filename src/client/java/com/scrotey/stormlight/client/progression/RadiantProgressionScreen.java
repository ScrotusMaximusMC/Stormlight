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
    private static final int MIN_PANEL_WIDTH = 390;
    private static final int MAX_PANEL_WIDTH = 680;
    private static final int MIN_PANEL_HEIGHT = 250;
    private static final int MAX_PANEL_HEIGHT = 410;

    private static final int BACKDROP = 0xD80A0806;
    private static final int SHADOW = 0xB0000000;
    private static final int LEATHER_DARK = 0xFF3A2418;
    private static final int LEATHER = 0xFF62402A;
    private static final int PAGE_EDGE = 0xFF8A673D;
    private static final int PARCHMENT_DARK = 0xFFD0AD70;
    private static final int PARCHMENT = 0xFFE4C98E;
    private static final int PARCHMENT_LIGHT = 0xFFF0DBA6;
    private static final int STAIN = 0x39734A25;
    private static final int INK = 0xFF302219;
    private static final int MUTED_INK = 0xFF705B42;
    private static final int WINDRUNNER_BLUE = 0xFF276E83;
    private static final int WINDRUNNER_PALE = 0xFF8FC6CF;
    private static final int LOCKED = 0xFF8B775B;

    private final OpenRadiantProgressionPayload state;
    private boolean choosing;

    private int panelWidth;
    private int panelHeight;
    private int panelLeft;
    private int panelTop;

    public RadiantProgressionScreen(
            OpenRadiantProgressionPayload state
    ) {
        super(Component.translatable("screen.stormlight.radiant.title"));
        this.state = state;
    }

    @Override
    protected void init() {
        panelWidth = clamp((int) (width * 0.62F), MIN_PANEL_WIDTH, MAX_PANEL_WIDTH);
        panelHeight = clamp((int) (height * 0.62F), MIN_PANEL_HEIGHT, MAX_PANEL_HEIGHT);

        // Leave a small safety margin for low resolutions and large GUI scales.
        panelWidth = Math.min(panelWidth, width - 24);
        panelHeight = Math.min(panelHeight, height - 20);
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;

        int centreX = width / 2;

        if (state.orderNetworkId()
                == RadiantOrderRegistry.NO_ORDER_NETWORK_ID) {
            addRenderableWidget(
                    Button.builder(
                            Component.translatable(
                                    "button.stormlight.choose_windrunner"
                            ),
                            this::chooseWindrunner
                    ).bounds(
                            centreX - 105,
                            panelTop + panelHeight - 73,
                            210,
                            24
                    ).build()
            );
        }

        addRenderableWidget(
                Button.builder(
                        Component.translatable("gui.done"),
                        button -> onClose()
                ).bounds(
                        centreX - 45,
                        panelTop + panelHeight - 35,
                        90,
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
        drawParchmentBook(graphics);

        drawCentered(
                graphics,
                Component.translatable("screen.stormlight.radiant.title"),
                width / 2,
                panelTop + 18,
                INK
        );

        drawOrnament(graphics, panelTop + 34);

        if (state.orderNetworkId()
                == RadiantOrderRegistry.NO_ORDER_NETWORK_ID) {
            drawOrderSelection(graphics, mouseX, mouseY);
        } else {
            drawSkillTree(graphics);
        }

        // Widgets must be extracted last. Previously the parchment card was
        // painted over the Windrunner button, leaving an invisible hitbox.
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawParchmentBook(GuiGraphicsExtractor graphics) {
        graphics.fill(0, 0, width, height, BACKDROP);

        graphics.fill(
                panelLeft + 7,
                panelTop + 8,
                panelLeft + panelWidth + 9,
                panelTop + panelHeight + 10,
                SHADOW
        );
        graphics.fill(
                panelLeft - 5,
                panelTop - 5,
                panelLeft + panelWidth + 5,
                panelTop + panelHeight + 5,
                LEATHER_DARK
        );
        graphics.fill(
                panelLeft - 2,
                panelTop - 2,
                panelLeft + panelWidth + 2,
                panelTop + panelHeight + 2,
                LEATHER
        );
        graphics.fill(
                panelLeft,
                panelTop,
                panelLeft + panelWidth,
                panelTop + panelHeight,
                PAGE_EDGE
        );
        graphics.fill(
                panelLeft + 4,
                panelTop + 4,
                panelLeft + panelWidth - 4,
                panelTop + panelHeight - 4,
                PARCHMENT
        );
        graphics.fill(
                panelLeft + 11,
                panelTop + 9,
                panelLeft + panelWidth - 11,
                panelTop + panelHeight - 9,
                PARCHMENT_LIGHT
        );

        // Uneven aged edges and faded stains make the panel read as paper
        // without requiring a large screen texture.
        graphics.fill(panelLeft + 11, panelTop + 9,
                panelLeft + 15, panelTop + panelHeight - 9, PARCHMENT_DARK);
        graphics.fill(panelLeft + panelWidth - 15, panelTop + 9,
                panelLeft + panelWidth - 11, panelTop + panelHeight - 9, PARCHMENT_DARK);
        graphics.fill(panelLeft + 15, panelTop + 9,
                panelLeft + panelWidth - 15, panelTop + 12, PARCHMENT_DARK);
        graphics.fill(panelLeft + 15, panelTop + panelHeight - 12,
                panelLeft + panelWidth - 15, panelTop + panelHeight - 9, PARCHMENT_DARK);

        graphics.fill(panelLeft + 17, panelTop + 15,
                panelLeft + 55, panelTop + 19, STAIN);
        graphics.fill(panelLeft + 21, panelTop + 19,
                panelLeft + 42, panelTop + 22, STAIN);
        graphics.fill(panelLeft + panelWidth - 61, panelTop + panelHeight - 22,
                panelLeft + panelWidth - 18, panelTop + panelHeight - 16, STAIN);

        int bindingX = width / 2;
        graphics.fill(bindingX - 3, panelTop + 5,
                bindingX + 3, panelTop + panelHeight - 5, 0x50553A24);
        graphics.fill(bindingX - 1, panelTop + 7,
                bindingX + 1, panelTop + panelHeight - 7, 0x606F5030);
    }

    private void drawOrnament(
            GuiGraphicsExtractor graphics,
            int y
    ) {
        int centreX = width / 2;
        int halfWidth = Math.min(135, panelWidth / 3);
        graphics.fill(centreX - halfWidth, y,
                centreX - 8, y + 1, MUTED_INK);
        graphics.fill(centreX + 8, y,
                centreX + halfWidth, y + 1, MUTED_INK);
        graphics.fill(centreX - 3, y - 2,
                centreX + 3, y + 4, WINDRUNNER_BLUE);
        graphics.fill(centreX - 1, y,
                centreX + 1, y + 2, PARCHMENT_LIGHT);
    }

    private void drawOrderSelection(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY
    ) {
        int centreX = width / 2;
        drawCentered(
                graphics,
                Component.translatable(
                        "screen.stormlight.radiant.choose_order"
                ),
                centreX,
                panelTop + 50,
                WINDRUNNER_BLUE
        );
        drawCentered(
                graphics,
                Component.translatable(
                        "screen.stormlight.radiant.choice_warning"
                ),
                centreX,
                panelTop + 68,
                MUTED_INK
        );

        int cardWidth = Math.min(330, panelWidth - 90);
        int cardHeight = Math.max(92, panelHeight - 150);
        int cardLeft = centreX - cardWidth / 2;
        int cardTop = panelTop + 84;
        int cardRight = cardLeft + cardWidth;
        int cardBottom = Math.min(
                cardTop + cardHeight,
                panelTop + panelHeight - 82
        );
        boolean hovered = mouseX >= cardLeft && mouseX < cardRight
                && mouseY >= cardTop && mouseY < cardBottom;

        graphics.fill(cardLeft - 2, cardTop - 2,
                cardRight + 2, cardBottom + 2, WINDRUNNER_BLUE);
        graphics.fill(cardLeft, cardTop,
                cardRight, cardBottom,
                hovered ? 0xFFE0CEA0 : 0xFFD6BE88);
        graphics.fill(cardLeft + 5, cardTop + 5,
                cardRight - 5, cardBottom - 5, 0x55FFF0C6);

        // Simple windswept glyph, kept abstract and spoiler-safe.
        int glyphY = cardTop + 16;
        graphics.fill(centreX - 29, glyphY,
                centreX + 29, glyphY + 2, WINDRUNNER_BLUE);
        graphics.fill(centreX - 20, glyphY + 6,
                centreX + 20, glyphY + 8, WINDRUNNER_BLUE);
        graphics.fill(centreX - 10, glyphY + 12,
                centreX + 10, glyphY + 14, WINDRUNNER_BLUE);

        drawCentered(
                graphics,
                Component.translatable("order.stormlight.windrunner"),
                centreX,
                glyphY + 25,
                INK
        );
        drawCentered(
                graphics,
                Component.translatable(
                        "screen.stormlight.radiant.windrunner_preview"
                ),
                centreX,
                glyphY + 43,
                MUTED_INK
        );
    }

    private void drawSkillTree(GuiGraphicsExtractor graphics) {
        RadiantOrder order = RadiantOrderRegistry
                .byNetworkId(state.orderNetworkId())
                .orElse(RadiantOrderRegistry.WINDRUNNER);
        int centreX = width / 2;

        drawCentered(
                graphics,
                Component.translatable(order.translationKey()),
                centreX,
                panelTop + 51,
                WINDRUNNER_BLUE
        );

        int nodeY = panelTop + Math.max(105, panelHeight / 2 - 18);
        int spacing = Math.min(120, panelWidth / 5);
        graphics.fill(centreX - spacing, nodeY - 1,
                centreX + spacing, nodeY + 2, LOCKED);
        graphics.fill(centreX - spacing, nodeY - 1,
                centreX, nodeY + 2, WINDRUNNER_BLUE);

        drawNode(graphics, centreX - spacing, nodeY, true, "1");
        drawNode(graphics, centreX, nodeY, false, "2");
        drawNode(graphics, centreX + spacing, nodeY, false, "3");

        drawCentered(
                graphics,
                Component.translatable(
                        "screen.stormlight.radiant.level",
                        state.level()
                ),
                centreX,
                nodeY + 35,
                INK
        );
        drawCentered(
                graphics,
                Component.translatable(
                        "screen.stormlight.radiant.level_one_bonuses"
                ),
                centreX,
                nodeY + 54,
                WINDRUNNER_BLUE
        );
        drawCentered(
                graphics,
                Component.translatable(
                        "screen.stormlight.radiant.future_levels"
                ),
                centreX,
                nodeY + 72,
                MUTED_INK
        );
    }

    private void drawNode(
            GuiGraphicsExtractor graphics,
            int centreX,
            int centreY,
            boolean unlocked,
            String label
    ) {
        int outer = unlocked ? WINDRUNNER_BLUE : LOCKED;
        int inner = unlocked ? WINDRUNNER_PALE : PARCHMENT_DARK;

        graphics.fill(centreX - 16, centreY - 16,
                centreX + 16, centreY + 16, outer);
        graphics.fill(centreX - 12, centreY - 12,
                centreX + 12, centreY + 12, inner);
        graphics.fill(centreX - 9, centreY - 9,
                centreX + 9, centreY + 9, PARCHMENT_LIGHT);

        drawCentered(
                graphics,
                Component.literal(label),
                centreX,
                centreY - 4,
                unlocked ? INK : MUTED_INK
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
                false
        );
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
