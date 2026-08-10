package com.scrotey.stormlight.client.highstorm;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class ClientHighstormWind {
    /*
     * Highstorms blow due west (negative X). Ground acceleration is stronger
     * than the original server-side value because ordinary ground movement
     * and friction made that value almost impossible to feel. Airborne force
     * remains close to the strength used by the first working version.
     */
    private static final double GROUND_WIND_ACCELERATION = 0.0160;
    private static final double AIRBORNE_WIND_ACCELERATION = 0.0115;
    private static final double SNEAKING_WIND_MULTIPLIER = 0.30;
    private static final double MAX_GROUND_WEST_SPEED = 0.18;
    private static final double MAX_AIRBORNE_WEST_SPEED = 0.32;

    /*
     * The storm comes from the east. Nine short rays sample a player-sized
     * rectangle in that direction, allowing nearby walls, cliffs and boulders
     * to create full or partial wind shadows even when the sky is visible.
     */
    private static final int WIND_SHELTER_DISTANCE = 6;
    private static final int FULL_SHELTER_BLOCKED_RAYS = 5;
    private static final double[] SHELTER_HEIGHT_OFFSETS = {
            0.25,
            1.0,
            1.75
    };
    private static final double[] SHELTER_SIDE_OFFSETS = {
            -0.35,
            0.0,
            0.35
    };

    private ClientHighstormWind() {
    }

    public static void tick(Minecraft client) {
        LocalPlayer player = client.player;

        if (player == null
                || client.level == null
                || player.isCreative()
                || player.isSpectator()) {
            return;
        }

        float intensity = getWindIntensity();

        if (intensity <= 0.0F
                || !client.level.canSeeSky(
                        player.blockPosition().above()
                )) {
            return;
        }

        double shelterMultiplier = getShelterMultiplier(
                player
        );

        if (shelterMultiplier <= 0.0) {
            return;
        }

        boolean airborne = !player.onGround();
        double baseAcceleration = airborne
                ? AIRBORNE_WIND_ACCELERATION
                : GROUND_WIND_ACCELERATION;
        double bracingMultiplier = player.isShiftKeyDown()
                ? SNEAKING_WIND_MULTIPLIER
                : 1.0;
        double acceleration = baseAcceleration
                * intensity
                * bracingMultiplier
                * shelterMultiplier;

        Vec3 movement = player.getDeltaMovement();
        double maximumWestSpeed = airborne
                ? MAX_AIRBORNE_WEST_SPEED
                : MAX_GROUND_WEST_SPEED;

        /*
         * Do not add more westward speed after the cap is reached. Eastward
         * movement is resisted rather than blocked, while existing knockback
         * and faster movement in either direction remain otherwise intact.
         */
        double allowedAcceleration = Math.max(
                0.0,
                movement.x + maximumWestSpeed
        );
        double appliedAcceleration = Math.min(
                acceleration,
                allowedAcceleration
        );

        if (appliedAcceleration > 0.0) {
            player.setDeltaMovement(
                    movement.add(-appliedAcceleration, 0.0, 0.0)
            );
        }
    }

    private static float getWindIntensity() {
        if (ClientHighstormState.isApproaching()) {
            float progress = ClientHighstormState.getApproachProgress();
            return progress * progress * 0.35F;
        }

        if (ClientHighstormState.isHighstorm()) {
            return 1.0F;
        }

        if (ClientHighstormState.isPassing()) {
            return 1.0F - ClientHighstormState.getPassingProgress();
        }

        return 0.0F;
    }

    private static double getShelterMultiplier(
            LocalPlayer player
    ) {
        int blockedRays = 0;

        for (double heightOffset : SHELTER_HEIGHT_OFFSETS) {
            for (double sideOffset : SHELTER_SIDE_OFFSETS) {
                if (isShelterRayBlocked(
                        player,
                        heightOffset,
                        sideOffset
                )) {
                    blockedRays++;
                }
            }
        }

        double shelterCoverage = blockedRays
                / (double) FULL_SHELTER_BLOCKED_RAYS;

        return Math.max(
                0.0,
                Math.min(1.0, 1.0 - shelterCoverage)
        );
    }

    private static boolean isShelterRayBlocked(
            LocalPlayer player,
            double heightOffset,
            double sideOffset
    ) {
        for (int eastOffset = 1;
             eastOffset <= WIND_SHELTER_DISTANCE;
             eastOffset++) {
            BlockPos samplePos = BlockPos.containing(
                    player.getX() + eastOffset,
                    player.getY() + heightOffset,
                    player.getZ() + sideOffset
            );

            if (player.level().getBlockState(samplePos)
                    .isCollisionShapeFullBlock(
                            player.level(),
                            samplePos
                    )) {
                return true;
            }
        }

        return false;
    }
}
