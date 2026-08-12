package com.scrotey.stormlight.progression;

/**
 * The gameplay bonuses unlocked at one level of a Radiant order.
 * Effect amplifiers use Minecraft's zero-based convention: 0 is level I.
 */
public record RadiantLevel(
        int level,
        int stormlightCapacity,
        int speedAmplifier,
        int strengthAmplifier,
        String unlockTranslationKey,
        String descriptionTranslationKey
) {
}
