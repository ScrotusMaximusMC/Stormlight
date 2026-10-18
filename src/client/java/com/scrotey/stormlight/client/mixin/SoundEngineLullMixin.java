package com.scrotey.stormlight.client.mixin;

import com.scrotey.stormlight.client.highstorm.ClientHighstormState;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Quietens the world before a Highstorm. Ambient loops and animal sounds
 * fade during the Gathering lull, so the distant thunder carries further.
 * Ticking sounds such as biome ambience recalculate their volume every tick
 * and therefore fade smoothly; one-shot sounds take the volume current when
 * they start.
 */
@Mixin(SoundEngine.class)
public abstract class SoundEngineLullMixin {
    @Inject(
            method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F",
            at = @At("RETURN"),
            cancellable = true
    )
    private void stormlight$quietenBeforeHighstorm(
            SoundInstance sound,
            CallbackInfoReturnable<Float> callback
    ) {
        SoundSource source = sound.getSource();

        if (source != SoundSource.AMBIENT
                && source != SoundSource.NEUTRAL) {
            return;
        }

        float multiplier =
                ClientHighstormState.getAmbientVolumeMultiplier();

        if (multiplier < 1.0F) {
            callback.setReturnValue(
                    callback.getReturnValue() * multiplier
            );
        }
    }
}
