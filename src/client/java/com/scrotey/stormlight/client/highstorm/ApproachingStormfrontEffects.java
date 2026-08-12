package com.scrotey.stormlight.client.highstorm;

import com.scrotey.stormlight.particle.ModParticles;
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

    /*
     * During the active Highstorm, particles spawn in a tighter
     * area just east of the player and stream rapidly westwards.
     */
    private static final int HIGHSTORM_PARTICLES_PER_TICK = 52;
    private static final double HIGHSTORM_MIN_EAST_DISTANCE = 8.0;
    private static final double HIGHSTORM_SPAWN_DEPTH = 52.0;
    private static final double HIGHSTORM_HALF_WIDTH = 58.0;
    private static final double HIGHSTORM_HEIGHT = 48.0;

    /*
     * Windblown leaves are kept separate from the smoke count so their
     * density and turbulence can be tuned without changing the storm wall.
     */
    private static final int HIGHSTORM_LEAVES_PER_TICK = 15;
    private static final double LEAF_MIN_EAST_DISTANCE = 9.0;
    private static final double LEAF_SPAWN_DEPTH = 38.0;
    private static final double LEAF_HALF_WIDTH = 28.0;
    private static final double LEAF_MIN_HEIGHT = 1.5;
    private static final double LEAF_HEIGHT = 15.0;

    private static final RandomSource RANDOM =
            RandomSource.create();

    private ApproachingStormfrontEffects() {
    }

    public static void tick(
            Minecraft client
    ) {
        if (client.level == null
                || client.player == null
                || !client.level.dimension()
                .equals(Level.OVERWORLD)) {
            return;
        }

        /*
         * The active Highstorm emits a denser and faster-moving
         * particle field every client tick.
         */
        if (ClientHighstormState.isHighstorm()) {
            spawnHighstormClouds(client, 1.0F);
            return;
        }

        if (ClientHighstormState.isPassing()) {
            float progress =
                    ClientHighstormState.getPassingProgress();

            float intensity =
                    1.0F - smoothstep(progress);

            spawnHighstormClouds(client, intensity);
            return;
        }

        if (!ClientHighstormState.isApproaching()) {
            return;
        }

        float progress =
                ClientHighstormState.getApproachProgress();

        float smoothProgress =
                progress
                        * progress
                        * (3.0F - 2.0F * progress);

        /*
         * Loose leaves arrive ahead of the storm wall. Their density rises
         * smoothly from none to the full Highstorm amount, avoiding a sudden
         * burst when the phase changes to HIGHSTORM.
         */
        spawnHighstormLeaves(
                client,
                smoothProgress
        );

        /*
         * The heavier smoke wall and rain retain their lighter,
         * every-second-tick emission rate.
         */
        if ((client.level.getGameTime() & 1L) != 0L) {
            return;
        }

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

        if (choice < 0.25F) {
            return ParticleTypes.CAMPFIRE_SIGNAL_SMOKE;
        }

        if (choice < 0.50F) {
            return ParticleTypes.LARGE_SMOKE;
        }

        return ParticleTypes.ASH;
    }

    private static void spawnHighstormClouds(
            Minecraft client,
            float intensity
    ) {
        int particleCount = scaledCount(
                HIGHSTORM_PARTICLES_PER_TICK,
                intensity
        );

        for (int i = 0;
             i < particleCount;
             i++) {
            /*
             * Spawn the particles east of the player so they
             * travel through and beyond the player's position.
             */
            double x =
                    client.player.getX()
                            + HIGHSTORM_MIN_EAST_DISTANCE
                            + RANDOM.nextDouble()
                            * HIGHSTORM_SPAWN_DEPTH;

            double z =
                    client.player.getZ()
                            + (RANDOM.nextDouble() - 0.5)
                            * HIGHSTORM_HALF_WIDTH
                            * 2.0;

            int groundY =
                    client.level.getHeight(
                            Heightmap.Types.MOTION_BLOCKING,
                            Mth.floor(x),
                            Mth.floor(z)
                    );

            /*
             * Bias the particles towards the ground, producing
             * a particularly thick layer of smoke and ash below.
             */
            double heightFraction =
                    Math.pow(
                            RANDOM.nextDouble(),
                            1.25
                    );

            double y =
                    groundY - 3.0
                            + heightFraction
                            * HIGHSTORM_HEIGHT;

            /*
             * East is positive X. Because the Highstorm arrives
             * from the east, negative X carries debris westwards.
             *
             * This is around three times faster than the particles
             * at the end of the approaching phase.
             */
            double westSpeed =
                    (-0.52
                            - RANDOM.nextDouble() * 0.34)
                            * (0.55 + intensity * 0.45);

            double verticalMovement =
                    -0.045
                            + RANDOM.nextDouble() * 0.085;

            double sidewaysMovement =
                    (RANDOM.nextDouble() - 0.5)
                            * 0.16
                            * (0.55 + intensity * 0.45);

            client.level.addAlwaysVisibleParticle(
                    selectCloudParticle(),
                    x,
                    y,
                    z,
                    westSpeed,
                    verticalMovement,
                    sidewaysMovement
            );
        }

        spawnHighstormLeaves(client, intensity);
    }

    private static void spawnHighstormLeaves(
            Minecraft client,
            float intensity
    ) {
        int leafCount = scaledCount(
                HIGHSTORM_LEAVES_PER_TICK,
                intensity
        );

        for (int i = 0; i < leafCount; i++) {
            double x = client.player.getX()
                    + LEAF_MIN_EAST_DISTANCE
                    + RANDOM.nextDouble() * LEAF_SPAWN_DEPTH;

            double z = client.player.getZ()
                    + (RANDOM.nextDouble() - 0.5)
                    * LEAF_HALF_WIDTH * 2.0;

            int groundY = client.level.getHeight(
                    Heightmap.Types.MOTION_BLOCKING,
                    Mth.floor(x),
                    Mth.floor(z)
            );

            double y = groundY
                    + LEAF_MIN_HEIGHT
                    + RANDOM.nextDouble() * LEAF_HEIGHT;

            // Every leaf heads west, but no two receive quite the same gust.
            double westSpeed = (-0.48
                    - RANDOM.nextDouble() * 0.46)
                    * (0.55 + intensity * 0.45);
            double verticalMovement = -0.04
                    + RANDOM.nextDouble() * 0.16;
            double sidewaysMovement =
                    (RANDOM.nextDouble() - 0.5)
                            * 0.34
                            * (0.55 + intensity * 0.45);

            client.level.addAlwaysVisibleParticle(
                    ModParticles.HIGHSTORM_LEAF,
                    x,
                    y,
                    z,
                    westSpeed,
                    verticalMovement,
                    sidewaysMovement
            );
        }
    }

    /*
     * Fractional random rounding makes low intensities fade naturally instead
     * of snapping between whole particles per tick.
     */
    private static int scaledCount(
            int fullCount,
            float intensity
    ) {
        double exactCount = fullCount
                * Mth.clamp(intensity, 0.0F, 1.0F);

        int count = Mth.floor(exactCount);

        if (RANDOM.nextDouble() < exactCount - count) {
            count++;
        }

        return count;
    }

    private static float smoothstep(float progress) {
        float clamped = Mth.clamp(progress, 0.0F, 1.0F);
        return clamped
                * clamped
                * (3.0F - 2.0F * clamped);
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
