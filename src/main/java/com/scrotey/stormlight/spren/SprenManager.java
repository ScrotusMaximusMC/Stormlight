package com.scrotey.stormlight.spren;

import com.scrotey.stormlight.progression.RadiantProgression;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Owns the online lifecycle of player-bound spren.
 *
 * The bond belongs to the player, not to a permanently persisted mob instance:
 * while an eligible player is online they have exactly one spren; when they
 * leave, that spren is discarded. A fresh physical manifestation is created
 * next time they join.
 */
public final class SprenManager {
    private static final Map<UUID, UUID> ACTIVE = new HashMap<>();

    private static final int CHECK_INTERVAL_TICKS = 20;
    private static final double SEARCH_RADIUS = 96.0;

    private SprenManager() {
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(SprenManager::tick);

        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> {
                    ServerPlayer player = handler.getPlayer();

                    // Do not trust any stale in-memory entry across reconnects.
                    ACTIVE.remove(player.getUUID());

                    // The regular server tick will create the spren once the
                    // player's progression data is fully available.
                }
        );

        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> removePlayerSpren(handler.getPlayer())
        );
    }

    private static void tick(MinecraftServer server) {
        if (server.overworld().getGameTime() % CHECK_INTERVAL_TICKS != 0L) {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tickPlayer(player);
        }

        // An online player is the only valid reason for an ACTIVE entry.
        ACTIVE.keySet().removeIf(
                uuid -> server.getPlayerList().getPlayer(uuid) == null
        );
    }

    private static void tickPlayer(ServerPlayer player) {
        UUID ownerUuid = player.getUUID();

        if (RadiantProgression.getOrder(player).isEmpty()) {
            removePlayerSpren(player);
            return;
        }

        List<SprenEntity> owned = findOwnedSpren(player);

        if (!owned.isEmpty()) {
            SprenEntity keeper = chooseKeeper(ownerUuid, owned);
            ACTIVE.put(ownerUuid, keeper.getUUID());

            // Exactly one manifestation per online player.
            for (SprenEntity spren : owned) {
                if (spren != keeper) {
                    spren.discard();
                }
            }
            return;
        }

        ACTIVE.remove(ownerUuid);
        spawnSpren(player);
    }

    private static SprenEntity chooseKeeper(
            UUID ownerUuid,
            List<SprenEntity> owned
    ) {
        UUID activeUuid = ACTIVE.get(ownerUuid);

        if (activeUuid != null) {
            for (SprenEntity spren : owned) {
                if (spren.getUUID().equals(activeUuid)) {
                    return spren;
                }
            }
        }

        return owned.getFirst();
    }

    private static List<SprenEntity> findOwnedSpren(ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(SEARCH_RADIUS);

        return player.level().getEntitiesOfClass(
                SprenEntity.class,
                area,
                spren -> spren.isAlive() && spren.isOwnedBy(player)
        );
    }

    private static void spawnSpren(ServerPlayer player) {
        ServerLevel level = player.level();
        SprenEntity spren = new SprenEntity(
                ModSprenEntities.SPREN,
                level
        );

        Vec3 start = player.position().add(1.2, 1.8, 0.4);

        spren.setOwner(player);
        spren.setPos(start.x, start.y, start.z);
        level.addFreshEntity(spren);

        ACTIVE.put(player.getUUID(), spren.getUUID());
    }

    /**
     * Called on logout and whenever a player ceases to qualify for a spren.
     */
    private static void removePlayerSpren(ServerPlayer player) {
        UUID ownerUuid = player.getUUID();

        for (SprenEntity spren : findOwnedSpren(player)) {
            spren.discard();
        }

        ACTIVE.remove(ownerUuid);
    }
}
