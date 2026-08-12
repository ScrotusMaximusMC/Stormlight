package com.scrotey.stormlight.client;

public final class StormlightFovFeedback {
    private static final float INHALE_FOV_OFFSET = 7.0F;
    private static final float EXHALE_FOV_OFFSET = -4.0F;
    private static final double RESPONSE_PER_SECOND = 9.0;

    private static float targetOffset;
    private static float currentOffset;
    private static long lastUpdateNanos = System.nanoTime();

    private StormlightFovFeedback() {
    }

    public static void setBreathingState(
            boolean breathingIn,
            boolean breathingOut
    ) {
        if (breathingOut) {
            targetOffset = EXHALE_FOV_OFFSET;
        } else if (breathingIn) {
            targetOffset = INHALE_FOV_OFFSET;
        } else {
            targetOffset = 0.0F;
        }
    }

    public static float getFovOffset() {
        long now = System.nanoTime();
        double elapsedSeconds = Math.min(
                0.1,
                Math.max(0.0, (now - lastUpdateNanos) / 1_000_000_000.0)
        );
        lastUpdateNanos = now;

        double blend = 1.0
                - Math.exp(-RESPONSE_PER_SECOND * elapsedSeconds);
        currentOffset += (targetOffset - currentOffset) * (float) blend;

        if (Math.abs(targetOffset - currentOffset) < 0.01F) {
            currentOffset = targetOffset;
        }

        return currentOffset;
    }
}
