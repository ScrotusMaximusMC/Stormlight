package com.scrotey.stormlight.client.mixin;

import com.scrotey.stormlight.client.highstorm.ClientHighstormState;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Darkens the fog towards the stormwall's colour while the player looks at
 * the storm. Without it, the furthest terrain fades into the pale sky-blue
 * fog colour and glows as a light band in front of the dark curtain.
 */
@Mixin(FogRenderer.class)
public abstract class StormFogMixin {
    @Unique
    private static final float STORM_FOG_RED = 0.11F;
    @Unique
    private static final float STORM_FOG_GREEN = 0.11F;
    @Unique
    private static final float STORM_FOG_BLUE = 0.125F;
    @Unique
    private static final float MAX_STORM_FOG_BLEND = 0.85F;

    @Inject(
            method = "setupFog",
            at = @At("RETURN")
    )
    private void stormlight$tintFogTowardsStorm(
            Camera camera,
            int renderDistance,
            DeltaTracker deltaTracker,
            float darkness,
            ClientLevel level,
            CallbackInfoReturnable<FogData> callback
    ) {
        if (!level.dimension().equals(Level.OVERWORLD)
                || camera.getFluidInCamera() != FogType.NONE) {
            return;
        }

        Vector3fc forward = camera.forwardVector();

        float horizontalLength =
                (float) Math.sqrt(
                        forward.x() * forward.x()
                                + forward.z() * forward.z()
                );

        float eastFacing =
                horizontalLength > 1.0E-4F
                        ? forward.x() / horizontalLength
                        : 0.0F;

        float blend =
                ClientHighstormState.getStormFogStrength(eastFacing)
                        * MAX_STORM_FOG_BLEND;

        if (blend <= 0.0F) {
            return;
        }

        FogData fog = callback.getReturnValue();

        fog.color.set(
                fog.color.x() + (STORM_FOG_RED - fog.color.x()) * blend,
                fog.color.y() + (STORM_FOG_GREEN - fog.color.y()) * blend,
                fog.color.z() + (STORM_FOG_BLUE - fog.color.z()) * blend,
                fog.color.w()
        );
    }
}
