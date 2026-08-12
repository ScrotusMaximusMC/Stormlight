package com.scrotey.stormlight.surefoot;

import com.scrotey.stormlight.attachment.ModAttachments;
import com.scrotey.stormlight.breathing.StormlightManager;
import com.scrotey.stormlight.breathing.StormlightPool;
import com.scrotey.stormlight.particle.ModParticles;
import com.scrotey.stormlight.progression.RadiantOrderRegistry;
import com.scrotey.stormlight.progression.RadiantProgression;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class SurefootManager {
    /*
     * Minecraft damage is measured in half-hearts, so this is
     * 10 Stormlight for every half-heart of fall damage prevented.
     */
    private static final int STORMLIGHT_PER_DAMAGE_POINT = 10;
    private static final double COST_ROUNDING_EPSILON = 1.0E-5;
    private static final int IMPACT_PARTICLES = 12;

    private SurefootManager() {
    }

    public static float protectFromFall(
            ServerPlayer player,
            float proposedHealth
    ) {
        if (!player.isAlive()
                || player.isSpectator()
                || !RadiantProgression.hasUnlocked(
                player,
                RadiantOrderRegistry.FALL_PROTECTION_LEVEL
        )) {
            return proposedHealth;
        }

        float currentHealth = player.getHealth();
        float incomingDamage = currentHealth - proposedHealth;
        int reserve = ModAttachments.getPersonalStormlight(player);

        if (incomingDamage <= 0.0F || reserve <= 0) {
            return proposedHealth;
        }

        int fullProtectionCost = Math.max(
                1,
                (int) Math.ceil(
                        incomingDamage * STORMLIGHT_PER_DAMAGE_POINT
                                - COST_ROUNDING_EPSILON
                )
        );
        int stormlightSpent = Math.min(reserve, fullProtectionCost);
        float preventedDamage;

        if (stormlightSpent >= fullProtectionCost) {
            preventedDamage = incomingDamage;
        } else {
            preventedDamage = Math.min(
                    incomingDamage,
                    stormlightSpent
                            / (float) STORMLIGHT_PER_DAMAGE_POINT
            );
        }

        StormlightPool.drainReserve(player, stormlightSpent);
        spawnImpactRing(player, stormlightSpent >= fullProtectionCost);
        playImpactSound(player, stormlightSpent >= fullProtectionCost);
        StormlightManager.refreshStatus(player);

        return Math.min(
                currentHealth,
                proposedHealth + preventedDamage
        );
    }

    private static void spawnImpactRing(
            ServerPlayer player,
            boolean fullyProtected
    ) {
        ServerLevel level = (ServerLevel) player.level();
        double speed = fullyProtected ? 0.19 : 0.13;

        for (int particle = 0;
             particle < IMPACT_PARTICLES;
             particle++) {
            double angle = particle
                    * Math.PI
                    * 2.0
                    / IMPACT_PARTICLES;

            level.sendParticles(
                    ModParticles.SURGE_LIGHT,
                    player.getX(),
                    player.getY() + 0.12,
                    player.getZ(),
                    0,
                    Math.cos(angle) * speed,
                    0.035,
                    Math.sin(angle) * speed,
                    1.0
            );
        }
    }

    private static void playImpactSound(
            ServerPlayer player,
            boolean fullyProtected
    ) {
        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.SHIELD_BLOCK.value(),
                SoundSource.PLAYERS,
                fullyProtected ? 0.75F : 0.5F,
                fullyProtected ? 1.35F : 1.05F
        );

        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS,
                fullyProtected ? 0.55F : 0.36F,
                fullyProtected ? 1.1F : 0.82F
        );
    }
}
