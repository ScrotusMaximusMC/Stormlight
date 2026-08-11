package com.scrotey.stormlight.progression;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RadiantOrderRegistry {
    public static final int NO_ORDER_NETWORK_ID = 0;

    public static final RadiantOrder WINDRUNNER = new RadiantOrder(
            "windrunner",
            1,
            "order.stormlight.windrunner",
            0xFF8DEBFF,
            List.of(new RadiantLevel(1, 100, 0, 0))
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
