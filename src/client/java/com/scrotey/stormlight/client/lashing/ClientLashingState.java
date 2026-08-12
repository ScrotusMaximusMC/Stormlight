package com.scrotey.stormlight.client.lashing;

import com.scrotey.stormlight.client.StormlightFovFeedback;
import com.scrotey.stormlight.lashing.LashingDirection;
import com.scrotey.stormlight.network.LashingStatePayload;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class ClientLashingState {
    private static final double GRAVITY_ACCELERATION = 0.08;
    private static final double STABILISATION_DECELERATION = 0.25;
    private static final double STOPPED_SPEED = 0.025;

    private static LashingDirection direction;
    private static boolean active;
    private static boolean stabilising;
    private static boolean restoreNoGravity;

    private ClientLashingState() {
    }

    public static void update(LashingStatePayload payload) {
        Minecraft client = Minecraft.getInstance();

        if (!payload.active() && !payload.stabilising()) {
            clear(client);
            return;
        }

        LashingDirection newDirection = null;

        if (payload.active()) {
            newDirection = LashingDirection.fromComponents(
                    payload.directionX(),
                    payload.directionY(),
                    payload.directionZ()
            ).orElse(null);

            if (newDirection == null) {
                clear(client);
                return;
            }
        }

        if (!active && !stabilising && client.player != null) {
            restoreNoGravity = client.player.isNoGravity();
        }

        direction = newDirection;
        active = payload.active();
        stabilising = payload.stabilising();
        StormlightFovFeedback.setLashingActive(active || stabilising);
    }

    public static void tick(Minecraft client) {
        if (client.player == null) {
            clear(client);
            return;
        }

        if (!active && !stabilising) {
            return;
        }

        client.player.setNoGravity(true);

        if (stabilising) {
            applyStabilisation(client);
            return;
        }

        if (direction == null) {
            clear(client);
            return;
        }

        Vec3 velocity = client.player.getDeltaMovement();
        client.player.setDeltaMovement(
                velocity.add(
                        direction.x() * GRAVITY_ACCELERATION,
                        direction.y() * GRAVITY_ACCELERATION,
                        direction.z() * GRAVITY_ACCELERATION
                )
        );
    }

    private static void applyStabilisation(Minecraft client) {
        client.player.fallDistance = 0.0F;

        Vec3 velocity = client.player.getDeltaMovement();
        double speed = velocity.length();

        if (speed <= STABILISATION_DECELERATION
                || speed <= STOPPED_SPEED) {
            client.player.setDeltaMovement(Vec3.ZERO);
            return;
        }

        client.player.setDeltaMovement(
                velocity.scale(
                        (speed - STABILISATION_DECELERATION) / speed
                )
        );
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isStabilising() {
        return stabilising;
    }

    private static void clear(Minecraft client) {
        if ((active || stabilising) && client.player != null) {
            client.player.setNoGravity(restoreNoGravity);
        }

        direction = null;
        active = false;
        stabilising = false;
        restoreNoGravity = false;
        StormlightFovFeedback.setLashingActive(false);
    }
}
