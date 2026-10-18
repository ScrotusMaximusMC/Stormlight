package com.scrotey.stormlight.progression;

import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class SprenBondProgression {
    public static final int MAX_LEVEL = 5;

    private static final List<SprenBondLevel> LEVELS = List.of(
            level(1, 1, "first_ideal"),
            level(2, 3, "second_ideal"),
            level(3, 5, "third_ideal"),
            level(4, 7, "fourth_ideal"),
            level(5, 9, "fifth_ideal")
    );

    private SprenBondProgression() {
    }

    public static List<SprenBondLevel> levels() {
        return LEVELS;
    }

    public static int getLevel(Player player) {
        return getLevelForRadiantLevel(
                RadiantProgression.getLevel(player)
        );
    }

    public static int getLevelForRadiantLevel(int radiantLevel) {
        int bondLevel = 0;

        for (SprenBondLevel level : LEVELS) {
            if (radiantLevel < level.requiredRadiantLevel()) {
                break;
            }

            bondLevel = level.level();
        }

        return bondLevel;
    }

    private static SprenBondLevel level(
            int level,
            int requiredRadiantLevel,
            String translationSuffix
    ) {
        String prefix = "progression.stormlight.spren_bond."
                + translationSuffix;

        return new SprenBondLevel(
                level,
                requiredRadiantLevel,
                prefix + ".title",
                prefix + ".words"
        );
    }
}
