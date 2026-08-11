package com.scrotey.stormlight.progression;

import java.util.List;
import java.util.Optional;

public record RadiantOrder(
        String id,
        int networkId,
        String translationKey,
        int colour,
        List<RadiantLevel> levels
) {
    public Optional<RadiantLevel> level(int requestedLevel) {
        return levels.stream()
                .filter(level -> level.level() == requestedLevel)
                .findFirst();
    }
}
