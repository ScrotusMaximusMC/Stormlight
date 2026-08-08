package com.scrotey.stormlight.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class SpherePouchScreen
        extends AbstractContainerScreen<SpherePouchMenu> {

    private static final int PANEL_COLOUR =
            0xFF172630;

    private static final int PANEL_BORDER =
            0xFF5BD8F2;

    private static final int SLOT_BORDER =
            0xFF08141C;

    private static final int SLOT_BACKGROUND =
            0xFF365365;

    public SpherePouchScreen(
            SpherePouchMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(
                menu,
                inventory,
                title,
                176,
                190
        );

        titleLabelX = 8;
        titleLabelY = 6;

        inventoryLabelX = 8;
        inventoryLabelY = 96;
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        super.extractBackground(
                graphics,
                mouseX,
                mouseY,
                delta
        );

        graphics.fill(
                leftPos,
                topPos,
                leftPos + imageWidth,
                topPos + imageHeight,
                PANEL_BORDER
        );

        graphics.fill(
                leftPos + 2,
                topPos + 2,
                leftPos + imageWidth - 2,
                topPos + imageHeight - 2,
                PANEL_COLOUR
        );

        drawSlotGrid(
                graphics,
                53,
                18,
                4,
                4
        );

        drawSlotGrid(
                graphics,
                8,
                108,
                9,
                3
        );

        drawSlotGrid(
                graphics,
                8,
                166,
                9,
                1
        );
    }

    private void drawSlotGrid(
            GuiGraphicsExtractor graphics,
            int startX,
            int startY,
            int columns,
            int rows
    ) {
        for (int row = 0; row < rows; row++) {
            for (int column = 0;
                 column < columns;
                 column++) {

                int x = leftPos
                        + startX
                        + column * 18;

                int y = topPos
                        + startY
                        + row * 18;

                graphics.fill(
                        x - 1,
                        y - 1,
                        x + 17,
                        y + 17,
                        SLOT_BORDER
                );

                graphics.fill(
                        x,
                        y,
                        x + 16,
                        y + 16,
                        SLOT_BACKGROUND
                );
            }
        }
    }
}