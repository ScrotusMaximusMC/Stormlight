package com.scrotey.stormlight.client.progression;

import com.scrotey.stormlight.network.TakeLecternBookPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public final class MysteriousBookScreen extends Screen {
    private static final int BACKDROP = 0xD80A0806;
    private static final int LEATHER = 0xFF4A3325;
    private static final int PAGE_EDGE = 0xFF8A673D;
    private static final int PARCHMENT = 0xFFE0C58B;
    private static final int INK = 0xFF35251A;
    private static final int FADED_INK = 0xFF765F46;
    private static final int REVEALED = 0xFF245F78;

    private static final String[] LEFT_PAGE = {
            "⟟ ϟ ⋔ ᚷ ⌁ ᚱ Ϟ 𐍈 ⋇ ⟁",
            "ᛉ ⋰ Ϡ ⌇ ⟡ ᚾ ϟ ⋔ ᛝ",
            "⋇ ᚱ ᚨ Ϟ  highstorms  ⌁",
            "ϟ ⟟ ⋔ ᛉ ⟁ ᚷ ⋰ Ϡ ⌇",
            "ᚾ ⋇ ⟡ Ϟ ⌁ ᚱ ᛝ ϟ",
            "⋔ ᚨ ⌇ ⟟ ⋰ ᚷ Ϡ ⟁",
            "Ϟ ᛉ ⋇ ᚾ ⌁ ᚱ ⟡ ⋔",
            "⟁ ϟ ⋰ ᚨ ᛝ ⌇ Ϡ ᚷ"
    };

    private static final String[] RIGHT_PAGE = {
            "ᚷ ⌁ Ϟ ⋇ ᚱ ⟟ ᛉ ⋔",
            "⋰ Ϡ ⟁ ᚾ ⌇ ᛝ ϟ",
            "⟡ ᚨ ⋔ Ϟ ⌁  radiant",
            "ᚱ ᛉ ⋇ Ϡ ⟟ ⌇ ⟁",
            "⋔ ᚷ ϟ ᛝ ⋰ ᚾ ⟡",
            "Ϟ ⌁ ᚨ ᚱ ⋇ Ϡ ᛉ",
            "⟁ ⟟ ⌇ ⋔ ᚷ ϟ ⋰",
            "ᚾ ᛝ ⟡ Ϟ ⌁ ᚱ ⋇"
    };

    private final BlockPos lecternPos;

    private int bookLeft;
    private int bookTop;
    private int bookWidth;
    private int bookHeight;

    public MysteriousBookScreen(BlockPos lecternPos) {
        super(Component.translatable(
                "screen.stormlight.mysterious_book.title"
        ));

        this.lecternPos = lecternPos;
    }

    @Override
    protected void init() {
        bookWidth = Math.min(520, width - 30);
        bookHeight = Math.min(300, height - 35);
        bookLeft = (width - bookWidth) / 2;
        bookTop = (height - bookHeight) / 2;

        int buttonWidth = 90;
        int buttonGap = 8;
        int totalWidth = buttonWidth * 2 + buttonGap;
        int startX = width / 2 - totalWidth / 2;
        int buttonY = bookTop + bookHeight - 31;

        addRenderableWidget(
                Button.builder(
                        Component.literal("Take Book"),
                        button -> takeBook()
                ).bounds(
                        startX,
                        buttonY,
                        buttonWidth,
                        20
                ).build()
        );

        addRenderableWidget(
                Button.builder(
                        Component.translatable("gui.done"),
                        button -> onClose()
                ).bounds(
                        startX + buttonWidth + buttonGap,
                        buttonY,
                        buttonWidth,
                        20
                ).build()
        );
    }

    private void takeBook() {
        ClientPlayNetworking.send(
                new TakeLecternBookPayload(
                        lecternPos
                )
        );

        onClose();
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        graphics.fill(
                0,
                0,
                width,
                height,
                BACKDROP
        );

        graphics.fill(
                bookLeft - 5,
                bookTop - 5,
                bookLeft + bookWidth + 5,
                bookTop + bookHeight + 5,
                LEATHER
        );

        graphics.fill(
                bookLeft,
                bookTop,
                bookLeft + bookWidth,
                bookTop + bookHeight,
                PAGE_EDGE
        );

        int centre = bookLeft + bookWidth / 2;

        graphics.fill(
                bookLeft + 5,
                bookTop + 5,
                centre - 2,
                bookTop + bookHeight - 5,
                PARCHMENT
        );

        graphics.fill(
                centre + 2,
                bookTop + 5,
                bookLeft + bookWidth - 5,
                bookTop + bookHeight - 5,
                PARCHMENT
        );

        graphics.fill(
                centre - 2,
                bookTop + 5,
                centre + 2,
                bookTop + bookHeight - 5,
                0x55382418
        );

        drawCentered(
                graphics,
                Component.translatable(
                        "screen.stormlight.mysterious_book.title"
                ),
                width / 2,
                bookTop + 16,
                INK
        );

        drawPage(
                graphics,
                LEFT_PAGE,
                bookLeft + 22,
                bookTop + 46
        );

        drawPage(
                graphics,
                RIGHT_PAGE,
                centre + 20,
                bookTop + 46
        );

        drawCentered(
                graphics,
                Component.translatable(
                        "screen.stormlight.mysterious_book.hint"
                ),
                width / 2,
                bookTop + bookHeight - 51,
                FADED_INK
        );

        super.extractRenderState(
                graphics,
                mouseX,
                mouseY,
                delta
        );
    }

    private void drawPage(
            GuiGraphicsExtractor graphics,
            String[] lines,
            int x,
            int y
    ) {
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            int colour =
                    line.contains("highstorms")
                            || line.contains("radiant")
                            ? REVEALED
                            : INK;

            graphics.text(
                    font,
                    Component.literal(line),
                    x,
                    y + i * 19,
                    colour,
                    false
            );
        }
    }
    private void drawCentered(
            GuiGraphicsExtractor graphics,
            Component text,
            int x,
            int y,
            int colour
    ) {
        graphics.text(
                font,
                text,
                x - font.width(text) / 2,
                y,
                colour,
                false
        );
    }
}