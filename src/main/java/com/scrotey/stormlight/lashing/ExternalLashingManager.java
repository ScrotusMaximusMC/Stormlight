package com.scrotey.stormlight.lashing;

import com.scrotey.stormlight.attachment.ModAttachments;
import com.scrotey.stormlight.breathing.StormlightManager;
import com.scrotey.stormlight.breathing.StormlightPool;
import com.scrotey.stormlight.network.ExternalLashingTargetPayload;
import com.scrotey.stormlight.particle.ModParticles;
import com.scrotey.stormlight.progression.RadiantOrderRegistry;
import com.scrotey.stormlight.progression.RadiantProgression;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class ExternalLashingManager {
    public static final double TARGETING_RANGE = 24.0;

    private static final double MAX_SELECTED_DISTANCE = 32.0;
    private static final double MAX_TRAVEL_DISTANCE = 100.0;
    private static final double ACCELERATION = 0.08;
    private static final double MAX_SPEED = 2.0;
    private static final int ACTIVATION_COST = 40;
    private static final int MAX_ACTIVE_TICKS = 200;
    private static final int MAX_STALLED_TICKS = 5;
    private static final int PARTICLE_INTERVAL_TICKS = 2;
    private static final int SOUND_INTERVAL_TICKS = 20;

    private static final Map<UUID, SelectedTarget> SELECTED_TARGETS =
            new HashMap<>();
    private static final Map<UUID, ActiveExternalLashing> ACTIVE_LASHINGS =
            new HashMap<>();

    private ExternalLashingManager() {
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(
                ExternalLashingManager::tick
        );

        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) ->
                        sendTargetCleared(handler.getPlayer())
        );

        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) ->
                        SELECTED_TARGETS.remove(
                                handler.getPlayer().getUUID()
                        )
        );

        ServerLifecycleEvents.SERVER_STOPPING.register(
                ExternalLashingManager::stopAll
        );
    }

    public static void handleInput(
            ServerPlayer player,
            boolean cancel
    ) {
        if (cancel) {
            cancelSelection(player, true);
            return;
        }

        if (!canUse(player)) {
            return;
        }

        SelectedTarget selected = SELECTED_TARGETS.get(player.getUUID());

        if (selected == null) {
            selectLookedAtTarget(player);
        } else {
            launchSelectedTarget(player, selected);
        }
    }

    private static boolean canUse(ServerPlayer player) {
        if (!RadiantProgression.hasUnlocked(
                player,
                RadiantOrderRegistry.EXTERNAL_LASHING_LEVEL
        )) {
            player.sendOverlayMessage(
                    Component.translatable(
                            "message.stormlight.external_lashing.locked"
                    )
            );
            return false;
        }

        if (!player.isAlive() || player.isSpectator()) {
            return false;
        }

        if (ModAttachments.getPersonalStormlight(player) <= 0) {
            player.sendOverlayMessage(
                    Component.translatable(
                            "message.stormlight.external_lashing.empty"
                    )
            );
            return false;
        }

        return true;
    }

    private static void selectLookedAtTarget(ServerPlayer player) {
        LivingEntity target = findLookedAtTarget(player);

        if (target == null) {
            player.sendOverlayMessage(
                    Component.translatable(
                            "message.stormlight.external_lashing.no_target"
                    )
            );
            return;
        }

        if (ACTIVE_LASHINGS.containsKey(target.getUUID())) {
            player.sendOverlayMessage(
                    Component.translatable(
                            "message.stormlight.external_lashing.already_lashed"
                    )
            );
            return;
        }

        SELECTED_TARGETS.put(
                player.getUUID(),
                new SelectedTarget(
                        target.getUUID(),
                        target.level().dimension()
                )
        );

        ServerPlayNetworking.send(
                player,
                new ExternalLashingTargetPayload(target.getId())
        );
        player.sendOverlayMessage(
                Component.translatable(
                        "message.stormlight.external_lashing.selected"
                )
        );
        playSelectionSound(target);
    }

    private static void launchSelectedTarget(
            ServerPlayer player,
            SelectedTarget selected
    ) {
        LivingEntity target = resolveTarget(
                player.level().getServer(),
                selected
        );

        if (!isStillValidSelection(player, target, selected)) {
            cancelSelection(player, false);
            player.sendOverlayMessage(
                    Component.translatable(
                            "message.stormlight.external_lashing.target_lost"
                    )
            );
            return;
        }

        if (ACTIVE_LASHINGS.containsKey(target.getUUID())) {
            cancelSelection(player, false);
            player.sendOverlayMessage(
                    Component.translatable(
                            "message.stormlight.external_lashing.already_lashed"
                    )
            );
            return;
        }

        if (ModAttachments.getPersonalStormlight(player) < ACTIVATION_COST) {
            player.sendOverlayMessage(
                    Component.translatable(
                            "message.stormlight.external_lashing.no_stormlight",
                            ACTIVATION_COST
                    )
            );
            return;
        }

        int spent = StormlightPool.drainReserve(player, ACTIVATION_COST);

        if (spent < ACTIVATION_COST) {
            return;
        }

        Vec3 direction = player.getLookAngle().normalize();

        ACTIVE_LASHINGS.put(
                target.getUUID(),
                new ActiveExternalLashing(
                        target.getUUID(),
                        target.level().dimension(),
                        direction,
                        target.position(),
                        target.isNoGravity()
                )
        );

        target.setNoGravity(true);
        target.resetFallDistance();
        cancelSelection(player, false);
        playLaunchSound(target);
        player.sendOverlayMessage(
                Component.translatable(
                        "message.stormlight.external_lashing.launched"
                )
        );
        StormlightManager.refreshStatus(player);
    }

    private static LivingEntity findLookedAtTarget(ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 view = player.getLookAngle();
        Vec3 end = start.add(view.scale(TARGETING_RANGE));
        HitResult blockHit = player.pick(TARGETING_RANGE, 1.0F, false);

        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }

        AABB searchBox = player.getBoundingBox()
                .expandTowards(end.subtract(start))
                .inflate(1.0);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player,
                start,
                end,
                searchBox,
                ExternalLashingManager::isValidTarget,
                start.distanceToSqr(end)
        );

        return hit != null && hit.getEntity() instanceof LivingEntity living
                ? living
                : null;
    }

    private static boolean isValidTarget(Entity entity) {
        return entity instanceof LivingEntity living
                && !(living instanceof Player)
                && living.isAlive()
                && living.isPickable();
    }

    private static boolean isStillValidSelection(
            ServerPlayer player,
            LivingEntity target,
            SelectedTarget selected
    ) {
        return target != null
                && isValidTarget(target)
                && target.level().dimension().equals(selected.dimension())
                && player.level().dimension().equals(selected.dimension())
                && player.distanceToSqr(target)
                <= MAX_SELECTED_DISTANCE * MAX_SELECTED_DISTANCE;
    }

    private static LivingEntity resolveTarget(
            MinecraftServer server,
            SelectedTarget selected
    ) {
        ServerLevel level = server.getLevel(selected.dimension());

        if (level == null) {
            return null;
        }

        Entity entity = level.getEntityInAnyDimension(selected.targetId());
        return entity instanceof LivingEntity living ? living : null;
    }

    private static void tick(MinecraftServer server) {
        tickSelections(server);

        Iterator<Map.Entry<UUID, ActiveExternalLashing>> iterator =
                ACTIVE_LASHINGS.entrySet().iterator();

        while (iterator.hasNext()) {
            ActiveExternalLashing active = iterator.next().getValue();
            LivingEntity target = resolveTarget(
                    server,
                    new SelectedTarget(active.targetId, active.dimension)
            );

            if (target == null || !target.isAlive()) {
                iterator.remove();
                continue;
            }

            if (!target.level().dimension().equals(active.dimension)) {
                finish(target, active);
                iterator.remove();
                continue;
            }

            double moved = target.position().distanceTo(active.lastPosition);
            active.distanceTravelled += moved;
            active.lastPosition = target.position();
            active.ticks++;

            if (active.ticks > 4 && moved < 0.01) {
                active.stalledTicks++;
            } else {
                active.stalledTicks = 0;
            }

            if (active.distanceTravelled >= MAX_TRAVEL_DISTANCE
                    || active.ticks >= MAX_ACTIVE_TICKS
                    || active.stalledTicks >= MAX_STALLED_TICKS) {
                finish(target, active);
                iterator.remove();
                continue;
            }

            applyLashing(target, active.direction);

            if (active.ticks % PARTICLE_INTERVAL_TICKS == 0) {
                spawnParticles(target, active.direction);
            }

            if (active.ticks % SOUND_INTERVAL_TICKS == 0) {
                playFlightSound(target);
            }
        }
    }

    private static void tickSelections(MinecraftServer server) {
        Iterator<Map.Entry<UUID, SelectedTarget>> iterator =
                SELECTED_TARGETS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, SelectedTarget> entry = iterator.next();
            ServerPlayer player = server.getPlayerList()
                    .getPlayer(entry.getKey());

            if (player == null) {
                iterator.remove();
                continue;
            }

            LivingEntity target = resolveTarget(server, entry.getValue());
            boolean usable = player.isAlive()
                    && !player.isSpectator()
                    && RadiantProgression.hasUnlocked(
                            player,
                            RadiantOrderRegistry.EXTERNAL_LASHING_LEVEL
                    )
                    && ModAttachments.getPersonalStormlight(player) > 0;

            if (!usable || !isStillValidSelection(
                    player,
                    target,
                    entry.getValue()
            )) {
                iterator.remove();
                sendTargetCleared(player);
            }
        }
    }

    private static void applyLashing(
            LivingEntity target,
            Vec3 direction
    ) {
        target.setNoGravity(true);
        target.resetFallDistance();

        Vec3 velocity = target.getDeltaMovement();
        double forwardSpeed = Math.max(0.0, velocity.dot(direction));
        Vec3 sideways = velocity
                .subtract(direction.scale(velocity.dot(direction)))
                .scale(0.2);
        double nextSpeed = Math.min(
                MAX_SPEED,
                forwardSpeed + ACCELERATION
        );

        target.setDeltaMovement(
                sideways.add(direction.scale(nextSpeed))
        );
        target.hurtMarked = true;
    }

    private static void finish(
            LivingEntity target,
            ActiveExternalLashing active
    ) {
        target.setNoGravity(active.hadNoGravity);
        target.resetFallDistance();
        target.setDeltaMovement(target.getDeltaMovement().scale(0.35));
        target.hurtMarked = true;
    }

    private static void cancelSelection(
            ServerPlayer player,
            boolean showMessage
    ) {
        SelectedTarget removed = SELECTED_TARGETS.remove(player.getUUID());
        sendTargetCleared(player);

        if (removed != null && showMessage) {
            player.sendOverlayMessage(
                    Component.translatable(
                            "message.stormlight.external_lashing.cancelled"
                    )
            );
        }
    }

    private static void sendTargetCleared(ServerPlayer player) {
        ServerPlayNetworking.send(
                player,
                ExternalLashingTargetPayload.cleared()
        );
    }

    private static void stopAll(MinecraftServer server) {
        for (ActiveExternalLashing active : ACTIVE_LASHINGS.values()) {
            LivingEntity target = resolveTarget(
                    server,
                    new SelectedTarget(active.targetId, active.dimension)
            );

            if (target != null) {
                finish(target, active);
            }
        }

        ACTIVE_LASHINGS.clear();
        SELECTED_TARGETS.clear();
    }

    private static void spawnParticles(
            LivingEntity target,
            Vec3 direction
    ) {
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }

        level.sendParticles(
                ModParticles.SURGE_LIGHT,
                target.getX() - direction.x * 0.35,
                target.getY() + target.getBbHeight() * 0.5
                        - direction.y * 0.35,
                target.getZ() - direction.z * 0.35,
                3,
                target.getBbWidth() * 0.25,
                target.getBbHeight() * 0.2,
                target.getBbWidth() * 0.25,
                0.01
        );
    }

    private static void playSelectionSound(LivingEntity target) {
        target.level().playSound(
                null,
                target.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS,
                0.45F,
                1.55F
        );
    }

    private static void playLaunchSound(LivingEntity target) {
        target.level().playSound(
                null,
                target.blockPosition(),
                SoundEvents.BREEZE_CHARGE,
                SoundSource.PLAYERS,
                0.85F,
                1.05F
        );
    }

    private static void playFlightSound(LivingEntity target) {
        target.level().playSound(
                null,
                target.blockPosition(),
                SoundEvents.ELYTRA_FLYING,
                SoundSource.PLAYERS,
                0.28F,
                1.25F
        );
    }

    private record SelectedTarget(
            UUID targetId,
            ResourceKey<Level> dimension
    ) {
    }

    private static final class ActiveExternalLashing {
        private final UUID targetId;
        private final ResourceKey<Level> dimension;
        private final Vec3 direction;
        private final boolean hadNoGravity;
        private Vec3 lastPosition;
        private double distanceTravelled;
        private int ticks;
        private int stalledTicks;

        private ActiveExternalLashing(
                UUID targetId,
                ResourceKey<Level> dimension,
                Vec3 direction,
                Vec3 lastPosition,
                boolean hadNoGravity
        ) {
            this.targetId = targetId;
            this.dimension = dimension;
            this.direction = direction;
            this.lastPosition = lastPosition;
            this.hadNoGravity = hadNoGravity;
        }
    }
}
