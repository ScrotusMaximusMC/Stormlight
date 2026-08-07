package com.scrotey.stormlight.highstorm;

public enum HighstormPhase {
    CALM("Calm"),
    APPROACHING("Approaching"),
    HIGHSTORM("Highstorm"),
    PASSING("Passing");

    private final String displayName;

    HighstormPhase(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
