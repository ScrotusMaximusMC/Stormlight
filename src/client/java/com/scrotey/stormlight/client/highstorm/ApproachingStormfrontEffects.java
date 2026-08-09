package com.scrotey.stormlight.client.highstorm;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

public final class ApproachingStormfrontEffects {
    /*
     * Minecraft east is positive X. The front therefore
     * appears at +X and advances towards the player.
     */
    private static final double START_DISTANCE = 150.0;
    private static final double END_DISTANCE = 16.0;

    private static final double WALL_HALF_WIDTH = 105.0;
    private static final double WALL_THICKNESS = 15.0;
    private static final double WALL_HEIGHT = 62.0;

    /*
     * Particles are emitted every second client tick.
     * These numbers are intentionally easy to tune later.
     */
    private static final int MIN_CLOUD_PARTICLES = 24;
    private static final int MAX_CLOUD_PARTICLES = 78;

    private static final RandomSource RANDOM =
            RandomSource.create();

    private ApproachingStormfrontEffects() {
    }

    public static void tick(
            Minecraft client
    ) {
        if (client.level == null
                || client.player == null
                || !ClientHighstormState.isApproaching()
                || !client.level.dimension()
                .equals(Level.OVERWORLD)) {
            return;
        }

        /*
         * Half the spawn frequency without making the
         * particles themselves move less smoothly.
         */
        if ((client.level.getGameTime() & 1L) != 0L) {
            return;
        }

        float progress =
                ClientHighstormState.getApproachProgress();

        /*
         * Smoothstep prevents the wall suddenly starting
         * or stopping when the phase changes.
         */
        float smoothProgress =
                progress
                        * progress
                        * (3.0F - 2.0F * progress);

        double distance =
                START_DISTANCE
                        + (END_DISTANCE - START_DISTANCE)
                        * smoothProgress;

        int cloudCount =
                Mth.floor(
                        MIN_CLOUD_PARTICLES
                                + (MAX_CLOUD_PARTICLES
                                - MIN_CLOUD_PARTICLES)
                                * progress
                );

        spawnCloudWall(
                client,
                progress,
                distance,
                cloudCount
        );

        if (progress >= 0.30F) {
            spawnRainCurtain(
                    client,
                    progress,
                    distance
            );
        }
    }

    private static void spawnCloudWall(
            Minecraft client,
            float progress,
            double distance,
            int count
    ) {
        for (int i = 0; i < count; i++) {
            double x =
                    client.player.getX()
                            + distance
                            + (RANDOM.nextDouble() - 0.5)
                            * WALL_THICKNESS;

            double z =
                    client.player.getZ()
                            + (RANDOM.nextDouble() - 0.5)
                            * WALL_HALF_WIDTH
                            * 2.0;

            int groundY =
                    client.level.getHeight(
                            Heightmap.Types.MOTION_BLOCKING,
                            Mth.floor(x),
                            Mth.floor(z)
                    );

            /*
             * Bias some particles toward the bottom,
             * creating a visibly darker leading edge
             * along the terrain.
             */
            double heightFraction =
                    Math.pow(
                            RANDOM.nextDouble(),
                            1.35
                    );

            double y =
                    groundY - 3.0
                            + heightFraction
                            * WALL_HEIGHT;

            double westSpeed =
                    -0.055
                            - progress * 0.16
                            - RANDOM.nextDouble() * 0.045;

            double verticalMovement =
                    -0.018
                            + RANDOM.nextDouble() * 0.036;

            double sidewaysMovement =
                    (RANDOM.nextDouble() - 0.5)
                            * 0.035;

            ParticleOptions particle =
                    selectCloudParticle();

            client.level.addAlwaysVisibleParticle(
                    particle,
                    x,
                    y,
                    z,
                    westSpeed,
                    verticalMovement,
                    sidewaysMovement
            );
        }
    }

    private static ParticleOptions selectCloudParticle() {
        float choice = RANDOM.nextFloat();

        if (choice < 0.48F) {
            return ParticleTypes.CAMPFIRE_SIGNAL_SMOKE;
        }

        if (choice < 0.88F) {
            return ParticleTypes.LARGE_SMOKE;
        }

        return ParticleTypes.ASH;
    }

    private static void spawnRainCurtain(
            Minecraft client,
            float progress,
            double distance
    ) {
        int rainCount =
                Mth.floor(
                        5.0F
                                + progress * 24.0F
                );

        for (int i = 0; i < rainCount; i++) {
            double x =
                    client.player.getX()
                            + distance
                            + (RANDOM.nextDouble() - 0.5)
                            * (WALL_THICKNESS + 10.0);

            double z =
                    client.player.getZ()
                            + (RANDOM.nextDouble() - 0.5)
                            * WALL_HALF_WIDTH
                            * 2.0;

            int groundY =
                    client.level.getHeight(
                            Heightmap.Types.MOTION_BLOCKING,
                            Mth.floor(x),
                            Mth.floor(z)
                    );

            double y =
                    groundY
                            + 12.0
                            + RANDOM.nextDouble() * 42.0;

            client.level.addAlwaysVisibleParticle(
                    ParticleTypes.RAIN,
                    x,
                    y,
                    z,
                    -0.10 - progress * 0.18,
                    -1.0,
                    (RANDOM.nextDouble() - 0.5)
                            * 0.05
            );
        }
    }
}