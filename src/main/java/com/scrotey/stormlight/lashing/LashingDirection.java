package com.scrotey.stormlight.lashing;

import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public record LashingDirection(
        double x,
        double y,
        double z
) {
    private static final double MINIMUM_LENGTH_SQUARED = 1.0E-8;

    public LashingDirection {
        double lengthSquared = x * x + y * y + z * z;

        if (!Double.isFinite(lengthSquared)
                || lengthSquared < MINIMUM_LENGTH_SQUARED) {
            throw new IllegalArgumentException(
                    "Lashing direction must be finite and non-zero"
            );
        }

        double inverseLength = 1.0 / Math.sqrt(lengthSquared);
        x *= inverseLength;
        y *= inverseLength;
        z *= inverseLength;
    }

    public static LashingDirection horizontalFromYaw(float yawDegrees) {
        double yawRadians = Math.toRadians(yawDegrees);

        return new LashingDirection(
                -Math.sin(yawRadians),
                0.0,
                Math.cos(yawRadians)
        );
    }

    public static LashingDirection fromLook(Vec3 look) {
        return new LashingDirection(look.x, look.y, look.z);
    }

    public static Optional<LashingDirection> fromComponents(
            double x,
            double y,
            double z
    ) {
        try {
            return Optional.of(new LashingDirection(x, y, z));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
