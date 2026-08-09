package com.scrotey.stormlight.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Shared Stormlight visual language for sphere storage screens.
 * Everything here is client-side decoration only.
 */
public final class StormlightGuiStyle {
    private static final int OUTER_GLOW = 0xB06FF5FF;
    private static final int EDGE_BRIGHT = 0xFFB8F8FF;
    private static final int EDGE_MID = 0xFF3EBCE8;
    private static final int EDGE_DARK = 0xFF061829;
    private static final int PANEL = 0xFF0A2B48;
    private static final int PANEL_INNER = 0xFF0D3858;
    private static final int SLOT_RIM = 0xFF69E9FF;
    private static final int SLOT_SHADOW = 0xFF020A13;
    private static final int SLOT_WELL = 0xFF123E5C;
    private static final int TEXT = 0xFFFFFFFF;

    private StormlightGuiStyle() {
    }

    public static void drawSpherePanel(
            GuiGraphicsExtractor graphics,
            Font font,
            Component title,
            int left,
            int top,
            int width,
            int height,
            int slotStartX,
            int slotStartY,
            int columns,
            int rows
    ) {
        int right = left + width;
        int bottom = top + height;

        // A faint, slowly breathing aura outside the hard frame.
        int pulse = (int) ((System.currentTimeMillis() / 350L) % 4L);
        int pulseColour = pulse < 2 ? OUTER_GLOW : 0x805FE5FF;

        graphics.fill(left - 1, top + 3, right + 1, bottom - 3, pulseColour);
        graphics.fill(left + 3, top - 1, right - 3, bottom + 1, pulseColour);

        // Blue-steel frame and deep infused interior.
        graphics.fill(left, top, right, bottom, EDGE_DARK);
        graphics.fill(left + 1, top + 1, right - 1, bottom - 1, EDGE_BRIGHT);
        graphics.fill(left + 2, top + 2, right - 2, bottom - 2, EDGE_MID);
        graphics.fill(left + 3, top + 3, right - 3, bottom - 3, PANEL);
        graphics.fill(left + 5, top + 17, right - 5, bottom - 5, PANEL_INNER);

        drawCorners(graphics, left, top, right, bottom);

        if (width >= 120) {
            drawHeaderGlyph(graphics, left + width / 2, top + 9);
        } else {
            graphics.fill(
                    left + 6,
                    top + 15,
                    right - 6,
                    top + 16,
                    0xE070EEFF
            );
        }

        graphics.text(
                font,
                title,
                left + 6,
                top + 5,
                TEXT,
                false
        );

        drawSlotGrid(
                graphics,
                slotStartX,
                slotStartY,
                columns,
                rows
        );
    }

    private static void drawSlotGrid(
            GuiGraphicsExtractor graphics,
            int startX,
            int startY,
            int columns,
            int rows
    ) {
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int x = startX + column * 18;
                int y = startY + row * 18;

                graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_SHADOW);
                graphics.fill(x, y, x + 16, y + 16, SLOT_RIM);
                graphics.fill(x + 1, y + 1, x + 16, y + 16, EDGE_DARK);
                graphics.fill(x + 2, y + 2, x + 15, y + 15, SLOT_WELL);

                // Tiny reflected Stormlight in the upper-left of each well.
                graphics.fill(x + 2, y + 2, x + 9, y + 3, 0xD070EEFF);
                graphics.fill(x + 2, y + 3, x + 3, y + 9, 0xA050D8F5);
            }
        }
    }

    private static void drawHeaderGlyph(
            GuiGraphicsExtractor graphics,
            int centreX,
            int centreY
    ) {
        // Compact diamond glyph; intentionally kept clear of slot contents.
        graphics.fill(centreX, centreY - 3, centreX + 1, centreY + 4, EDGE_BRIGHT);
        graphics.fill(centreX - 3, centreY, centreX + 4, centreY + 1, EDGE_BRIGHT);
        graphics.fill(centreX - 2, centreY - 2, centreX - 1, centreY + 3, EDGE_MID);
        graphics.fill(centreX + 2, centreY - 2, centreX + 3, centreY + 3, EDGE_MID);
    }

    private static void drawCorners(
            GuiGraphicsExtractor graphics,
            int left,
            int top,
            int right,
            int bottom
    ) {
        graphics.fill(left + 3, top + 3, left + 8, top + 4, EDGE_BRIGHT);
        graphics.fill(left + 3, top + 3, left + 4, top + 8, EDGE_BRIGHT);
        graphics.fill(right - 8, top + 3, right - 3, top + 4, EDGE_BRIGHT);
        graphics.fill(right - 4, top + 3, right - 3, top + 8, EDGE_BRIGHT);
        graphics.fill(left + 3, bottom - 4, left + 8, bottom - 3, EDGE_BRIGHT);
        graphics.fill(left + 3, bottom - 8, left + 4, bottom - 3, EDGE_BRIGHT);
        graphics.fill(right - 8, bottom - 4, right - 3, bottom - 3, EDGE_BRIGHT);
        graphics.fill(right - 4, bottom - 8, right - 3, bottom - 3, EDGE_BRIGHT);
    }
}
