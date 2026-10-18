package com.scrotey.stormlight.client.progression;

import com.scrotey.stormlight.network.ChooseRadiantOrderPayload;
import com.scrotey.stormlight.network.OpenRadiantProgressionPayload;
import com.scrotey.stormlight.network.UnlockRadiantLevelPayload;
import com.scrotey.stormlight.network.TakeLecternBookPayload;
import com.scrotey.stormlight.progression.RadiantLevel;
import com.scrotey.stormlight.progression.RadiantOrder;
import com.scrotey.stormlight.progression.RadiantOrderRegistry;
import com.scrotey.stormlight.progression.RadiantProgression;
import com.scrotey.stormlight.progression.SprenBondLevel;
import com.scrotey.stormlight.progression.SprenBondProgression;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public final class RadiantProgressionScreen extends Screen {
    private static final int MAX_PANEL_WIDTH = 1000;
    private static final int MIN_ROW_HEIGHT = 34;
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
    private static final int BOND_GOLD = 0xFF9A7135;
    private static final int IDEAL_GOLD = 0xFF805927;
    private static final int LOCKED = 0xFF8B775B;
    private static final int ABILITY_GAP = 6;

    private final OpenRadiantProgressionPayload state;
    private boolean choosing;
    private boolean unlocking;

    private int panelWidth;
    private int panelHeight;
    private int panelLeft;
    private int panelTop;
    private int minimumRowHeight;
    private int scrollIndex;
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
        panelWidth = Math.min(MAX_PANEL_WIDTH, width - 24);
        panelHeight = height - 20;
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;
        minimumRowHeight = MIN_ROW_HEIGHT;
        for (SprenBondLevel bond : SprenBondProgression.levels()) {
            boolean unlocked = state.level()
                    >= bond.requiredRadiantLevel();
            int lineCount = font.split(idealWords(bond, unlocked),
                    bondWordsWidth()).size();
            if (unlocked && bond.level() <= 3) {
                int detailLines = font.split(Component.translatable(
                        abilityDescriptionKey(bond.level())
                ), bondWordsWidth()).size();
                minimumRowHeight = Math.max(minimumRowHeight,
                        24 + 18 * (lineCount + detailLines)
                                + 2 * ABILITY_GAP);
            } else {
                minimumRowHeight = Math.max(minimumRowHeight,
                        20 + 18 * (lineCount - 1));
            }
        }
        int radiantTextWidth = Math.max(1, panelWidth / 2 - 65);
        int masteryLines = font.split(Component.translatable(
                "progression.stormlight.windrunner.radiant_mastery.description"
        ), radiantTextWidth).size();
        int mobLashingLines = font.split(Component.translatable(
                "progression.stormlight.windrunner.radiant_mastery.mob_lashing"
        ), radiantTextWidth).size();
        minimumRowHeight = Math.max(minimumRowHeight,
                24 + 18 * (masteryLines + mobLashingLines));
        int lockedLineCount = font.split(
                Component.translatable(
                        "screen.stormlight.spren_bond.locked_words"
                ),
                bondWordsWidth()
        ).size();
        minimumRowHeight = Math.max(
                minimumRowHeight,
                20 + 18 * (lockedLineCount - 1)
        );
        scrollIndex = Math.min(scrollIndex, maxScrollIndex());

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
                            panelLeft + 24,
                            panelTop + panelHeight - 35,
                            panelWidth / 2 - 48,
                            20
                    ).build()
            );
            chooseButton.active = state.experienceLevel()
                    >= RadiantProgression.experienceCost(1);
        } else if (state.level() < RadiantProgression.MAX_LEVEL) {
            unlockTargetLevel = state.level() + 1;
            scrollIndex = Math.min(
                    maxScrollIndex(),
                    Math.max(0, unlockTargetLevel - visibleRows())
            );
            int cost = RadiantProgression.experienceCost(
                    unlockTargetLevel
            );

            unlockButton = addRenderableWidget(
                    Button.builder(
                            Component.translatable(
                                    "button.stormlight.radiant.unlock",
                                    unlockTargetLevel,
                                    cost
                            ),
                            this::unlockLevel
                    ).bounds(
                            panelLeft + 24,
                            panelTop + panelHeight - 35,
                            panelWidth / 2 - 48,
                            20
                    ).build()
            );
            unlockButton.active = state.experienceLevel() >= cost;
        }

        int bottomButtonWidth = 90;
        int bottomButtonGap = 8;
        int bottomButtonsWidth =
                bottomButtonWidth * 2 + bottomButtonGap;

        int bottomButtonStartX =
                centreX + (panelWidth / 2 - bottomButtonsWidth) / 2;

        int bottomButtonY =
                panelTop + panelHeight - 35;

        addRenderableWidget(
                Button.builder(
                        Component.literal("Take Book"),
                        button -> takeBook()
                ).bounds(
                        bottomButtonStartX,
                        bottomButtonY,
                        bottomButtonWidth,
                        20
                ).build()
        );

        addRenderableWidget(
                Button.builder(
                        Component.translatable("gui.done"),
                        button -> onClose()
                ).bounds(
                        bottomButtonStartX
                                + bottomButtonWidth
                                + bottomButtonGap,
                        bottomButtonY,
                        bottomButtonWidth,
                        20
                ).build()
        );
    }

    private void takeBook() {
        ClientPlayNetworking.send(
                new TakeLecternBookPayload(
                        state.lecternPos()
                )
        );

        onClose();
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
        drawOrnament(graphics, panelTop + 32);

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

        int bindingX = width / 2;
        int leftPageCentre = (panelLeft + 17 + bindingX - 7) / 2;
        int rightPageCentre = (
                bindingX + 7 + panelLeft + panelWidth - 17
        ) / 2;

        drawCentered(
                graphics,
                Component.translatable(order.translationKey()),
                leftPageCentre,
                panelTop + 42,
                WINDRUNNER_BLUE
        );
        drawCentered(
                graphics,
                Component.translatable(
                        "screen.stormlight.radiant.xp_available",
                        state.experienceLevel()
                ),
                leftPageCentre,
                panelTop + 53,
                MUTED_INK
        );

        drawCentered(
                graphics,
                Component.translatable(
                        "screen.stormlight.spren_bond.title"
                ),
                rightPageCentre,
                panelTop + 42,
                BOND_GOLD
        );
        drawCentered(
                graphics,
                Component.translatable(
                        "screen.stormlight.spren_bond.current",
                        SprenBondProgression.getLevelForRadiantLevel(
                                state.level()
                        )
                ),
                rightPageCentre,
                panelTop + 53,
                MUTED_INK
        );

        int treeTop = treeTop();
        int treeBottom = treeBottom();
        int nodeX = panelLeft + 39;

        drawSprenBondColumn(
                graphics,
                treeTop,
                treeBottom,
                bindingX
        );

        for (RadiantLevel level : order.levels()) {
            if (!rowVisible(level.level() - 1)) {
                continue;
            }
            int nodeY = rowCentre(level.level() - 1);

            boolean unlocked = level.level() <= state.level();
            boolean available = level.level() == state.level() + 1;
            int lineColour = unlocked ? WINDRUNNER_BLUE : LOCKED;

            if (level.level() > RadiantProgression.MIN_LEVEL) {
                // Join the outer edges of adjacent nodes. Drawing from centre
                // to centre allowed a later segment to repaint a line through
                // the number inside the node above it.
                int lineTop = Math.max(
                        treeTop,
                        rowCentre(level.level() - 2) + 10
                );
                int lineBottom = Math.min(
                        treeBottom,
                        nodeY - 10
                );
                if (lineTop < lineBottom) {
                    graphics.fill(
                            nodeX - 1,
                            lineTop,
                            nodeX + 2,
                            lineBottom,
                            lineColour
                    );
                }
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
        if (maxScrollIndex() > 0) {
            int indicatorX = panelLeft + panelWidth - 18;
            if (scrollIndex > 0) {
                drawCentered(
                        graphics,
                        Component.literal("↑"),
                        indicatorX,
                        treeTop + 2,
                        MUTED_INK
                );
            }
            if (scrollIndex < maxScrollIndex()) {
                drawCentered(
                        graphics,
                        Component.literal("↓"),
                        indicatorX,
                        treeBottom - 12,
                        MUTED_INK
                );
            }
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
                nodeX - 10,
                nodeY - 10,
                nodeX + 10,
                nodeY + 10,
                outer
        );
        graphics.fill(
                nodeX - 7,
                nodeY - 7,
                nodeX + 7,
                nodeY + 7,
                inner
        );
        drawCentered(
                graphics,
                Component.literal(Integer.toString(level.level())),
                nodeX,
                nodeY - 4,
                textColour
        );

        int textX = nodeX + 16;
        boolean roomyRow = rowHeight() >= 34;
        graphics.text(
                font,
                Component.translatable(level.unlockTranslationKey()),
                textX,
                nodeY - (roomyRow ? 15 : 10),
                textColour,
                false
        );
        Component description = Component.translatable(
                level.descriptionTranslationKey()
        );
        if (level.level() == RadiantProgression.MAX_LEVEL) {
            int lineY = nodeY - 4;
            for (String key : List.of(
                    level.descriptionTranslationKey(),
                    "progression.stormlight.windrunner.radiant_mastery.mob_lashing"
            )) {
                for (FormattedCharSequence line : font.split(
                        Component.translatable(key),
                        Math.max(1, width / 2 - textX - 10))) {
                    graphics.text(font, line, textX, lineY,
                            unlocked ? WINDRUNNER_BLUE : MUTED_INK, false);
                    lineY += 9;
                }
            }
            graphics.text(font, Component.translatable(
                    "screen.stormlight.radiant.capacity",
                    level.stormlightCapacity()), textX, lineY,
                    unlocked ? WINDRUNNER_BLUE : MUTED_INK, false);
            return;
        }
        if (roomyRow
                && font.width(description) < width / 2 - textX - 10) {
            graphics.text(
                    font,
                    description,
                    textX,
                    nodeY - 4,
                    unlocked ? WINDRUNNER_BLUE : MUTED_INK,
                    false
            );
        }

        Component capacity = Component.translatable(
                "screen.stormlight.radiant.capacity",
                level.stormlightCapacity()
        );
        graphics.text(
                font,
                capacity,
                textX,
                nodeY + (roomyRow ? 7 : 1),
                unlocked ? WINDRUNNER_BLUE : MUTED_INK,
                false
        );
    }

    private void drawSprenBondColumn(
            GuiGraphicsExtractor graphics,
            int treeTop,
            int treeBottom,
            int bindingX
    ) {
        int currentBondLevel =
                SprenBondProgression.getLevelForRadiantLevel(
                        state.level()
                );
        int nodeX = bindingX + 29;

        graphics.fill(
                nodeX - 1,
                treeTop,
                nodeX + 2,
                treeBottom,
                LOCKED
        );

        if (currentBondLevel > 0) {
            SprenBondLevel currentBond = SprenBondProgression.levels()
                    .get(currentBondLevel - 1);
            int unlockedLineBottom = rowCentre(
                    currentBond.requiredRadiantLevel() - 1
            );
            graphics.fill(
                    nodeX - 1,
                    treeTop,
                    nodeX + 2,
                    Math.min(treeBottom, Math.max(treeTop,
                            unlockedLineBottom)),
                    WINDRUNNER_BLUE
            );
        }

        for (SprenBondLevel bond : SprenBondProgression.levels()) {
            if (!rowVisible(bond.requiredRadiantLevel() - 1)) {
                continue;
            }
            int nodeY = rowCentre(bond.requiredRadiantLevel() - 1);

            boolean unlocked = bond.level() <= currentBondLevel;
            int outer = unlocked ? WINDRUNNER_BLUE : LOCKED;
            int inner = unlocked ? WINDRUNNER_PALE : PARCHMENT_DARK;
            int textColour = unlocked ? INK : MUTED_INK;

            graphics.fill(
                    nodeX - 9,
                    nodeY - 9,
                    nodeX + 9,
                    nodeY + 9,
                    outer
            );
            graphics.fill(
                    nodeX - 6,
                    nodeY - 6,
                    nodeX + 6,
                    nodeY + 6,
                    inner
            );
            drawCentered(
                    graphics,
                    Component.literal(Integer.toString(bond.level())),
                    nodeX,
                    nodeY - 4,
                    textColour
            );

            int textX = nodeX + 15;
            Component heading = Component.translatable(
                    "screen.stormlight.spren_bond.level",
                    bond.level(),
                    Component.translatable(bond.titleTranslationKey())
            );

            graphics.text(
                    font,
                    heading,
                    textX,
                    nodeY - 12,
                    textColour,
                    false
            );

            Component wordsText = idealWords(bond, unlocked);
            List<FormattedCharSequence> words = font.split(
                    wordsText,
                    bondWordsWidth()
            );

            for (int line = 0;
                    line < words.size();
                    line++) {
                graphics.text(
                        font,
                        words.get(line),
                        textX,
                        nodeY + 1 + line * 9,
                        unlocked ? IDEAL_GOLD : MUTED_INK,
                        false
                );
            }
            if (bond.level() <= 3 && unlocked) {
                int detailY = nodeY + 1 + words.size() * 9
                        + ABILITY_GAP;
                graphics.text(
                        font,
                        Component.translatable(
                                abilityKey(bond.level())
                        ),
                        textX,
                        detailY,
                        WINDRUNNER_BLUE,
                        false
                );
                List<FormattedCharSequence> detail = font.split(
                        Component.translatable(
                                abilityDescriptionKey(bond.level())),
                        bondWordsWidth());
                for (int line = 0; line < detail.size(); line++) {
                    graphics.text(font, detail.get(line), textX,
                            detailY + 9 + line * 9,
                            INK, false);
                }
            }
        }
    }

    private Component idealWords(SprenBondLevel bond, boolean unlocked) {
        return Component.translatable(unlocked
                ? bond.wordsTranslationKey()
                : "screen.stormlight.spren_bond.locked_words")
                .withStyle(ChatFormatting.ITALIC);
    }

    private String abilityKey(int bondLevel) {
        return "screen.stormlight.spren_bond." + switch (bondLevel) {
            case 1 -> "spren_seek";
            case 2 -> "spren_shield";
            case 3 -> "shardblade";
            default -> throw new IllegalArgumentException();
        };
    }

    private String abilityDescriptionKey(int bondLevel) {
        return abilityKey(bondLevel) + ".description";
    }

    private int treeTop() {
        return panelTop + 65;
    }

    private int treeBottom() {
        return panelTop + panelHeight - 40;
    }

    private int rowHeight() {
        return Math.max(
                minimumRowHeight,
                (treeBottom() - treeTop()) / RadiantProgression.MAX_LEVEL
        );
    }

    private int bondWordsWidth() {
        return Math.max(1, panelWidth / 2 - 65);
    }

    private int rowCentre(int rowIndex) {
        return treeTop() + (rowIndex - scrollIndex) * rowHeight()
                + rowHeight() / 2;
    }

    private int visibleRows() {
        return Math.max(1, Math.min(
                RadiantProgression.MAX_LEVEL,
                (treeBottom() - treeTop()) / rowHeight()
        ));
    }

    private int maxScrollIndex() {
        return RadiantProgression.MAX_LEVEL - visibleRows();
    }

    private boolean rowVisible(int rowIndex) {
        return rowIndex >= scrollIndex
                && rowIndex < scrollIndex + visibleRows();
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
                && maxScrollIndex() > 0
                && mouseX >= panelLeft + 14
                && mouseX < panelLeft + panelWidth - 14
                && mouseY >= treeTop()
                && mouseY < treeBottom()
                && scrollY != 0.0) {
            int direction = scrollY > 0.0 ? -1 : 1;
            scrollIndex = Math.max(0, Math.min(
                    maxScrollIndex(),
                    scrollIndex + direction
            ));
            return true;
        }

        return super.mouseScrolled(
                mouseX,
                mouseY,
                scrollX,
                scrollY
        );
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

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
