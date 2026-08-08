package com.scrotey.stormlight.breathing;

import net.minecraft.server.level.ServerPlayer;

/**
 * A single Stormlight-powered capability. Implementations are
 * self-contained: each owns its own activation checks, per-tick
 * cadence gating, drain amounts and effects. Adding a new ability
 * later is a matter of writing one new implementation and registering
 * it in {@link StormlightManager#ABILITIES} - no other plumbing needs
 * to change.
 */
public interface StormlightAbility {
    enum ActivationMode {
        /** Started and stopped by independent toggle requests. */
        TOGGLE,
        /** Active only while the triggering input is held down. */
        HOLD
    }

    AbilityId id();

    ActivationMode activationMode();

    /**
     * Attempt to start the ability now. Returns false if preconditions
     * fail (no charge, dead, spectator, already at some limit, etc.),
     * in which case the manager will not mark it active and will not
     * call {@link #stop}.
     */
    boolean tryStart(ServerPlayer player);

    /**
     * Called once per server tick while active. Returns false to
     * request the manager deactivate this ability now (out of charge,
     * player invalid, goal already met, etc.) - the manager will then
     * call {@link #stop}.
     */
    boolean tick(ServerPlayer player, long gameTime);

    /** Called exactly once when the ability transitions to inactive. */
    void stop(ServerPlayer player);
}
