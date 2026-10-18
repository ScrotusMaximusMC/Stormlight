package com.scrotey.stormlight.client.highstorm;

import com.scrotey.stormlight.highstorm.HighstormPhase;
import com.scrotey.stormlight.highstorm.HighstormTimings;
import com.scrotey.stormlight.network.HighstormVisualPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class ClientHighstormState {
    /*
     * Full thunder darkness is reached after 70% of the approach.
     * During departure, darkness remains full for the mirrored
     * opening 30% before daylight begins returning.
     */
    private static final float DARKENING_DURATION = 0.70F;
    private static final float PASSING_DARKNESS_HOLD =
            1.0F - DARKENING_DURATION;

    /*
     * The Gathering lull greys the sky slightly before the stormwall is
     * close enough to darken it properly.
     */
    private static final float GATHERING_HAZE = 0.20F;

    /*
     * How built-up the stormwall is. It rises over the horizon as a low
     * wall during Gathering and finishes growing taller partway through
     * the approach, before its leading edge reaches the player.
     */
    private static final float GATHERING_START_FORMATION = 0.0F;
    private static final float GATHERING_END_FORMATION = 0.30F;
    private static final float FULL_FORMATION_APPROACH_PROGRESS = 0.50F;

    /*
     * Ambient and animal sounds fall quiet early in the lull and stay
     * subdued while the wall approaches.
     */
    private static final float LULL_AMBIENT_VOLUME = 0.30F;
    private static final float LULL_SETTLE_PROGRESS = 0.40F;

    /*
     * Storm-tinted fog. The spread lets the tint reach round to the north
     * and south, where the wall still lines the horizon.
     */
    private static final float STORM_FOG_FACING_SPREAD = 0.45F;
    private static final float STORM_FOG_GATHERING_FULL = 0.80F;
    private static final float STORM_FOG_PASSING_SURROUNDED = 0.45F;

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

    public static boolean isGathering() {
        return phaseOrdinal
                == HighstormPhase.GATHERING.ordinal();
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
        return isGathering()
                || isApproaching()
                || isHighstorm()
                || isPassing();
    }

    /*
     * Supplies Minecraft's rendered thunder intensity:
     *
     * 0.0 = ordinary daylight
     * 1.0 = full Highstorm darkness
     */
    public static float getStormDarkness() {
        if (isGathering()) {
            return GATHERING_HAZE
                    * (float) Mth.smoothstep(
                    getGatheringProgress()
            );
        }

        if (isApproaching() || isHighstorm()) {
            float progress =
                    Mth.clamp(
                            getWallProgress()
                                    / DARKENING_DURATION,
                            0.0F,
                            1.0F
                    );

            return Mth.lerp(
                    (float) Mth.smoothstep(progress),
                    GATHERING_HAZE,
                    1.0F
            );
        }

        if (isPassing()) {
            float progress =
                    Mth.clamp(
                            (
                                    getPassingProgress()
                                            - PASSING_DARKNESS_HOLD
                            ) / DARKENING_DURATION,
                            0.0F,
                            1.0F
                    );

            return (float) (1.0F
                                - Mth.smoothstep(progress));
        }

        return 0.0F;
    }

    /*
     * 0.0 = barely visible line on the horizon
     * 1.0 = complete stormwall
     */
    public static float getStormfrontFormation() {
        if (isGathering()) {
            return Mth.lerp(
                    (float) Mth.smoothstep(
                            getGatheringProgress()
                    ),
                    GATHERING_START_FORMATION,
                    GATHERING_END_FORMATION
            );
        }

        if (isApproaching()) {
            float progress =
                    Mth.clamp(
                            getApproachProgress()
                                    / FULL_FORMATION_APPROACH_PROGRESS,
                            0.0F,
                            1.0F
                    );

            return Mth.lerp(
                    (float) Mth.smoothstep(progress),
                    GATHERING_END_FORMATION,
                    1.0F
            );
        }

        return 1.0F;
    }

    /*
     * Fades vanilla's clouds away during the lull, leaving the clear sky
     * that comes before a stormwall. They are hidden entirely once the
     * approach begins.
     */
    public static float getVanillaCloudOpacity() {
        if (isGathering()) {
            return 1.0F
                    - (float) Mth.smoothstep(
                    getGatheringProgress()
            );
        }

        return isStormfrontVisible()
                ? 0.0F
                : 1.0F;
    }

    /*
     * How strongly the fog should take on the storm's colour, given how far
     * the camera faces east (+1) or west (-1). Fog has a single colour per
     * frame, so this follows where the player is looking, much as vanilla
     * warms the fog when looking towards a sunset. It stops distant terrain
     * fading into a pale band in front of the dark stormwall.
     */
    public static float getStormFogStrength(
            float eastFacing
    ) {
        float towardsEast =
                (float) Mth.smoothstep(
                        Mth.clamp(
                                (eastFacing + STORM_FOG_FACING_SPREAD)
                                        / (1.0F + STORM_FOG_FACING_SPREAD),
                                0.0F,
                                1.0F
                        )
                );

        float towardsWest =
                (float) Mth.smoothstep(
                        Mth.clamp(
                                (-eastFacing + STORM_FOG_FACING_SPREAD)
                                        / (1.0F + STORM_FOG_FACING_SPREAD),
                                0.0F,
                                1.0F
                        )
                );

        if (isGathering()) {
            return towardsEast
                    * (float) Mth.smoothstep(
                    getGatheringProgress()
                            / STORM_FOG_GATHERING_FULL
            );
        }

        if (isApproaching()) {
            return towardsEast;
        }

        if (isHighstorm()) {
            // Once the wall has rolled overhead, the storm is all around.
            return Mth.lerp(
                    (float) Mth.smoothstep(getWallPassProgress()),
                    towardsEast,
                    1.0F
            );
        }

        if (isPassing()) {
            float progress = getPassingProgress();

            float surrounding =
                    1.0F
                            - (float) Mth.smoothstep(
                            progress / STORM_FOG_PASSING_SURROUNDED
                    );

            return Mth.lerp(surrounding, towardsWest, 1.0F)
                    * (1.0F - (float) Mth.smoothstep(progress));
        }

        return 0.0F;
    }

    public static float getAmbientVolumeMultiplier() {
        if (isGathering()) {
            float settle =
                    Mth.clamp(
                            getGatheringProgress()
                                    / LULL_SETTLE_PROGRESS,
                            0.0F,
                            1.0F
                    );

            return Mth.lerp(
                    (float) Mth.smoothstep(settle),
                    1.0F,
                    LULL_AMBIENT_VOLUME
            );
        }

        if (isApproaching()) {
            return LULL_AMBIENT_VOLUME;
        }

        return 1.0F;
    }

    public static float getGatheringProgress() {
        if (!isGathering()) {
            return 0.0F;
        }

        return calculateProgress(
                HighstormTimings.GATHERING_TICKS
        );
    }

    public static float getApproachProgress() {
        if (!isApproaching()) {
            return 0.0F;
        }

        return calculateProgress(
                HighstormTimings.APPROACHING_TICKS
        );
    }

    /*
     * How far through the wall's journey it is; see HighstormTimings.
     * The Approach brings its leading edge overhead, and the opening of
     * the Highstorm carries it on past the player.
     */
    public static float getWallProgress() {
        if (isApproaching()) {
            return HighstormTimings.STORMWALL_ARRIVAL_PROGRESS
                    * getApproachProgress();
        }

        if (isHighstorm()) {
            return HighstormTimings.wallPassProgress(
                    getWallPassProgress()
            );
        }

        return 0.0F;
    }

    /*
     * 0 as the Highstorm begins, 1 once the wall has rolled on past.
     */
    public static float getWallPassProgress() {
        if (!isHighstorm()) {
            return 0.0F;
        }

        int elapsed =
                HighstormTimings.HIGHSTORM_TICKS
                        - Math.max(
                        0,
                        syncedTicksRemaining - ticksSinceSync
                );

        return Mth.clamp(
                elapsed
                        / (float) HighstormTimings.WALL_PASS_TICKS,
                0.0F,
                1.0F
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