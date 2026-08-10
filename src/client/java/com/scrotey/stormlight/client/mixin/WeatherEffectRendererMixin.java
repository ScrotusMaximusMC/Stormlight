package com.scrotey.stormlight.client.mixin;

import com.scrotey.stormlight.client.highstorm.ClientHighstormState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Makes Highstorm precipitation render as westward-driving rain everywhere.
 *
 * Minecraft renders rain as camera-facing quads rather than ordinary
 * particles. Changing particle velocity therefore cannot tilt the visible
 * rain. This mixin changes only the X coordinate of vanilla rain vertices
 * while their batch is being built. During Approach, Highstorm and Passing,
 * biome precipitation is also treated as rain so snowy and normally dry
 * biomes join the same storm. Vanilla biome precipitation remains untouched
 * outside those phases.
 */
@Mixin(WeatherEffectRenderer.class)
public abstract class WeatherEffectRendererMixin {
    private static final float APPROACH_MAX_ANGLE_DEGREES = 20.0F;
    private static final float HIGHSTORM_ANGLE_DEGREES = 45.0F;

    /*
     * Vanilla's rain texture is strongly blue. Multiplying it by this warm
     * counter-tint balances its RGB channels into a neutral charcoal grey.
     * Alpha is retained from vanilla so distance and phase opacity still work.
     */
    private static final int HIGHSTORM_RAIN_GREY_RGB = 0x00FFAD5C;

    @Unique
    private boolean stormlight$buildingRain;

    @Unique
    private float stormlight$rainSlope;

    /**
     * WeatherEffectRenderer normally asks the biome whether each column is
     * rain, snow or dry. Treating that answer as RAIN during the three visual
     * Highstorm phases makes vanilla build ordinary rain columns everywhere.
     * All of vanilla's height, roof, light and weather-radius calculations
     * still run normally around this redirected result.
     */
    @Redirect(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientLevel;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"
            )
    )
    private Biome.Precipitation stormlight$useGlobalHighstormRain(
            ClientLevel level,
            BlockPos position
    ) {
        if (stormlight$isHighstormWeatherPhase()) {
            return Biome.Precipitation.RAIN;
        }

        return level.getPrecipitationAt(position);
    }

    /**
     * The first renderInstances call in WeatherEffectRenderer.render is the
     * rain batch. During a Highstorm, all precipitation columns have already
     * been converted to this batch by the extractRenderState redirect above.
     */
    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/WeatherEffectRenderer;renderInstances(Lcom/mojang/blaze3d/vertex/VertexConsumer;Ljava/util/List;Lnet/minecraft/world/phys/Vec3;FIF)V",
                    ordinal = 0,
                    shift = At.Shift.BEFORE
            )
    )
    private void stormlight$beginRainBatch(
            CallbackInfo callback
    ) {
        float angleDegrees =
                stormlight$getRainAngleDegrees();

        stormlight$buildingRain =
                stormlight$isHighstormWeatherPhase();

        stormlight$rainSlope =
                (float) Math.tan(
                        Math.toRadians(angleDegrees)
                );
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/WeatherEffectRenderer;renderInstances(Lcom/mojang/blaze3d/vertex/VertexConsumer;Ljava/util/List;Lnet/minecraft/world/phys/Vec3;FIF)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    private void stormlight$finishRainBatch(
            CallbackInfo callback
    ) {
        stormlight$buildingRain = false;
        stormlight$rainSlope = 0.0F;
    }

    /**
     * Every matching invocation supplies one vertex as X, Y and Z.
     * Rain vertices are already camera-relative, so Y is the vertex's height
     * relative to the camera. Offsetting X by Y * slope keeps the streak near
     * its original column at eye level, moves its top east and its bottom
     * west, producing westward-falling rain independently of camera facing.
     */
    @ModifyArgs(
            method = "renderInstances",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
            )
    )
    private void stormlight$slantWeatherVertex(
            Args args
    ) {
        if (!stormlight$buildingRain) {
            return;
        }

        float x = (Float) args.get(0);
        float y = (Float) args.get(1);

        args.set(
                0,
                x + y * stormlight$rainSlope
        );
    }

    /**
     * Neutralise the blue built into vanilla's rain texture while retaining
     * Minecraft's calculated alpha for distance and weather intensity.
     */
    @ModifyArg(
            method = "renderInstances",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;setColor(I)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
            ),
            index = 0
    )
    private int stormlight$greyHighstormRain(
            int vanillaColor
    ) {
        if (!stormlight$buildingRain) {
            return vanillaColor;
        }

        return (vanillaColor & 0xFF000000)
                | HIGHSTORM_RAIN_GREY_RGB;
    }

    @Unique
    private static float stormlight$getRainAngleDegrees() {
        return stormlight$getWeatherAngleDegrees();
    }

    @Unique
    private static boolean stormlight$isHighstormWeatherPhase() {
        return ClientHighstormState.isApproaching()
                || ClientHighstormState.isHighstorm()
                || ClientHighstormState.isPassing();
    }

    @Unique
    private static float stormlight$getWeatherAngleDegrees() {
        if (ClientHighstormState.isApproaching()) {
            float progress =
                    (float) Mth.smoothstep(
                            ClientHighstormState
                                    .getApproachProgress()
                    );

            return APPROACH_MAX_ANGLE_DEGREES
                    * progress;
        }

        if (ClientHighstormState.isHighstorm()) {
            return HIGHSTORM_ANGLE_DEGREES;
        }

        if (ClientHighstormState.isPassing()) {
            float progress =
                    (float) Mth.smoothstep(
                            ClientHighstormState
                                    .getPassingProgress()
                    );

            return HIGHSTORM_ANGLE_DEGREES
                    * (1.0F - progress);
        }

        return 0.0F;
    }
}
