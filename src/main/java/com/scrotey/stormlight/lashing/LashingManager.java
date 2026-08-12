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
    private static final int DRAIN_INTERVAL_TICKS = 2;
    private static final int DRAIN_PER_INTERVAL = 1;
    private static final int MINIMUM_START_RESERVE =
            ACTIVATION_COST + DRAIN_PER_INTERVAL;
    private static final double GRAVITY_ACCELERATION = 0.08;

    private static final int PARTICLE_INTERVAL_TICKS = 2;
    private static final int SOUND_INTERVAL_TICKS = 20;

    private static final Map<UUID, ActiveLashing>
            ACTIVE_LASHINGS = new HashMap<>();

    private LashingManager() {
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(
                LashingManager::tick
        );

        ServerLifecycleEvents.SERVER_STOPPED.register(
                server -> ACTIVE_LASHINGS.clear()
        );

        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) ->
                        ServerPlayNetworking.send(
                                handler.getPlayer(),
                                LashingStatePayload.inactive()
                        )
        );

        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> {
                    ServerPlayer player = handler.getPlayer();
                    ActiveLashing active = ACTIVE_LASHINGS.remove(
                            player.getUUID()
                    );

                    if (active != null) {
                        player.setNoGravity(active.hadNoGravity());
                    }
                }
        );
    }

    public static void toggle(ServerPlayer player) {
        if (ACTIVE_LASHINGS.containsKey(player.getUUID())) {
            deactivate(player, "message.stormlight.lashing.deactivated");
            return;
        }

        if (!player.isAlive() || player.isSpectator()) {
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

        if (player.isPassenger()
                || player.isSleeping()
                || player.isFallFlying()) {
            player.sendOverlayMessage(
                    Component.translatable(
                            "message.stormlight.lashing.unavailable"
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

        LashingDirection direction =
                LashingDirection.fromYaw(player.getYRot());

        StormlightPool.drainReserve(player, ACTIVATION_COST);

        ActiveLashing active = new ActiveLashing(
                direction,
                player.level().dimension(),
                player.isNoGravity()
        );

        ACTIVE_LASHINGS.put(player.getUUID(), active);
        player.setNoGravity(true);

        ServerPlayNetworking.send(
                player,
                new LashingStatePayload(
                        true,
                        (float) direction.x(),
                        (float) direction.z()
                )
        );

        player.sendOverlayMessage(
                Component.translatable(
                        "message.stormlight.lashing.activated"
                )
        );

        playActivationSound(player);
        StormlightManager.refreshStatus(player);
    }

    private static void tick(MinecraftServer server) {
        long gameTime = server.overworld().getGameTime();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ActiveLashing active = ACTIVE_LASHINGS.get(player.getUUID());

            if (active == null) {
                continue;
            }

            if (!canContinue(player, active)) {
                deactivate(player, "message.stormlight.lashing.deactivated");
                continue;
            }

            if (gameTime % DRAIN_INTERVAL_TICKS == 0L) {
                int spent = StormlightPool.drainReserve(
                        player,
                        DRAIN_PER_INTERVAL
                );

                if (spent < DRAIN_PER_INTERVAL
                        || ModAttachments.getPersonalStormlight(player) <= 0) {
                    deactivate(player, "message.stormlight.lashing.depleted");
                    StormlightManager.refreshStatus(player);
                    continue;
                }
            }

            applyGravity(player, active.direction());

            if (gameTime % PARTICLE_INTERVAL_TICKS == 0L) {
                spawnLashingParticles(player, active.direction());
            }

            if (gameTime % SOUND_INTERVAL_TICKS == 0L) {
                playWindSound(player);
            }
        }
    }

    private static boolean canContinue(
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

    private static void applyGravity(
            ServerPlayer player,
            LashingDirection direction
    ) {
        player.setNoGravity(true);

        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(
                velocity.add(
                        direction.x() * GRAVITY_ACCELERATION,
                        0.0,
                        direction.z() * GRAVITY_ACCELERATION
                )
        );
        player.hurtMarked = true;
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

        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS,
                0.45F,
                0.72F
        );
    }

    private static void spawnLashingParticles(
            ServerPlayer player,
            LashingDirection direction
    ) {
        ServerLevel level = (ServerLevel) player.level();
        double perpendicularX = -direction.z();
        double perpendicularZ = direction.x();

        for (int particle = -1; particle <= 1; particle++) {
            double spread = particle * 0.16;

            level.sendParticles(
                    ModParticles.SURGE_LIGHT,
                    player.getX() - direction.x() * 0.35
                            + perpendicularX * spread,
                    player.getY() + 0.9 + particle * 0.18,
                    player.getZ() - direction.z() * 0.35
                            + perpendicularZ * spread,
                    0,
                    -direction.x() * 0.13,
                    0.01 * particle,
                    -direction.z() * 0.13,
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

    private record ActiveLashing(
            LashingDirection direction,
            ResourceKey<Level> dimension,
            boolean hadNoGravity
    ) {
    }
}
