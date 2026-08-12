package com.scrotey.stormlight.client.progression;

import com.scrotey.stormlight.network.ChooseRadiantOrderPayload;
import com.scrotey.stormlight.network.OpenRadiantProgressionPayload;
import com.scrotey.stormlight.network.UnlockRadiantLevelPayload;
import com.scrotey.stormlight.progression.RadiantLevel;
import com.scrotey.stormlight.progression.RadiantOrder;
import com.scrotey.stormlight.progression.RadiantOrderRegistry;
import com.scrotey.stormlight.progression.RadiantProgression;

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

    private static final int ROW_HEIGHT = 42;
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
    private boolean unlocking;

    private int panelWidth;
    private int panelHeight;
    private int panelLeft;
    private int panelTop;
    private int scrollOffset;
    private int unlockTargetLevel;
    private Button unlockButton;

    public RadiantProgressionScreen(
            OpenRadiantProgressionPayload state
    ) {
        super(Component.translatable("screen.stormlight.radiant.title"));
        this.state = state;
    }

    @Override
    protected void init() {
        panelWidth = clamp(
                (int) (width * 0.62F),
                MIN_PANEL_WIDTH,
                MAX_PANEL_WIDTH
        );
        panelHeight = clamp(
                (int) (height * 0.62F),
                MIN_PANEL_HEIGHT,
                MAX_PANEL_HEIGHT
        );
        panelWidth = Math.min(panelWidth, width - 24);
        panelHeight = Math.min(panelHeight, height - 20);
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;

        int centreX = width / 2;

        if (state.orderNetworkId()
                == RadiantOrderRegistry.NO_ORDER_NETWORK_ID) {
            Button chooseButton = addRenderableWidget(
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
            chooseButton.active = state.experienceLevel()
                    >= RadiantProgression.experienceCost(1);
        } else if (state.level() < RadiantProgression.MAX_LEVEL) {
            unlockTargetLevel = state.level() + 1;
            int cost = RadiantProgression.experienceCost(
                    unlockTargetLevel
            );

            centreScrollOn(unlockTargetLevel);
            unlockButton = addRenderableWidget(
                    Button.builder(
                            Component.translatable(
                                    "button.stormlight.radiant.unlock",
                                    unlockTargetLevel,
                                    cost
                            ),
                            this::unlockLevel
                    ).bounds(
                            panelLeft + panelWidth - 121,
                            0,
                            101,
                            20
                    ).build()
            );
            updateUnlockButton();
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

    private void unlockLevel(Button button) {
        if (unlocking || unlockTargetLevel <= 0) {
            return;
        }

        unlocking = true;
        button.active = false;
        button.setMessage(
                Component.translatable("button.stormlight.radiant.unlocking")
        );
        ClientPlayNetworking.send(
                new UnlockRadiantLevelPayload(
                        state.lecternPos(),
                        unlockTargetLevel
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

        // Render widgets after the parchment and tree so buttons remain visible.
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawSkillTree(GuiGraphicsExtractor graphics) {
        RadiantOrder order = RadiantOrderRegistry
                .byNetworkId(state.orderNetworkId())
                .orElse(RadiantOrderRegistry.WINDRUNNER);

        drawCentered(
                graphics,
                Component.translatable(order.translationKey()),
                width / 2,
                panelTop + 45,
                WINDRUNNER_BLUE
        );
        drawCentered(
                graphics,
                Component.translatable(
                        "screen.stormlight.radiant.xp_available",
                        state.experienceLevel()
                ),
                width / 2,
                panelTop + 57,
                MUTED_INK
        );

        int treeTop = treeTop();
        int treeBottom = treeBottom();
        int nodeX = panelLeft + 43;

        for (RadiantLevel level : order.levels()) {
            int rowTop = treeTop
                    + (level.level() - 1) * ROW_HEIGHT
                    - scrollOffset;
            int nodeY = rowTop + ROW_HEIGHT / 2;

            if (rowTop < treeTop
                    || rowTop + ROW_HEIGHT > treeBottom) {
                continue;
            }

            boolean unlocked = level.level() <= state.level();
            boolean available = level.level() == state.level() + 1;
            int lineColour = unlocked ? WINDRUNNER_BLUE : LOCKED;

            if (level.level() > RadiantProgression.MIN_LEVEL) {
                // Join the outer edges of adjacent nodes. Drawing from centre
                // to centre allowed a later segment to repaint a line through
                // the number inside the node above it.
                int lineTop = Math.max(
                        treeTop,
                        nodeY - ROW_HEIGHT + 14
                );
                int lineBottom = Math.min(
                        treeBottom,
                        nodeY - 14
                );
                graphics.fill(
                        nodeX - 1,
                        lineTop,
                        nodeX + 2,
                        lineBottom,
                        lineColour
                );
            }

            drawLevelNode(
                    graphics,
                    level,
                    nodeX,
                    nodeY,
                    unlocked,
                    available
            );
        }

        graphics.fill(
                panelLeft + 17,
                treeTop - 2,
                panelLeft + panelWidth - 17,
                treeTop,
                PAGE_EDGE
        );
        graphics.fill(
                panelLeft + 17,
                treeBottom,
                panelLeft + panelWidth - 17,
                treeBottom + 2,
                PAGE_EDGE
        );

        if (maxScrollOffset() > 0) {
            graphics.text(
                    font,
                    Component.translatable(
                            "screen.stormlight.radiant.scroll_hint"
                    ),
                    panelLeft + 20,
                    panelTop + panelHeight - 31,
                    MUTED_INK,
                    false
            );
        }
    }

    private void drawLevelNode(
            GuiGraphicsExtractor graphics,
            RadiantLevel level,
            int nodeX,
            int nodeY,
            boolean unlocked,
            boolean available
    ) {
        int outer = unlocked
                ? WINDRUNNER_BLUE
                : available ? PAGE_EDGE : LOCKED;
        int inner = unlocked
                ? WINDRUNNER_PALE
                : available ? PARCHMENT : PARCHMENT_DARK;
        int textColour = unlocked || available ? INK : MUTED_INK;

        graphics.fill(
                nodeX - 14,
                nodeY - 14,
                nodeX + 14,
                nodeY + 14,
                outer
        );
        graphics.fill(
                nodeX - 10,
                nodeY - 10,
                nodeX + 10,
                nodeY + 10,
                inner
        );
        drawCentered(
                graphics,
                Component.literal(Integer.toString(level.level())),
                nodeX,
                nodeY - 4,
                textColour
        );

        int textX = nodeX + 22;
        graphics.text(
                font,
                Component.translatable(level.unlockTranslationKey()),
                textX,
                nodeY - 12,
                textColour,
                false
        );
        // On normal layouts, keep the reward summary beneath its title.
        // Very narrow layouts omit it so the capacity column cannot overlap.
        if (panelWidth >= 600) {
            graphics.text(
                    font,
                    Component.translatable(
                            level.descriptionTranslationKey()
                    ),
                    textX,
                    nodeY + 1,
                    unlocked ? WINDRUNNER_BLUE : MUTED_INK,
                    false
            );
        }

        // Every capacity value begins at the same X coordinate, forming a
        // clean vertical column independently of the reward text's length.
        int capacityX = panelLeft + panelWidth - 260;
        graphics.text(
                font,
                Component.translatable(
                        "screen.stormlight.radiant.capacity",
                        level.stormlightCapacity()
                ),
                capacityX,
                nodeY + 1,
                unlocked ? WINDRUNNER_BLUE : MUTED_INK,
                false
        );

        if (unlocked) {
            graphics.text(
                    font,
                    Component.translatable(
                            "screen.stormlight.radiant.unlocked"
                    ),
                    panelLeft + panelWidth - 76,
                    nodeY - 4,
                    WINDRUNNER_BLUE,
                    false
            );
        }
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double scrollX,
            double scrollY
    ) {
        if (state.orderNetworkId()
                != RadiantOrderRegistry.NO_ORDER_NETWORK_ID
                && mouseX >= panelLeft + 14
                && mouseX < panelLeft + panelWidth - 14
                && mouseY >= treeTop()
                && mouseY < treeBottom()
                && scrollY != 0.0) {
            int direction = scrollY > 0.0 ? -1 : 1;
            setScrollOffset(scrollOffset + direction * ROW_HEIGHT);
            return true;
        }

        return super.mouseScrolled(
                mouseX,
                mouseY,
                scrollX,
                scrollY
        );
    }

    private void centreScrollOn(int level) {
        int viewportHeight = treeBottom() - treeTop();
        int desired = (level - 1) * ROW_HEIGHT
                - (viewportHeight - ROW_HEIGHT) / 2;
        scrollOffset = clamp(desired, 0, maxScrollOffset());
    }

    private void setScrollOffset(int value) {
        scrollOffset = clamp(value, 0, maxScrollOffset());
        updateUnlockButton();
    }

    private int maxScrollOffset() {
        int contentHeight = RadiantProgression.MAX_LEVEL * ROW_HEIGHT;
        return Math.max(0, contentHeight - (treeBottom() - treeTop()));
    }

    private int treeTop() {
        return panelTop + 72;
    }

    private int treeBottom() {
        return panelTop + panelHeight - 44;
    }

    private void updateUnlockButton() {
        if (unlockButton == null) {
            return;
        }

        int rowTop = treeTop()
                + (unlockTargetLevel - 1) * ROW_HEIGHT
                - scrollOffset;
        int buttonY = rowTop + (ROW_HEIGHT - unlockButton.getHeight()) / 2;
        boolean fullyVisible = buttonY >= treeTop()
                && buttonY + unlockButton.getHeight() <= treeBottom();

        unlockButton.setY(buttonY);
        unlockButton.visible = fullyVisible;
        unlockButton.active = fullyVisible
                && !unlocking
                && state.experienceLevel()
                >= RadiantProgression.experienceCost(unlockTargetLevel);
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

        graphics.fill(
                cardLeft - 2,
                cardTop - 2,
                cardRight + 2,
                cardBottom + 2,
                WINDRUNNER_BLUE
        );
        graphics.fill(
                cardLeft,
                cardTop,
                cardRight,
                cardBottom,
                hovered ? 0xFFE0CEA0 : 0xFFD6BE88
        );
        graphics.fill(
                cardLeft + 5,
                cardTop + 5,
                cardRight - 5,
                cardBottom - 5,
                0x55FFF0C6
        );

        int glyphY = cardTop + 16;
        graphics.fill(
                centreX - 29,
                glyphY,
                centreX + 29,
                glyphY + 2,
                WINDRUNNER_BLUE
        );
        graphics.fill(
                centreX - 20,
                glyphY + 6,
                centreX + 20,
                glyphY + 8,
                WINDRUNNER_BLUE
        );
        graphics.fill(
                centreX - 10,
                glyphY + 12,
                centreX + 10,
                glyphY + 14,
                WINDRUNNER_BLUE
        );

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
        drawCentered(
                graphics,
                Component.translatable(
                        "screen.stormlight.radiant.order_cost",
                        RadiantProgression.experienceCost(1),
                        state.experienceLevel()
                ),
                centreX,
                cardBottom - 17,
                state.experienceLevel()
                        >= RadiantProgression.experienceCost(1)
                        ? WINDRUNNER_BLUE : 0xFF9A382F
        );
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

        graphics.fill(panelLeft + 11, panelTop + 9,
                panelLeft + 15, panelTop + panelHeight - 9, PARCHMENT_DARK);
        graphics.fill(panelLeft + panelWidth - 15, panelTop + 9,
                panelLeft + panelWidth - 11, panelTop + panelHeight - 9,
                PARCHMENT_DARK);
        graphics.fill(panelLeft + 15, panelTop + 9,
                panelLeft + panelWidth - 15, panelTop + 12, PARCHMENT_DARK);
        graphics.fill(panelLeft + 15, panelTop + panelHeight - 12,
                panelLeft + panelWidth - 15, panelTop + panelHeight - 9,
                PARCHMENT_DARK);

        graphics.fill(panelLeft + 17, panelTop + 15,
                panelLeft + 55, panelTop + 19, STAIN);
        graphics.fill(panelLeft + 21, panelTop + 19,
                panelLeft + 42, panelTop + 22, STAIN);
        graphics.fill(panelLeft + panelWidth - 61,
                panelTop + panelHeight - 22,
                panelLeft + panelWidth - 18,
                panelTop + panelHeight - 16, STAIN);

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
