package com.scrotey.stormlight.breathing;

import net.minecraft.core.Holder;
import com.scrotey.stormlight.particle.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Tap-toggle Surge: sustained Speed I + Strength I while active,
 * draining 1 stormlight/sec from carried spheres.
 */
public final class StrengthSurgeAbility implements StormlightAbility {
    private static final int TICKS_PER_SECOND = 20;

    // One unit per second means a full Diamond Chip lasts 10 seconds,
    // while a full Emerald Broam lasts 1,000 seconds.
    private static final int DRAIN_INTERVAL_TICKS = TICKS_PER_SECOND;
    private static final int DRAIN_PER_INTERVAL = 1;

    private static final int EFFECT_REFRESH_INTERVAL_TICKS = 20;
    private static final int EFFECT_DURATION_TICKS = 45;
    private static final int PARTICLE_INTERVAL_TICKS = 3;
    private static final int PARTICLES_PER_BURST = 4;

    @Override
    public AbilityId id() {
        return AbilityId.STRENGTH_SURGE;
    }

    @Override
    public ActivationMode activationMode() {
        return ActivationMode.TOGGLE;
    }

    @Override
    public boolean tryStart(ServerPlayer player) {
        boolean canStart = player.isAlive()
                && !player.isSpectator()
                && StormlightPool.getTotals(player).charge() > 0;

        if (canStart) {
            applyEffects(player);
        }

        return canStart;
    }

    @Override
    public boolean tick(ServerPlayer player, long gameTime) {
        if (!player.isAlive() || player.isSpectator()) {
            return false;
        }

        if (gameTime % EFFECT_REFRESH_INTERVAL_TICKS == 0) {
            applyEffects(player);
        }

        if (gameTime % PARTICLE_INTERVAL_TICKS == 0) {
            spawnParticles(player);
        }

        if (gameTime % DRAIN_INTERVAL_TICKS == 0) {
            StormlightPool.drain(player, DRAIN_PER_INTERVAL);

            if (StormlightPool.getTotals(player).charge() <= 0) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void stop(ServerPlayer player) {
        // Effects are hidden/short-duration and simply expire naturally
        // (within EFFECT_DURATION_TICKS) once no longer refreshed.
    }

    private void applyEffects(ServerPlayer player) {
        addHiddenEffect(player, MobEffects.SPEED);
        addHiddenEffect(player, MobEffects.STRENGTH);
    }

    private void addHiddenEffect(ServerPlayer player, Holder<MobEffect> effect) {
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

    private void spawnParticles(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();

        level.sendParticles(
                ModParticles.SURGE_LIGHT,
                player.getX(),
                player.getY() + 0.95,
                player.getZ(),
                PARTICLES_PER_BURST,
                0.42,
                0.78,
                0.42,
                0.012
        );
    }
}
