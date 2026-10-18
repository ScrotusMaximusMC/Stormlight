package com.scrotey.stormlight.spren;

import com.scrotey.stormlight.network.SprenShieldBlockPayload;
import com.scrotey.stormlight.progression.SprenBondProgression;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Calls a bonded spren to guard after the first hostile hit. */
public final class SprenShieldManager {
    private static final Map<UUID, Guard> GUARDS = new HashMap<>();
    private static final int THREAT_GRACE_TICKS = 40;
    private static final int THREAT_CHECK_INTERVAL = 10;
    private static final double THREAT_RADIUS = 48.0;

    private SprenShieldManager() {
    }

    public static void initialize() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(
                (entity, source, amount) -> {
                    if (!(entity instanceof ServerPlayer player)
                            || !(source.getEntity() instanceof Enemy)
                            || amount <= 0) {
                        return true;
                    }
                    return allowHostileDamage(player, source);
                });
        ServerLivingEntityEvents.AFTER_DAMAGE.register(
                (entity, source, baseDamage, damageTaken, blocked) -> {
                    if (damageTaken > 0 && !blocked
                            && entity instanceof ServerPlayer player
                            && source.getEntity() instanceof Enemy) {
                        onWounded(player);
                    }
                });
        ServerTickEvents.END_SERVER_TICK.register(SprenShieldManager::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                GUARDS.remove(handler.getPlayer().getUUID()));
    }

    private static void onWounded(ServerPlayer player) {
        if (SprenBondProgression.getLevel(player) < 2
                || ShardbladeManager.isSummoned(player)) {
            return;
        }
        SprenEntity spren = SprenManager.getFor(player);
        if (spren == null) {
            return;
        }
        UUID playerId = player.getUUID();
        Guard guard = GUARDS.get(playerId);
        long now = player.level().getGameTime();
        if (guard == null || !guard.sprenUuid.equals(spren.getUUID())) {
            SprenSeekManager.cancel(player);
            spren.startGuard();
            GUARDS.put(playerId, new Guard(spren.getUUID(), now));
        } else {
            guard.lastThreatTick = now;
        }
    }

    private static boolean allowHostileDamage(ServerPlayer player,
            DamageSource source) {
        if (ShardbladeManager.isSummoned(player)) return true;
        Guard guard = GUARDS.get(player.getUUID());
        if (guard == null || SprenBondProgression.getLevel(player) < 2) {
            return true; // The warning hit is never blocked.
        }
        SprenEntity spren = SprenManager.getFor(player);
        if (spren == null || !guard.sprenUuid.equals(spren.getUUID())) {
            return true;
        }
        long now = player.level().getGameTime();
        guard.lastThreatTick = now;
        if (!spren.isGuardReady(player)
                || !player.getRandom().nextBoolean()) {
            return true;
        }
        if (source.getDirectEntity() == source.getEntity()
                && source.getDirectEntity() instanceof Mob attacker) {
            Vec3 away = attacker.position().subtract(player.position());
            away = new Vec3(away.x, 0, away.z).normalize();
            if (away.lengthSqr() < 0.01) {
                Vec3 look = player.getLookAngle();
                away = new Vec3(look.x, 0, look.z).normalize();
            }
            attacker.push(away.x * 0.9, 0.2, away.z * 0.9);
        }
        ServerPlayNetworking.send(player, SprenShieldBlockPayload.INSTANCE);
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 1.3F, 1.15F);
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.AMETHYST_CLUSTER_HIT,
                SoundSource.PLAYERS, 0.7F, 1.55F);
        return false;
    }

    public static void cancel(ServerPlayer player) {
        Guard removed = GUARDS.remove(player.getUUID());
        if (removed == null) return;
        SprenEntity spren = SprenManager.getFor(player);
        if (spren != null) spren.stopGuard();
    }

    private static void tick(MinecraftServer server) {
        if (server.overworld().getGameTime() % THREAT_CHECK_INTERVAL != 0) {
            return;
        }
        Iterator<Map.Entry<UUID, Guard>> iterator =
                GUARDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Guard> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(
                    entry.getKey());
            Guard guard = entry.getValue();
            if (player == null || !player.isAlive()
                    || SprenBondProgression.getLevel(player) < 2) {
                if (player != null) {
                    SprenEntity spren = SprenManager.getFor(player);
                    if (spren != null) spren.stopGuard();
                }
                iterator.remove();
                continue;
            }
            SprenEntity spren = SprenManager.getFor(player);
            if (spren == null || !spren.getUUID().equals(guard.sprenUuid)) {
                iterator.remove();
                continue;
            }
            AABB area = player.getBoundingBox().inflate(THREAT_RADIUS);
            boolean targeted = !player.level().getEntitiesOfClass(
                    Mob.class, area,
                    mob -> mob instanceof Enemy && mob.isAlive()
                            && mob.getTarget() == player
            ).isEmpty();
            if (targeted) {
                guard.lastThreatTick = player.level().getGameTime();
            } else if (player.level().getGameTime()
                    - guard.lastThreatTick >= THREAT_GRACE_TICKS) {
                spren.stopGuard();
                iterator.remove();
            }
        }
    }

    private static final class Guard {
        private final UUID sprenUuid;
        private long lastThreatTick;

        private Guard(UUID sprenUuid, long lastThreatTick) {
            this.sprenUuid = sprenUuid;
            this.lastThreatTick = lastThreatTick;
        }
    }
}
