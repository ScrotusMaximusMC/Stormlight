package com.scrotey.stormlight.progression;

import com.mojang.serialization.Codec;
import com.scrotey.stormlight.Stormlight;
import com.scrotey.stormlight.attachment.ModAttachments;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class RadiantProgression {
    private static final String NO_ORDER = "";

    public static final AttachmentType<String> RADIANT_ORDER =
            AttachmentRegistry.<String>create(
                    Stormlight.id("radiant_order"),
                    builder -> builder
                            .initializer(() -> NO_ORDER)
                            .persistent(Codec.STRING)
                            .copyOnDeath()
            );

    public static final AttachmentType<Integer> RADIANT_LEVEL =
            AttachmentRegistry.<Integer>create(
                    Stormlight.id("radiant_level"),
                    builder -> builder
                            .initializer(() -> 0)
                            .persistent(Codec.intRange(0, 100))
                            .copyOnDeath()
            );

    private RadiantProgression() {
    }

    public static void initialize() {
        // Forces attachment registration during common initialization.
    }

    public static Optional<RadiantOrder> getOrder(Player player) {
        return RadiantOrderRegistry.byId(
                player.getAttachedOrElse(RADIANT_ORDER, NO_ORDER)
        );
    }

    public static int getLevel(Player player) {
        if (getOrder(player).isEmpty()) {
            return 0;
        }

        return Math.max(
                0,
                player.getAttachedOrElse(RADIANT_LEVEL, 0)
        );
    }

    public static Optional<RadiantLevel> getLevelDefinition(
            Player player
    ) {
        return getOrder(player)
                .flatMap(order -> order.level(getLevel(player)));
    }

    public static int getStormlightCapacity(Player player) {
        return getLevelDefinition(player)
                .map(RadiantLevel::stormlightCapacity)
                .orElse(0);
    }

    public static int getOrderNetworkId(Player player) {
        return getOrder(player)
                .map(RadiantOrder::networkId)
                .orElse(RadiantOrderRegistry.NO_ORDER_NETWORK_ID);
    }

    public static boolean chooseOrder(
            ServerPlayer player,
            RadiantOrder order
    ) {
        if (getOrder(player).isPresent() || order.level(1).isEmpty()) {
            return false;
        }

        player.setAttached(RADIANT_ORDER, order.id());
        player.setAttached(RADIANT_LEVEL, 1);
        clampReserve(player);
        return true;
    }

    public static int clampReserve(Player player) {
        int capacity = getStormlightCapacity(player);
        int reserve = ModAttachments.getPersonalStormlight(player);
        int clamped = Math.min(reserve, capacity);

        if (reserve != clamped) {
            ModAttachments.setPersonalStormlight(player, clamped);
        }

        return clamped;
    }
}
