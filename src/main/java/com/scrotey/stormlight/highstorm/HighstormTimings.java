package com.scrotey.stormlight.highstorm;

public final class HighstormTimings {
    public static final int TICKS_PER_SECOND = 20;

    /*
     * The uneasy lull before the stormwall is visible: the wind drops,
     * the sky hazes over and a dark line forms on the eastern horizon.
     */
    public static final int GATHERING_TICKS =
            150 * TICKS_PER_SECOND;

    public static final int APPROACHING_TICKS =
            3 * 60 * TICKS_PER_SECOND;

    public static final int HIGHSTORM_TICKS =
            2 * 60 * TICKS_PER_SECOND;

    public static final int PASSING_TICKS =
            1 * 60 * TICKS_PER_SECOND;

    /*
     * The wall's journey is measured by "wall progress", from 0 (at the
     * horizon) to 1 (its western face far beyond the player). Its leading
     * edge crosses overhead at this value, mirroring StormfrontCloudRenderer's
     * geometry (see getApproachLeadingEdgeDistance there), so keep the two in
     * step if the wall's travel or depth changes.
     *
     * The Approach covers wall progress 0 up to arrival, so the Highstorm
     * begins as the first cloud reaches the player. The rest of the journey,
     * the wall rolling on over and past them, takes up the opening
     * WALL_PASS_TICKS of the Highstorm.
     */
    public static final float STORMWALL_ARRIVAL_PROGRESS = 0.54F;

    public static final int WALL_PASS_TICKS =
            20 * TICKS_PER_SECOND;

    /*
     * Vanilla rain takes roughly five seconds to reach full strength, so it
     * is started a little early to be falling hard as the wall passes over.
     */
    private static final int RAIN_LEAD_TICKS = 40;

    public static final int APPROACH_RAIN_START_TICK =
            APPROACHING_TICKS - RAIN_LEAD_TICKS;

    /*
     * Wall progress through the opening of the Highstorm. A cubic curve
     * starts at the Approach's pace, so the wall doesn't lurch at the
     * hand-over, then sweeps the rest of the way and settles.
     */
    public static float wallPassProgress(
            float passProgress
    ) {
        float u = Math.max(0.0F, Math.min(1.0F, passProgress));

        float arrivalSlope =
                STORMWALL_ARRIVAL_PROGRESS
                        * WALL_PASS_TICKS
                        / (float) APPROACHING_TICKS;

        return STORMWALL_ARRIVAL_PROGRESS
                + arrivalSlope * (u * u * u - 2.0F * u * u + u)
                + (1.0F - STORMWALL_ARRIVAL_PROGRESS)
                * (3.0F * u * u - 2.0F * u * u * u);
    }

    private HighstormTimings() {
    }
}
