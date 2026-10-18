package com.scrotey.stormlight.client.spren;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;

/** Brief blue edge flash when a bonded spren absorbs an attack. */
public final class ClientSprenShield {
    private static final int FLASH_TICKS = 12;
    private static ClientLevel activeLevel;
    private static int ticksRemaining;

    private ClientSprenShield() {
    }

    public static void flash(Minecraft client) {
        activeLevel = client.level;
        ticksRemaining = FLASH_TICKS;
    }

    public static void tick(Minecraft client) {
        if (client.level == null || client.level != activeLevel) {
            ticksRemaining = 0;
            activeLevel = null;
        } else if (ticksRemaining > 0) {
            ticksRemaining--;
        }
    }

    public static void render(GuiGraphicsExtractor graphics,
            net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (ticksRemaining <= 0 || client.player == null) {
            return;
        }

        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();
        int thickness = Math.min(48, Math.max(12,
                Math.min(width, height) / 9));
        float fade = Math.min(1.0F, ticksRemaining / 7.0F);
        int bands = 8;
        for (int band = 0; band < bands; band++) {
            int outer = thickness * band / bands;
            int inner = thickness * (band + 1) / bands;
            int alpha = (int) ((0.40F - band * 0.042F)
                    * fade * 255);
            int color = (alpha << 24) | 0x60CFFF;
            graphics.fill(outer, outer, width - outer, inner, color);
            graphics.fill(outer, height - inner,
                    width - outer, height - outer, color);
            graphics.fill(outer, inner, inner, height - inner, color);
            graphics.fill(width - inner, inner,
                    width - outer, height - inner, color);
        }
    }
}
