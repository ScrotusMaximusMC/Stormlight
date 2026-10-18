package com.scrotey.stormlight.progression;

public record SprenBondLevel(
        int level,
        int requiredRadiantLevel,
        String titleTranslationKey,
        String wordsTranslationKey
) {
}
