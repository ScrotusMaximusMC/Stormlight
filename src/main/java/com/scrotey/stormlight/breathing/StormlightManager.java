package com.scrotey.stormlight.breathing;

import com.scrotey.stormlight.attachment.ModAttachments;
import com.scrotey.stormlight.network.StormlightStatusPayload;
import com.scrotey.stormlight.particle.ModParticles;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class StormlightManager {
    private static final int BREATHE_AMOUNT_PER_TICK = 10;
    private static final int BREATHE_SOUND_INTERVAL_TICKS = 10;
    private static final int BREATHE_WARNING_INTERVAL_TICKS = 20;

    private static final int STATUS_SYNC_INTERVAL_TICKS = 5;

    private static final int PASSIVE_LEAK_INTERVAL_TICKS = 20;
    private static final int PASSIVE_LEAK_PER_INTERVAL = 1;

    private static final int EFFECT_REFRESH_INTERVAL_TICKS = 20;
    private static final int EFFECT_DURATION_TICKS = 45;

    private static final int HEAL_INTERVAL_TICKS = 10;
    private static final int HEAL_COST_PER_HALF_POINT = 25;
    private static final float HEAL_AMOUNT = 0.5F;

    private static final int PARTICLE_INTERVAL_TICKS = 5;
    private static final int PARTICLES_PER_BURST = 2;

    private static final Map<UUID, StormlightStatusPayload>
            LAST_SENT_STATUS = new HashMap<>();
    private static final Map<UUID, Long>
            LAST_BREATHE_SOUND_TICK = new HashMap<>();
    private static final Map<UUID, Long>
            LAST_BREATHE_WARNING_TICK = new HashMap<>();

    private StormlightManager() {
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(
                StormlightManager::tick
        );

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            LAST_SENT_STATUS.clear();
            LAST_BREATHE_SOUND_TICK.clear();
            LAST_BREATHE_WARNING_TICK.clear();
        });

        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> {
                    ServerPlayer player = handler.getPlayer();

                    LAST_SENT_STATUS.remove(player.getUUID());
                    sendStatus(player);
                }
        );

        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> {
                    UUID playerId = handler.getPlayer().getUUID();

                    LAST_SENT_STATUS.remove(playerId);
                    LAST_BREATHE_SOUND_TICK.remove(playerId);
                    LAST_BREATHE_WARNING_TICK.remove(playerId);
                }
        );
    }

    public static void breathe(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) {
            return;
        }

        if (!ModAttachments.hasEquippedPouch(player)) {
            sendBreatheWarning(
                    player,
                    Component.translatable(
                            "message.stormlight.breathe.no_pouch"
                    )
            );
            return;
        }

        int room = ModAttachments.MAX_PERSONAL_STORMLIGHT
                - ModAttachments.getPersonalStormlight(player);

        if (room <= 0) {
            sendBreatheWarning(
                    player,
                    Component.translatable(
                            "message.stormlight.breathe.full"
                    )
            );
            return;
        }

        int breathed = StormlightPool.drainSpheres(
                player,
                Math.min(room, BREATHE_AMOUNT_PER_TICK)
        );

        if (breathed <= 0) {
            sendBreatheWarning(
                    player,
                    Component.translatable(
                            "message.stormlight.breathe.no_charge"
                    )
            );
            return;
        }

        ModAttachments.setPersonalStormlight(
                player,
                ModAttachments.getPersonalStormlight(player) + breathed
        );

        applyEffects(player);
        spawnBreathingMote(player);
        playBreathingSoundIfDue(player);

        sendStatus(player);
    }

    private static void tick(MinecraftServer server) {
        long gameTime = server.overworld().getGameTime();
        boolean syncNow = gameTime % STATUS_SYNC_INTERVAL_TICKS == 0L;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            int reserve = ModAttachments.getPersonalStormlight(player);

            if (reserve > 0 && player.isAlive() && !player.isSpectator()) {
                if (gameTime % EFFECT_REFRESH_INTERVAL_TICKS == 0L) {
                    applyEffects(player);
                }

                if (gameTime % PARTICLE_INTERVAL_TICKS == 0L) {
                    spawnActiveParticles(player);
                }

                if (gameTime % HEAL_INTERVAL_TICKS == 0L
                        && player.getHealth() < player.getMaxHealth()) {
                    int spent = StormlightPool.drainReserve(
                            player,
                            HEAL_COST_PER_HALF_POINT
                    );

                    if (spent > 0) {
                        player.heal(
                                HEAL_AMOUNT
                                        * spent
                                        / HEAL_COST_PER_HALF_POINT
                        );
                    }
                }

                if (gameTime % PASSIVE_LEAK_INTERVAL_TICKS == 0L) {
                    StormlightPool.drainReserve(
                            player,
                            PASSIVE_LEAK_PER_INTERVAL
                    );
                }
            }

            if (syncNow) {
                syncStatusIfChanged(player);
            }
        }
    }

    private static void applyEffects(ServerPlayer player) {
        addHiddenEffect(player, MobEffects.SPEED);
        addHiddenEffect(player, MobEffects.STRENGTH);
    }

    private static void addHiddenEffect(
            ServerPlayer player,
            Holder<MobEffect> effect
    ) {
        player.addEffect(
                new MobEffectInstance(
                        effect,
                        EFFECT_DURATION_TICKS,
                        1,
                        true,
                        false,
                        false
                )
        );
    }

    private static void spawnBreathingMote(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();

        level.sendParticles(
                ModParticles.SURGE_LIGHT,
                player.getX(),
                player.getY() + 1.0,
                player.getZ(),
                1,
                0.42,
                0.72,
                0.42,
                0.018
        );
    }

    private static void playBreathingSoundIfDue(
            ServerPlayer player
    ) {
        long gameTime = player.level().getGameTime();
        UUID playerId = player.getUUID();
        Long lastTick = LAST_BREATHE_SOUND_TICK.get(playerId);

        if (lastTick != null
                && gameTime - lastTick
                < BREATHE_SOUND_INTERVAL_TICKS) {
            return;
        }

        LAST_BREATHE_SOUND_TICK.put(playerId, gameTime);

        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS,
                0.55F,
                1.35F
        );
    }

    private static void sendBreatheWarning(
            ServerPlayer player,
            Component message
    ) {
        long gameTime = player.level().getGameTime();
        UUID playerId = player.getUUID();
        Long lastTick = LAST_BREATHE_WARNING_TICK.get(playerId);

        if (lastTick != null
                && gameTime - lastTick
                < BREATHE_WARNING_INTERVAL_TICKS) {
            return;
        }

        LAST_BREATHE_WARNING_TICK.put(playerId, gameTime);
        player.sendOverlayMessage(message);
    }

    private static void spawnActiveParticles(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();

        level.sendParticles(
                ModParticles.SURGE_LIGHT,
                player.getX(),
                player.getY() + 0.95,
                player.getZ(),
                PARTICLES_PER_BURST,
                0.38,
                0.7,
                0.38,
                0.008
        );
    }

    private static void sendStatus(ServerPlayer player) {
        StormlightStatusPayload payload = createStatus(player);

        ServerPlayNetworking.send(player, payload);
        LAST_SENT_STATUS.put(player.getUUID(), payload);
    }

    private static void syncStatusIfChanged(ServerPlayer player) {
        StormlightStatusPayload payload = createStatus(player);
        StormlightStatusPayload previous =
                LAST_SENT_STATUS.get(player.getUUID());

        if (!payload.equals(previous)) {
            ServerPlayNetworking.send(player, payload);
            LAST_SENT_STATUS.put(player.getUUID(), payload);
        }
    }

    private static StormlightStatusPayload createStatus(
            ServerPlayer player
    ) {
        return new StormlightStatusPayload(
                ModAttachments.getPersonalStormlight(player),
                ModAttachments.MAX_PERSONAL_STORMLIGHT
        );
    }
}
