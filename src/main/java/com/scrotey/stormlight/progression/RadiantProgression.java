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
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 10;

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
        if (getOrder(player).isPresent()
                || order.level(MIN_LEVEL).isEmpty()
                || !canAffordLevel(player, MIN_LEVEL)) {
            return false;
        }

        consumeExperienceLevels(player, experienceCost(MIN_LEVEL));
        player.setAttached(RADIANT_ORDER, order.id());
        player.setAttached(RADIANT_LEVEL, MIN_LEVEL);
        clampReserve(player);
        return true;
    }

    public static UnlockResult unlockNextLevel(
            ServerPlayer player,
            int requestedLevel
    ) {
        Optional<RadiantOrder> order = getOrder(player);

        if (order.isEmpty()) {
            return UnlockResult.NO_ORDER;
        }

        int currentLevel = getLevel(player);

        if (currentLevel >= MAX_LEVEL) {
            return UnlockResult.MAX_LEVEL;
        }

        if (requestedLevel != currentLevel + 1
                || order.get().level(requestedLevel).isEmpty()) {
            return UnlockResult.INVALID_LEVEL;
        }

        if (!canAffordLevel(player, requestedLevel)) {
            return UnlockResult.NOT_ENOUGH_EXPERIENCE;
        }

        consumeExperienceLevels(
                player,
                experienceCost(requestedLevel)
        );
        player.setAttached(RADIANT_LEVEL, requestedLevel);
        clampReserve(player);
        return UnlockResult.SUCCESS;
    }

    public static int experienceCost(int level) {
        if (level < MIN_LEVEL || level > MAX_LEVEL) {
            throw new IllegalArgumentException(
                    "Radiant level out of range: " + level
            );
        }

        return level * 2;
    }

    public static boolean canAffordLevel(
            Player player,
            int level
    ) {
        return player.experienceLevel >= experienceCost(level);
    }

    public static boolean hasUnlocked(
            Player player,
            int requiredLevel
    ) {
        return getOrder(player).isPresent()
                && getLevel(player) >= requiredLevel;
    }

    private static void consumeExperienceLevels(
            ServerPlayer player,
            int amount
    ) {
        player.giveExperienceLevels(-amount);
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

    public enum UnlockResult {
        SUCCESS,
        NO_ORDER,
        INVALID_LEVEL,
        MAX_LEVEL,
        NOT_ENOUGH_EXPERIENCE
    }
}
