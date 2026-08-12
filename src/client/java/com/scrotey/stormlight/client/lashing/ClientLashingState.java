package com.scrotey.stormlight.client.lashing;

import com.scrotey.stormlight.client.StormlightFovFeedback;
import com.scrotey.stormlight.lashing.LashingDirection;
import com.scrotey.stormlight.network.LashingStatePayload;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class ClientLashingState {
    private static final double GRAVITY_ACCELERATION = 0.08;

    private static LashingDirection direction;
    private static boolean restoreNoGravity;

    private ClientLashingState() {
    }

    public static void update(LashingStatePayload payload) {
        Minecraft client = Minecraft.getInstance();

        if (!payload.active()) {
            clear(client);
            return;
        }

        LashingDirection newDirection =
                LashingDirection.fromComponents(
                        payload.directionX(),
                        payload.directionZ()
                ).orElse(null);

        if (newDirection == null) {
            clear(client);
            return;
        }

        if (direction == null && client.player != null) {
            restoreNoGravity = client.player.isNoGravity();
        }

        direction = newDirection;
        StormlightFovFeedback.setLashingActive(true);
    }

    public static void tick(Minecraft client) {
        if (client.player == null) {
            clear(client);
            return;
        }

        if (direction == null) {
            return;
        }

        client.player.setNoGravity(true);

        Vec3 velocity = client.player.getDeltaMovement();
        client.player.setDeltaMovement(
                velocity.add(
                        direction.x() * GRAVITY_ACCELERATION,
                        0.0,
                        direction.z() * GRAVITY_ACCELERATION
                )
        );
    }

    public static boolean isActive() {
        return direction != null;
    }

    private static void clear(Minecraft client) {
        if (direction != null && client.player != null) {
            client.player.setNoGravity(restoreNoGravity);
        }

        direction = null;
        restoreNoGravity = false;
        StormlightFovFeedback.setLashingActive(false);
    }
}
