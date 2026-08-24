package com.scrotey.stormlight.lashing;

import com.scrotey.stormlight.attachment.ModAttachments;
import com.scrotey.stormlight.breathing.StormlightManager;
import com.scrotey.stormlight.breathing.StormlightPool;
import com.scrotey.stormlight.network.LashingStatePayload;
import com.scrotey.stormlight.particle.ModParticles;
import com.scrotey.stormlight.progression.RadiantOrderRegistry;
import com.scrotey.stormlight.progression.RadiantProgression;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LashingManager {
    private static final int ACTIVATION_COST = 10;

    /* Regular Lashing: one Stormlight every two ticks = 10/second. */
    private static final int LASHING_DRAIN_INTERVAL_TICKS = 2;
    private static final int LASHING_DRAIN_PER_INTERVAL = 1;

    /* Stabilisation: one Stormlight every tick = 20/second. */
    private static final int STABILISATION_DRAIN_INTERVAL_TICKS = 1;
    private static final int STABILISATION_DRAIN_PER_INTERVAL = 1;

    private static final int MINIMUM_START_RESERVE =
            ACTIVATION_COST + LASHING_DRAIN_PER_INTERVAL;
    private static final int HOLD_THRESHOLD_TICKS = 5;

    private static final double GRAVITY_ACCELERATION = 0.08;
    private static final double STABILISATION_DECELERATION = 0.25;
    private static final double STOPPED_SPEED = 0.025;

    private static final int PARTICLE_INTERVAL_TICKS = 2;
    private static final int SOUND_INTERVAL_TICKS = 20;

    private static final Map<UUID, ActiveLashing>
            ACTIVE_LASHINGS = new HashMap<>();
    private static final Map<UUID, KeyPressState>
            KEY_PRESSES = new HashMap<>();
    private static final Map<UUID, StabilisationState>
            STABILISATIONS = new HashMap<>();

    private LashingManager() {
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(
                LashingManager::tick
        );

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            ACTIVE_LASHINGS.clear();
            KEY_PRESSES.clear();
            STABILISATIONS.clear();
        });

        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) ->
                        ServerPlayNetworking.send(
                                handler.getPlayer(),
                                LashingStatePayload.inactive()
                        )
        );

        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> cleanupPlayer(handler.getPlayer())
        );
    }

    public static boolean isLashing(ServerPlayer player) {
        return ACTIVE_LASHINGS.containsKey(player.getUUID());
    }

    public static boolean isStabilising(ServerPlayer player) {
        return STABILISATIONS.containsKey(player.getUUID());
    }

    public static void handleInput(
            ServerPlayer player,
            boolean pressed
    ) {
        UUID playerId = player.getUUID();

        if (pressed) {
            KEY_PRESSES.putIfAbsent(
                    playerId,
                    new KeyPressState(player.level().getGameTime())
            );
            return;
        }

        KeyPressState press = KEY_PRESSES.remove(playerId);
        StabilisationState stabilisation =
                STABILISATIONS.remove(playerId);

        if (stabilisation != null) {
            finishStabilisation(player, stabilisation);
            return;
        }

        if (press != null && !press.stabilisationAttempted) {
            toggle(player);
        }
    }

    private static void toggle(ServerPlayer player) {
        if (ACTIVE_LASHINGS.containsKey(player.getUUID())) {
            deactivate(player, "message.stormlight.lashing.deactivated");
            return;
        }

        if (!canUseMovementPowers(player)) {
            return;
        }

        if (!RadiantProgression.hasUnlocked(
                player,
                RadiantOrderRegistry.HORIZONTAL_LASHING_LEVEL
        )) {
            player.sendOverlayMessage(
                    Component.translatable(
                            "message.stormlight.lashing.locked"
                    )
            );
            return;
        }

        int reserve = ModAttachments.getPersonalStormlight(player);

        if (reserve < MINIMUM_START_RESERVE) {
            player.sendOverlayMessage(
                    Component.translatable(
                            "message.stormlight.lashing.no_stormlight",
                            MINIMUM_START_RESERVE
                    )
            );
            return;
        }

        boolean fullLashing = RadiantProgression.hasUnlocked(
                player,
                RadiantOrderRegistry.VERTICAL_LASHING_LEVEL
        );
        LashingDirection direction = fullLashing
                ? LashingDirection.fromLook(player.getLookAngle())
                : LashingDirection.horizontalFromYaw(player.getYRot());

        StormlightPool.drainReserve(player, ACTIVATION_COST);

        ActiveLashing active = new ActiveLashing(
                direction,
                player.level().dimension(),
                player.isNoGravity()
        );

        ACTIVE_LASHINGS.put(player.getUUID(), active);
        player.setNoGravity(true);
        sendState(player, active, false);

        player.sendOverlayMessage(
                Component.translatable(
                        fullLashing
                                ? "message.stormlight.lashing.full_activated"
                                : "message.stormlight.lashing.activated"
                )
        );

        playActivationSound(player);
        StormlightManager.refreshStatus(player);
    }

    private static void tick(MinecraftServer server) {
        long gameTime = server.overworld().getGameTime();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tryStartHeldStabilisation(player, gameTime);

            StabilisationState stabilisation =
                    STABILISATIONS.get(player.getUUID());

            if (stabilisation != null) {
                tickStabilisation(player, stabilisation, gameTime);
                continue;
            }

            ActiveLashing active = ACTIVE_LASHINGS.get(player.getUUID());

            if (active == null) {
                continue;
            }

            if (!canContinueLashing(player, active)) {
                deactivate(player, "message.stormlight.lashing.deactivated");
                continue;
            }

            if (gameTime % LASHING_DRAIN_INTERVAL_TICKS == 0L) {
                int spent = StormlightPool.drainReserve(
                        player,
                        LASHING_DRAIN_PER_INTERVAL
                );

                if (spent < LASHING_DRAIN_PER_INTERVAL
                        || ModAttachments.getPersonalStormlight(player) <= 0) {
                    deactivate(player, "message.stormlight.lashing.depleted");
                    StormlightManager.refreshStatus(player);
                    continue;
                }
            }

            applyLashingGravity(player, active.direction());

            if (gameTime % PARTICLE_INTERVAL_TICKS == 0L) {
                spawnLashingParticles(player, active.direction());
            }

            if (gameTime % SOUND_INTERVAL_TICKS == 0L) {
                playWindSound(player);
            }
        }
    }

    private static void tryStartHeldStabilisation(
            ServerPlayer player,
            long gameTime
    ) {
        UUID playerId = player.getUUID();
        KeyPressState press = KEY_PRESSES.get(playerId);

        if (press == null
                || press.stabilisationAttempted
                || gameTime - press.pressedAt < HOLD_THRESHOLD_TICKS
                || !RadiantProgression.hasUnlocked(
                player,
                RadiantOrderRegistry.VERTICAL_LASHING_LEVEL
        )) {
            return;
        }

        press.stabilisationAttempted = true;

        if (!canUseMovementPowers(player)) {
            return;
        }

        if (ModAttachments.getPersonalStormlight(player) <= 0) {
            player.sendOverlayMessage(
                    Component.translatable(
                            "message.stormlight.stabilisation.no_stormlight"
                    )
            );
            return;
        }

        ActiveLashing active = ACTIVE_LASHINGS.get(playerId);
        StabilisationState stabilisation = new StabilisationState(
                active != null,
                active != null
                        ? active.hadNoGravity()
                        : player.isNoGravity(),
                player.level().dimension()
        );

        STABILISATIONS.put(playerId, stabilisation);
        player.setNoGravity(true);
        sendState(player, active, true);
        player.sendOverlayMessage(
                Component.translatable(
                        "message.stormlight.stabilisation.engaged"
                )
        );
        playStabilisationSound(player);
    }

    private static void tickStabilisation(
            ServerPlayer player,
            StabilisationState stabilisation,
            long gameTime
    ) {
        if (!canContinueStabilisation(player, stabilisation)) {
            stopAllMovementPowers(
                    player,
                    "message.stormlight.lashing.deactivated"
            );
            return;
        }

        if (gameTime % STABILISATION_DRAIN_INTERVAL_TICKS == 0L) {
            int spent = StormlightPool.drainReserve(
                    player,
                    STABILISATION_DRAIN_PER_INTERVAL
            );

            if (spent < STABILISATION_DRAIN_PER_INTERVAL
                    || ModAttachments.getPersonalStormlight(player) <= 0) {
                stopAllMovementPowers(
                        player,
                        "message.stormlight.stabilisation.depleted"
                );
                StormlightManager.refreshStatus(player);
                return;
            }
        }

        applyStabilisation(player);

        if (gameTime % PARTICLE_INTERVAL_TICKS == 0L) {
            spawnStabilisationParticles(player);
        }

        if (gameTime % SOUND_INTERVAL_TICKS == 0L) {
            playStabilisationSound(player);
        }
    }

    private static boolean canUseMovementPowers(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) {
            return false;
        }

        if (player.isPassenger()
                || player.isSleeping()
                || player.isFallFlying()) {
            player.sendOverlayMessage(
                    Component.translatable(
                            "message.stormlight.lashing.unavailable"
                    )
            );
            return false;
        }

        return true;
    }

    private static boolean canContinueLashing(
            ServerPlayer player,
            ActiveLashing active
    ) {
        return player.isAlive()
                && !player.isSpectator()
                && !player.isPassenger()
                && !player.isSleeping()
                && !player.isFallFlying()
                && player.level().dimension().equals(active.dimension())
                && RadiantProgression.hasUnlocked(
                player,
                RadiantOrderRegistry.HORIZONTAL_LASHING_LEVEL
        )
                && ModAttachments.getPersonalStormlight(player) > 0;
    }

    private static boolean canContinueStabilisation(
            ServerPlayer player,
            StabilisationState stabilisation
    ) {
        return player.isAlive()
                && !player.isSpectator()
                && !player.isPassenger()
                && !player.isSleeping()
                && !player.isFallFlying()
                && player.level().dimension().equals(stabilisation.dimension())
                && RadiantProgression.hasUnlocked(
                player,
                RadiantOrderRegistry.VERTICAL_LASHING_LEVEL
        )
                && ModAttachments.getPersonalStormlight(player) > 0;
    }

    private static void applyLashingGravity(
            ServerPlayer player,
            LashingDirection direction
    ) {
        player.setNoGravity(true);

        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(
                velocity.add(
                        direction.x() * GRAVITY_ACCELERATION,
                        direction.y() * GRAVITY_ACCELERATION,
                        direction.z() * GRAVITY_ACCELERATION
                )
        );
        player.hurtMarked = true;
    }

    private static void applyStabilisation(ServerPlayer player) {
        player.setNoGravity(true);
        player.fallDistance = 0.0F;

        Vec3 velocity = player.getDeltaMovement();
        double speed = velocity.length();

        if (speed <= STABILISATION_DECELERATION
                || speed <= STOPPED_SPEED) {
            player.setDeltaMovement(Vec3.ZERO);
        } else {
            player.setDeltaMovement(
                    velocity.scale(
                            (speed - STABILISATION_DECELERATION) / speed
                    )
            );
        }

        player.hurtMarked = true;
    }

    private static void finishStabilisation(
            ServerPlayer player,
            StabilisationState stabilisation
    ) {
        ActiveLashing active = ACTIVE_LASHINGS.get(player.getUUID());

        if (stabilisation.hadActiveLashing() && active != null) {
            player.setNoGravity(true);
            sendState(player, active, false);
        } else {
            player.setNoGravity(stabilisation.hadNoGravity());
            ServerPlayNetworking.send(player, LashingStatePayload.inactive());
        }
    }

    private static void deactivate(
            ServerPlayer player,
            String messageKey
    ) {
        ActiveLashing active = ACTIVE_LASHINGS.remove(player.getUUID());

        if (active == null) {
            return;
        }

        player.setNoGravity(active.hadNoGravity());
        ServerPlayNetworking.send(player, LashingStatePayload.inactive());
        player.sendOverlayMessage(Component.translatable(messageKey));
        playReleaseSound(player);
    }

    private static void stopAllMovementPowers(
            ServerPlayer player,
            String messageKey
    ) {
        UUID playerId = player.getUUID();
        StabilisationState stabilisation = STABILISATIONS.remove(playerId);
        ActiveLashing active = ACTIVE_LASHINGS.remove(playerId);

        if (active != null) {
            player.setNoGravity(active.hadNoGravity());
        } else if (stabilisation != null) {
            player.setNoGravity(stabilisation.hadNoGravity());
        } else {
            player.setNoGravity(false);
        }

        ServerPlayNetworking.send(player, LashingStatePayload.inactive());
        player.sendOverlayMessage(Component.translatable(messageKey));
        playReleaseSound(player);
    }

    private static void cleanupPlayer(ServerPlayer player) {
        UUID playerId = player.getUUID();
        KEY_PRESSES.remove(playerId);
        StabilisationState stabilisation = STABILISATIONS.remove(playerId);
        ActiveLashing active = ACTIVE_LASHINGS.remove(playerId);

        if (active != null) {
            player.setNoGravity(active.hadNoGravity());
        } else if (stabilisation != null) {
            player.setNoGravity(stabilisation.hadNoGravity());
        }
    }

    private static void sendState(
            ServerPlayer player,
            ActiveLashing active,
            boolean stabilising
    ) {
        if (active == null) {
            ServerPlayNetworking.send(
                    player,
                    LashingStatePayload.stabilisingWithoutLashing()
            );
            return;
        }

        ServerPlayNetworking.send(
                player,
                LashingStatePayload.active(
                        active.direction(),
                        stabilising
                )
        );
    }

    private static void spawnLashingParticles(
            ServerPlayer player,
            LashingDirection direction
    ) {
        ServerLevel level = (ServerLevel) player.level();

        for (int particle = -1; particle <= 1; particle++) {
            level.sendParticles(
                    ModParticles.SURGE_LIGHT,
                    player.getX() - direction.x() * 0.35,
                    player.getY() + 0.9
                            - direction.y() * 0.35
                            + particle * 0.18,
                    player.getZ() - direction.z() * 0.35,
                    0,
                    -direction.x() * 0.13,
                    -direction.y() * 0.13 + particle * 0.01,
                    -direction.z() * 0.13,
                    1.0
            );
        }
    }

    private static void spawnStabilisationParticles(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();

        for (int particle = 0; particle < 8; particle++) {
            double angle = particle * Math.PI * 2.0 / 8.0;

            level.sendParticles(
                    ModParticles.SURGE_LIGHT,
                    player.getX() + Math.cos(angle) * 0.8,
                    player.getY() + 0.9
                            + (particle % 3 - 1) * 0.25,
                    player.getZ() + Math.sin(angle) * 0.8,
                    0,
                    -Math.cos(angle) * 0.11,
                    0.0,
                    -Math.sin(angle) * 0.11,
                    1.0
            );
        }
    }

    private static void playActivationSound(ServerPlayer player) {
        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.BREEZE_CHARGE,
                SoundSource.PLAYERS,
                0.85F,
                0.92F
        );

        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS,
                0.4F,
                1.35F
        );
    }

    private static void playWindSound(ServerPlayer player) {
        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.ELYTRA_FLYING,
                SoundSource.PLAYERS,
                0.32F,
                1.18F
        );
    }

    private static void playStabilisationSound(ServerPlayer player) {
        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.BREEZE_INHALE,
                SoundSource.PLAYERS,
                0.5F,
                0.68F
        );
    }

    private static void playReleaseSound(ServerPlayer player) {
        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS,
                0.45F,
                0.72F
        );
    }

    private record ActiveLashing(
            LashingDirection direction,
            ResourceKey<Level> dimension,
            boolean hadNoGravity
    ) {
    }

    private record StabilisationState(
            boolean hadActiveLashing,
            boolean hadNoGravity,
            ResourceKey<Level> dimension
    ) {
    }

    private static final class KeyPressState {
        private final long pressedAt;
        private boolean stabilisationAttempted;

        private KeyPressState(long pressedAt) {
            this.pressedAt = pressedAt;
        }
    }
}
