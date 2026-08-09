package com.scrotey.stormlight.client.mixin;

import com.scrotey.stormlight.client.highstorm.ClientHighstormState;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CloudRenderer.class)
public abstract class CloudRendererMixin {
    /*
     * Prevent vanilla's bright cloud layer from intersecting the
     * custom Highstorm cloud mass.
     */
    @Inject(
            method = "render",
            at = @At("HEAD"),
            cancellable = true
    )
    private void stormlight$hideVanillaClouds(
            int color,
            CloudStatus cloudStatus,
            float bottomY,
            int range,
            Vec3 cameraPosition,
            long gameTime,
            float partialTicks,
            CallbackInfo callback
    ) {
        if (ClientHighstormState.isStormfrontVisible()) {
            callback.cancel();
        }
    }
}