package com.scrotey.stormlight.progression;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RadiantOrderRegistry {
    public static final int NO_ORDER_NETWORK_ID = 0;

    public static final int HORIZONTAL_LASHING_LEVEL = 3;
    public static final int FALL_PROTECTION_LEVEL = 6;
    public static final int VERTICAL_LASHING_LEVEL = 7;

    public static final RadiantOrder WINDRUNNER = new RadiantOrder(
            "windrunner",
            1,
            "order.stormlight.windrunner",
            0xFF8DEBFF,
            List.of(
                    level(1, 100, "breathing"),
                    level(2, 200, "capacity_200"),
                    level(3, 200, "horizontal_lashing"),
                    level(4, 300, "capacity_300"),
                    level(5, 300, "undiscovered"),
                    level(6, 300, "fall_protection"),
                    level(7, 300, "vertical_lashing"),
                    level(8, 300, "undiscovered"),
                    level(9, 300, "undiscovered"),
                    level(10, 300, "undiscovered")
            )
    );

    private static final Map<String, RadiantOrder> BY_ID =
            new LinkedHashMap<>();
    private static final Map<Integer, RadiantOrder> BY_NETWORK_ID =
            new LinkedHashMap<>();

    static {
        register(WINDRUNNER);
    }

    private RadiantOrderRegistry() {
    }

    private static RadiantLevel level(
            int level,
            int capacity,
            String translationSuffix
    ) {
        return new RadiantLevel(
                level,
                capacity,
                0,
                0,
                "progression.stormlight.windrunner."
                        + translationSuffix + ".title",
                "progression.stormlight.windrunner."
                        + translationSuffix + ".description"
        );
    }

    private static void register(RadiantOrder order) {
        if (BY_ID.putIfAbsent(order.id(), order) != null
                || BY_NETWORK_ID.putIfAbsent(
                order.networkId(),
                order
        ) != null) {
            throw new IllegalStateException(
                    "Duplicate Radiant order: " + order.id()
            );
        }
    }

    public static Optional<RadiantOrder> byId(String id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    public static Optional<RadiantOrder> byNetworkId(int networkId) {
        return Optional.ofNullable(BY_NETWORK_ID.get(networkId));
    }
}
