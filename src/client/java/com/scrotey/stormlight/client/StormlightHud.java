package com.scrotey.stormlight.client;

import com.scrotey.stormlight.client.lashing.ClientLashingState;
import com.scrotey.stormlight.network.StormlightStatusPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class StormlightHud {
    private static final int BAR_WIDTH = 81;
    private static final int BAR_HEIGHT = 5;
    private static final int INNER_WIDTH = BAR_WIDTH - 2;

    private static final int BORDER_COLOUR = 0xDD07121F;
    private static final int EMPTY_COLOUR = 0xCC102638;
    private static final int TEXT_COLOUR = 0xFFFFFFFF;

    private static int reserve;
    private static int capacity;

    private StormlightHud() {
    }

    public static void update(StormlightStatusPayload payload) {
        reserve = Math.max(0, payload.reserve());
        capacity = Math.max(0, payload.capacity());
    }

    public static void render(
            GuiGraphicsExtractor graphics,
            net.minecraft.client.DeltaTracker deltaTracker
    ) {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null
                || capacity <= 0
                || reserve <= 0) {
            return;
        }

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        // Aligns with the hunger bar, opposite the armour display.
        int x = screenWidth / 2 + 10;
        int y = screenHeight - 49;

        double fullness = Math.min(
                1.0,
                reserve / (double) capacity
        );

        int filledWidth = (int) Math.floor(INNER_WIDTH * fullness);

        if (reserve > 0 && filledWidth == 0) {
            filledWidth = 1;
        }

        graphics.fill(
                x,
                y,
                x + BAR_WIDTH,
                y + BAR_HEIGHT,
                BORDER_COLOUR
        );

        graphics.fill(
                x + 1,
                y + 1,
                x + 1 + INNER_WIDTH,
                y + BAR_HEIGHT - 1,
                EMPTY_COLOUR
        );

        if (filledWidth > 0) {
            graphics.fill(
                    x + 1,
                    y + 1,
                    x + 1 + filledWidth,
                    y + BAR_HEIGHT - 1,
                    0xFFC8F7FF
            );
        }

        Component label = Component.literal(
                "Stormlight " + reserve + " / " + capacity
        );

        if (ClientLashingState.isActive()
                || ClientLashingState.isStabilising()) {
            Component lashingLabel = Component.translatable(
                    ClientLashingState.isStabilising()
                            ? "hud.stormlight.stabilising"
                            : "hud.stormlight.lashing.active"
            );
            int lashingX = x
                    + (BAR_WIDTH - client.font.width(lashingLabel)) / 2;

            graphics.text(
                    client.font,
                    lashingLabel,
                    lashingX,
                    y - 21,
                    0xFF8DEBFF,
                    true
            );
        }

        int textX = x + (BAR_WIDTH - client.font.width(label)) / 2;

        graphics.text(
                client.font,
                label,
                textX,
                y - 11,
                TEXT_COLOUR,
                true
        );
    }

}
