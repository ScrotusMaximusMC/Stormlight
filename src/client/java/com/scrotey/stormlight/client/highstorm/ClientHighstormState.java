package com.scrotey.stormlight.client.highstorm;

import com.scrotey.stormlight.highstorm.HighstormPhase;
import com.scrotey.stormlight.highstorm.HighstormTimings;
import com.scrotey.stormlight.network.HighstormVisualPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class ClientHighstormState {
    private static int phaseOrdinal =
            HighstormPhase.CALM.ordinal();

    private static int syncedTicksRemaining;
    private static int ticksSinceSync;

    private ClientHighstormState() {
    }

    public static void update(
            HighstormVisualPayload payload
    ) {
        phaseOrdinal = payload.phaseOrdinal();

        syncedTicksRemaining =
                Math.max(
                        0,
                        payload.ticksRemaining()
                );

        ticksSinceSync = 0;
    }

    public static void tick(
            Minecraft client
    ) {
        if (client.level == null
                || client.player == null) {
            reset();
            return;
        }

        ticksSinceSync++;
    }

    public static boolean isApproaching() {
        return phaseOrdinal
                == HighstormPhase.APPROACHING.ordinal();
    }

    public static boolean isHighstorm() {
        return phaseOrdinal
                == HighstormPhase.HIGHSTORM.ordinal();
    }

    public static boolean isPassing() {
        return phaseOrdinal
                == HighstormPhase.PASSING.ordinal();
    }

    public static boolean isStormfrontVisible() {
        return isApproaching()
                || isHighstorm()
                || isPassing();
    }

    public static float getApproachProgress() {
        if (!isApproaching()) {
            return 0.0F;
        }

        return calculateProgress(
                HighstormTimings.APPROACHING_TICKS
        );
    }

    public static float getPassingProgress() {
        if (!isPassing()) {
            return 0.0F;
        }

        return calculateProgress(
                HighstormTimings.PASSING_TICKS
        );
    }

    private static float calculateProgress(
            int phaseDuration
    ) {
        int estimatedRemaining =
                Math.max(
                        0,
                        syncedTicksRemaining
                                - ticksSinceSync
                );

        return Mth.clamp(
                1.0F
                        - estimatedRemaining
                        / (float) phaseDuration,
                0.0F,
                1.0F
        );
    }

    private static void reset() {
        phaseOrdinal =
                HighstormPhase.CALM.ordinal();

        syncedTicksRemaining = 0;
        ticksSinceSync = 0;
    }
}