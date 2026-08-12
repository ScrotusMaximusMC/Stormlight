package com.scrotey.stormlight.client.mixin;

import com.scrotey.stormlight.client.StormlightFovFeedback;

import net.minecraft.client.Camera;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraFovMixin {
    @Inject(
            method = "getFov",
            at = @At("RETURN"),
            cancellable = true
    )
    private void stormlight$applyBreathingFov(
            CallbackInfoReturnable<Float> callback
    ) {
        callback.setReturnValue(
                callback.getReturnValue()
                        + StormlightFovFeedback.getFovOffset()
        );
    }
}
