package com.scrotey.stormlight.breathing;

import net.minecraft.server.level.ServerPlayer;

/**
 * Hold-to-activate Surge: rapidly heals the player while held, at a
 * steep stormlight cost. Tuned so a full 20 HP heal costs exactly
 * 1000 stormlight over 8 seconds (2.5 HP/sec).
 */
public final class EmergencyHealAbility implements StormlightAbility {
    private static final int HEAL_INTERVAL_TICKS = 4;
    private static final float HEAL_AMOUNT_PER_INTERVAL = 0.5f;
    private static final int COST_PER_HEAL_POINT = 50;
    private static final int COST_PER_INTERVAL =
            Math.round(HEAL_AMOUNT_PER_INTERVAL * COST_PER_HEAL_POINT);

    @Override
    public AbilityId id() {
        return AbilityId.EMERGENCY_HEAL;
    }

    @Override
    public ActivationMode activationMode() {
        return ActivationMode.HOLD;
    }

    @Override
    public boolean tryStart(ServerPlayer player) {
        return player.isAlive()
                && !player.isSpectator()
                && player.getHealth() < player.getMaxHealth()
                && StormlightPool.getTotals(player).charge() > 0;
    }

    @Override
    public boolean tick(ServerPlayer player, long gameTime) {
        if (!player.isAlive() || player.isSpectator()) {
            return false;
        }

        if (player.getHealth() >= player.getMaxHealth()) {
            return false;
        }

        if (gameTime % HEAL_INTERVAL_TICKS != 0) {
            return true;
        }

        if (StormlightPool.getTotals(player).charge() <= 0) {
            return false;
        }

        int drained = StormlightPool.drain(player, COST_PER_INTERVAL);

        if (drained > 0) {
            float healAmount = (float) drained / COST_PER_HEAL_POINT;
            player.heal(Math.min(healAmount, player.getMaxHealth() - player.getHealth()));
        }

        return StormlightPool.getTotals(player).charge() > 0
                && player.getHealth() < player.getMaxHealth();
    }

    @Override
    public void stop(ServerPlayer player) {
        // No lingering effect to clean up.
    }
}
