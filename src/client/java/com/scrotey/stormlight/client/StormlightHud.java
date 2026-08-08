package com.scrotey.stormlight.client;

import com.scrotey.stormlight.breathing.AbilityId;
import com.scrotey.stormlight.network.StormlightStatusPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.EnumSet;

public final class StormlightHud {
    private static final int BAR_WIDTH = 120;
    private static final int BAR_HEIGHT = 9;
    private static final int INNER_WIDTH = BAR_WIDTH - 4;

    private static final int BORDER_COLOUR = 0xDD07121F;
    private static final int EMPTY_COLOUR = 0xCC102638;
    private static final int TEXT_COLOUR = 0xFFFFFFFF;

    private static int charge;
    private static int capacity;
    private static EnumSet<AbilityId> activeAbilities = EnumSet.noneOf(AbilityId.class);

    private StormlightHud() {
    }

    public static void update(StormlightStatusPayload payload) {
        charge = Math.max(0, payload.charge());
        capacity = Math.max(0, payload.capacity());
        activeAbilities = decode(payload.activeAbilitiesMask());
    }

    private static EnumSet<AbilityId> decode(int mask) {
        EnumSet<AbilityId> abilities = EnumSet.noneOf(AbilityId.class);

        for (AbilityId id : AbilityId.values()) {
            if ((mask & (1 << id.ordinal())) != 0) {
                abilities.add(id);
            }
        }

        return abilities;
    }

    public static void render(
            GuiGraphicsExtractor graphics,
            net.minecraft.client.DeltaTracker deltaTracker
    ) {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null
                || capacity <= 0) {
            return;
        }

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        int x = (screenWidth - BAR_WIDTH) / 2;
        int y = screenHeight - 55;

        double fullness = Math.min(
                1.0,
                charge / (double) capacity
        );

        int filledWidth = (int) Math.floor(INNER_WIDTH * fullness);

        if (charge > 0 && filledWidth == 0) {
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
                x + 2,
                y + 2,
                x + 2 + INNER_WIDTH,
                y + BAR_HEIGHT - 2,
                EMPTY_COLOUR
        );

        if (filledWidth > 0) {
            graphics.fill(
                    x + 2,
                    y + 2,
                    x + 2 + filledWidth,
                    y + BAR_HEIGHT - 2,
                    colourForCapacity(capacity)
            );
        }

        Component label = Component.literal(
                statusLabel() + " — " + charge + " / " + capacity
        );

        int textX = (screenWidth - client.font.width(label)) / 2;

        graphics.text(
                client.font,
                label,
                textX,
                y - 11,
                TEXT_COLOUR,
                true
        );
    }

    private static String statusLabel() {
        boolean surging = activeAbilities.contains(AbilityId.STRENGTH_SURGE);
        boolean healing = activeAbilities.contains(AbilityId.EMERGENCY_HEAL);

        if (surging && healing) {
            return "Surging + Healing";
        }

        if (healing) {
            return "Healing";
        }

        if (surging) {
            return "Surging";
        }

        return "Stormlight";
    }

    private static int colourForCapacity(int totalCapacity) {
        if (totalCapacity < 100) {
            return 0xFF8FE7FF; // Pale blue
        }

        if (totalCapacity < 400) {
            return 0xFF24D9FF; // Cyan
        }

        if (totalCapacity < 1000) {
            return 0xFF2F8FFF; // Bright azure
        }

        if (totalCapacity < 2000) {
            return 0xFFC8F7FF; // Blue-white
        }

        return 0xFFE8D8FF; // Radiant violet-white
    }
}
