package com.scrotey.stormlight.mixin;

import com.scrotey.stormlight.surefoot.SurefootManager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class LivingEntitySurefootMixin {
    @Unique
    private boolean stormlight$surefootFallDamage;

    @Inject(
            method = "actuallyHurt",
            at = @At("HEAD")
    )
    private void stormlight$captureFallDamage(
            ServerLevel level,
            DamageSource source,
            float amount,
            CallbackInfo callback
    ) {
        stormlight$surefootFallDamage = source.is(DamageTypes.FALL);
    }

    /*
     * This setHealth call receives damage after vanilla armour,
     * enchantment and absorption calculations have already run.
     */
    @ModifyArg(
            method = "actuallyHurt",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;setHealth(F)V"
            ),
            index = 0
    )
    private float stormlight$applySurefoot(float proposedHealth) {
        if (!stormlight$surefootFallDamage
                || !((Object) this instanceof ServerPlayer player)) {
            return proposedHealth;
        }

        return SurefootManager.protectFromFall(
                player,
                proposedHealth
        );
    }

    @Inject(
            method = "actuallyHurt",
            at = @At("RETURN")
    )
    private void stormlight$clearFallDamage(
            ServerLevel level,
            DamageSource source,
            float amount,
            CallbackInfo callback
    ) {
        stormlight$surefootFallDamage = false;
    }
}
