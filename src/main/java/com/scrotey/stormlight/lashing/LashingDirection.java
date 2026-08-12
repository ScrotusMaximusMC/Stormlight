package com.scrotey.stormlight.lashing;

import java.util.Optional;

public record LashingDirection(
        double x,
        double z
) {
    private static final double MINIMUM_LENGTH_SQUARED = 1.0E-8;

    public LashingDirection {
        double lengthSquared = x * x + z * z;

        if (!Double.isFinite(lengthSquared)
                || lengthSquared < MINIMUM_LENGTH_SQUARED) {
            throw new IllegalArgumentException(
                    "Horizontal Lashing direction must be finite and non-zero"
            );
        }

        double inverseLength = 1.0 / Math.sqrt(lengthSquared);
        x *= inverseLength;
        z *= inverseLength;
    }

    public static LashingDirection fromYaw(float yawDegrees) {
        double yawRadians = Math.toRadians(yawDegrees);

        return new LashingDirection(
                -Math.sin(yawRadians),
                Math.cos(yawRadians)
        );
    }

    public static Optional<LashingDirection> fromComponents(
            double x,
            double z
    ) {
        try {
            return Optional.of(new LashingDirection(x, z));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
