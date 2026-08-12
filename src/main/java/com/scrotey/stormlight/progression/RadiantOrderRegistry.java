package com.scrotey.stormlight.progression;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RadiantOrderRegistry {
    public static final int NO_ORDER_NETWORK_ID = 0;

    public static final int HORIZONTAL_LASHING_LEVEL = 3;
    public static final int FALL_PROTECTION_LEVEL = 6;
    public static final int VERTICAL_LASHING_LEVEL = 8;
    public static final int REABSORPTION_LEVEL = 10;

    public static final RadiantOrder WINDRUNNER = new RadiantOrder(
            "windrunner",
            1,
            "order.stormlight.windrunner",
            0xFF8DEBFF,
            List.of(
                    level(1, 100, 0, 0, "breathing"),
                    level(2, 200, 0, 0, "expanded_reserve"),
                    level(3, 200, 0, 0, "horizontal_lashing"),
                    level(4, 200, 1, 0, "adept_windrunning"),
                    level(5, 400, 1, 0, "greater_reserve"),
                    level(6, 400, 1, 0, "surefoot"),
                    level(7, 600, 1, 0, "vast_reserve"),
                    level(8, 600, 1, 0, "vertical_lashing"),
                    level(9, 600, 2, 1, "expert_windrunning"),
                    level(10, 1000, 2, 2, "radiant_mastery")
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
            int speedAmplifier,
            int strengthAmplifier,
            String translationSuffix
    ) {
        return new RadiantLevel(
                level,
                capacity,
                speedAmplifier,
                strengthAmplifier,
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
