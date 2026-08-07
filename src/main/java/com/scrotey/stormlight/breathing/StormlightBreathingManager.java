package com.scrotey.stormlight.breathing;

import com.scrotey.stormlight.item.SphereItem;
import com.scrotey.stormlight.network.StormlightStatusPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class StormlightBreathingManager {
    private static final int TICKS_PER_SECOND = 20;

    // One unit per second means a full Diamond Chip lasts 10 seconds,
    // while a full Emerald Broam lasts 1,000 seconds.
    private static final int DRAIN_INTERVAL_TICKS = TICKS_PER_SECOND;
    private static final int DRAIN_PER_INTERVAL = 1;

    private static final int EFFECT_REFRESH_INTERVAL_TICKS = 20;
    private static final int EFFECT_DURATION_TICKS = 45;
    private static final int PARTICLE_INTERVAL_TICKS = 5;
    private static final int STATUS_SYNC_INTERVAL_TICKS = 5;

    private static final Set<UUID> BREATHING_PLAYERS =
            new HashSet<>();

    private StormlightBreathingManager() {
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(
                StormlightBreathingManager::tick
        );

        ServerLifecycleEvents.SERVER_STOPPED.register(
                server -> BREATHING_PLAYERS.clear()
        );
    }

    public static void setBreathing(
            ServerPlayer player,
            boolean requested
    ) {
        boolean canStart = requested
                && player.isAlive()
                && !player.isSpectator()
                && getTotals(player).charge() > 0;

        if (canStart) {
            BREATHING_PLAYERS.add(player.getUUID());
            applyBreathingEffects(player);
        } else {
            BREATHING_PLAYERS.remove(player.getUUID());
        }

        syncStatus(player);
    }

    private static void tick(MinecraftServer server) {
        long gameTime = server.overworld().getGameTime();

        BREATHING_PLAYERS.removeIf(
                uuid -> server.getPlayerList().getPlayer(uuid) == null
        );

        boolean drainNow =
                gameTime % DRAIN_INTERVAL_TICKS == 0;
        boolean refreshEffectsNow =
                gameTime % EFFECT_REFRESH_INTERVAL_TICKS == 0;
        boolean particlesNow =
                gameTime % PARTICLE_INTERVAL_TICKS == 0;
        boolean syncNow =
                gameTime % STATUS_SYNC_INTERVAL_TICKS == 0;

        for (ServerPlayer player
                : server.getPlayerList().getPlayers()) {
            UUID playerId = player.getUUID();
            boolean breathing = BREATHING_PLAYERS.contains(playerId);
            Totals totals = getTotals(player);
            boolean stateChanged = false;

            if (breathing && (!player.isAlive()
                    || player.isSpectator()
                    || totals.charge() <= 0)) {
                BREATHING_PLAYERS.remove(playerId);
                breathing = false;
                stateChanged = true;
            }

            if (breathing) {
                if (refreshEffectsNow) {
                    applyBreathingEffects(player);
                }

                if (particlesNow) {
                    spawnBreathingParticles(player);
                }

                if (drainNow) {
                    drainStormlight(player, DRAIN_PER_INTERVAL);
                    totals = getTotals(player);

                    if (totals.charge() <= 0) {
                        BREATHING_PLAYERS.remove(playerId);
                        breathing = false;
                        stateChanged = true;
                    }
                }
            }

            if (syncNow || drainNow || stateChanged) {
                sendStatus(player, totals, breathing);
            }
        }
    }

    private static void applyBreathingEffects(ServerPlayer player) {
        addHiddenEffect(player, MobEffects.REGENERATION);
        addHiddenEffect(player, MobEffects.SPEED);
        addHiddenEffect(player, MobEffects.STRENGTH);
    }

    private static void addHiddenEffect(
            ServerPlayer player,
            net.minecraft.core.Holder<
                    net.minecraft.world.effect.MobEffect
                    > effect
    ) {
        player.addEffect(
                new MobEffectInstance(
                        effect,
                        EFFECT_DURATION_TICKS,
                        0,
                        true,
                        false,
                        false
                )
        );
    }

    private static void spawnBreathingParticles(
            ServerPlayer player
    ) {
        ServerLevel level = (ServerLevel) player.level();

        level.sendParticles(
                ParticleTypes.SOUL,
                player.getX(),
                player.getY() + 1.0,
                player.getZ(),
                2,
                0.30,
                0.45,
                0.30,
                0.015
        );
    }

    private static void drainStormlight(
            ServerPlayer player,
            int amount
    ) {
        int remaining = amount;

        for (ItemStack stack
                : player.getInventory().getNonEquipmentItems()) {
            remaining = drainFromSphere(stack, remaining);

            if (remaining == 0) {
                break;
            }
        }

        if (remaining > 0) {
            drainFromSphere(player.getOffhandItem(), remaining);
        }

        player.getInventory().setChanged();
    }

    private static int drainFromSphere(
            ItemStack stack,
            int requested
    ) {
        if (requested <= 0
                || !(stack.getItem() instanceof SphereItem sphere)) {
            return requested;
        }

        int available = sphere.getCharge(stack);
        int drained = Math.min(available, requested);

        if (drained > 0) {
            sphere.setCharge(stack, available - drained);
        }

        return requested - drained;
    }

    private static Totals getTotals(ServerPlayer player) {
        int charge = 0;
        int capacity = 0;

        for (ItemStack stack
                : player.getInventory().getNonEquipmentItems()) {
            if (stack.getItem() instanceof SphereItem sphere) {
                charge += sphere.getCharge(stack);
                capacity += sphere.getCapacity();
            }
        }

        ItemStack offhand = player.getOffhandItem();

        if (offhand.getItem() instanceof SphereItem sphere) {
            charge += sphere.getCharge(offhand);
            capacity += sphere.getCapacity();
        }

        return new Totals(charge, capacity);
    }

    private static void syncStatus(ServerPlayer player) {
        Totals totals = getTotals(player);

        sendStatus(
                player,
                totals,
                BREATHING_PLAYERS.contains(player.getUUID())
        );
    }

    private static void sendStatus(
            ServerPlayer player,
            Totals totals,
            boolean breathing
    ) {
        ServerPlayNetworking.send(
                player,
                new StormlightStatusPayload(
                        totals.charge(),
                        totals.capacity(),
                        breathing
                )
        );
    }

    private record Totals(int charge, int capacity) {
    }
}
