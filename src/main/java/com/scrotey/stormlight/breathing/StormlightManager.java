package com.scrotey.stormlight.breathing;

import com.scrotey.stormlight.network.AbilityInputPayload;
import com.scrotey.stormlight.network.StormlightStatusPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class StormlightManager {
    private static final int STATUS_SYNC_INTERVAL_TICKS = 5;

    private static final List<StormlightAbility> ABILITIES = List.of(
            new StrengthSurgeAbility(),
            new EmergencyHealAbility()
    );

    private static final Map<AbilityId, StormlightAbility> ABILITIES_BY_ID =
            ABILITIES.stream().collect(
                    Collectors.toMap(StormlightAbility::id, ability -> ability)
            );

    private static final Map<UUID, EnumSet<AbilityId>> ACTIVE_ABILITIES =
            new HashMap<>();

    private StormlightManager() {
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(StormlightManager::tick);

        ServerLifecycleEvents.SERVER_STOPPED.register(
                server -> ACTIVE_ABILITIES.clear()
        );

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();
            EnumSet<AbilityId> active = ACTIVE_ABILITIES.remove(player.getUUID());

            if (active != null) {
                for (AbilityId id : active) {
                    ABILITIES_BY_ID.get(id).stop(player);
                }
            }
        });
    }

    public static void handleInput(
            ServerPlayer player,
            AbilityId id,
            AbilityInputPayload.Action action
    ) {
        StormlightAbility ability = ABILITIES_BY_ID.get(id);

        if (ability == null) {
            return;
        }

        EnumSet<AbilityId> active = ACTIVE_ABILITIES.computeIfAbsent(
                player.getUUID(),
                uuid -> EnumSet.noneOf(AbilityId.class)
        );

        boolean isActive = active.contains(id);

        switch (action) {
            case TOGGLE -> {
                if (ability.activationMode() != StormlightAbility.ActivationMode.TOGGLE) {
                    return;
                }

                if (isActive) {
                    ability.stop(player);
                    active.remove(id);
                } else if (ability.tryStart(player)) {
                    active.add(id);
                }
            }
            case START -> {
                if (ability.activationMode() != StormlightAbility.ActivationMode.HOLD) {
                    return;
                }

                if (!isActive && ability.tryStart(player)) {
                    active.add(id);
                }
            }
            case STOP -> {
                if (isActive) {
                    ability.stop(player);
                    active.remove(id);
                }
            }
        }

        sendStatus(player, active);
    }

    private static void tick(MinecraftServer server) {
        long gameTime = server.overworld().getGameTime();
        boolean syncNow = gameTime % STATUS_SYNC_INTERVAL_TICKS == 0;

        ACTIVE_ABILITIES.keySet().removeIf(
                uuid -> server.getPlayerList().getPlayer(uuid) == null
        );

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            EnumSet<AbilityId> active = ACTIVE_ABILITIES.get(player.getUUID());

            if (active == null || active.isEmpty()) {
                continue;
            }

            boolean changed = false;

            for (Iterator<AbilityId> it = active.iterator(); it.hasNext(); ) {
                StormlightAbility ability = ABILITIES_BY_ID.get(it.next());

                if (!ability.tick(player, gameTime)) {
                    ability.stop(player);
                    it.remove();
                    changed = true;
                }
            }

            if (changed || syncNow) {
                sendStatus(player, active);
            }
        }
    }

    private static void sendStatus(ServerPlayer player, EnumSet<AbilityId> active) {
        StormlightPool.Totals totals = StormlightPool.getTotals(player);
        int mask = 0;

        for (AbilityId id : active) {
            mask |= 1 << id.ordinal();
        }

        ServerPlayNetworking.send(
                player,
                new StormlightStatusPayload(
                        totals.charge(),
                        totals.capacity(),
                        mask
                )
        );
    }
}
