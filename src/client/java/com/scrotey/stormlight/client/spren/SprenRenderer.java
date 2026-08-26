package com.scrotey.stormlight.client.spren;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.scrotey.stormlight.Stormlight;
import com.scrotey.stormlight.spren.SprenEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public final class SprenRenderer
        extends MobRenderer<SprenEntity, SprenRenderState, HonorSprenModel> {

    private static final Identifier TEXTURE =
            Stormlight.id("textures/entity/honorspren.png");

    public SprenRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                new HonorSprenModel(
                        context.bakeLayer(ModSprenModelLayers.HONORSPREN)
                ),
                0.0F
        );
    }

    @Override
    public SprenRenderState createRenderState() {
        return new SprenRenderState();
    }

    @Override
    public void extractRenderState(
            SprenEntity entity,
            SprenRenderState state,
            float partialTick
    ) {
        super.extractRenderState(entity, state, partialTick);

        Vec3 motion = entity.getDeltaMovement();
        state.motionX = (float) motion.x;
        state.motionY = (float) motion.y;
        state.motionZ = (float) motion.z;
        state.horizontalSpeed = (float) Math.sqrt(
                motion.x * motion.x + motion.z * motion.z
        );

        // Stable per-spren offset stops every manifestation from sharing the
        // exact same procedural timing.
        state.whimsyOffset = (entity.getId() * 47) % 260;
        state.twirlDirection = (entity.getId() & 1) == 0 ? 1.0F : -1.0F;
    }

    @Override
    public Identifier getTextureLocation(SprenRenderState state) {
        return TEXTURE;
    }

    @Override
    protected void scale(SprenRenderState state, PoseStack poseStack) {
        // Keep the exact size from the successful first in-game test.
        poseStack.scale(0.45F, 0.45F, 0.45F);

        // Whole-body hover. A second, smaller wave prevents a mechanical loop.
        float bob = (float) Math.sin(state.ageInTicks * 0.11F) * 0.035F;
        bob += (float) Math.sin(state.ageInTicks * 0.047F + 1.3F) * 0.012F;
        poseStack.translate(0.0F, bob, 0.0F);

        // IMPORTANT: flight lean belongs here, on the entire rendered model,
        // rather than on the torso/skirt/limbs independently. This keeps her
        // silhouette intact and makes her glide as one little figure.
        float speed01 = Math.min(state.horizontalSpeed / 0.35F, 1.0F);
        float flightLeanDegrees = 48.0F * speed01;
        poseStack.mulPose(Axis.XP.rotationDegrees(flightLeanDegrees));

        /*
         * Whimsical flight:
         *
         * A gentle moving bank keeps straight-line travel from feeling like
         * she's being dragged on an invisible rail. Then, only when she's
         * genuinely travelling, she occasionally performs a full 360-degree
         * ballerina-style twirl around her own vertical/body axis.
         */
        float phaseAge = state.ageInTicks + state.whimsyOffset;

        float bankDegrees =
                ((float) Math.sin(phaseAge * 0.075F) * 6.5F
                        + (float) Math.sin(phaseAge * 0.031F + 1.9F) * 3.0F)
                        * speed01;

        float twirlProgress = playfulTwirlProgress(phaseAge);
        float travelGate = smoothTravelGate(state.horizontalSpeed);
        float twirlDegrees =
                360.0F * twirlProgress * state.twirlDirection * travelGate;

        // Keep the small bank on Z, but do the full flourish around Y so it
        // reads as a ballerina twirl instead of a cartwheel.
        poseStack.mulPose(Axis.ZP.rotationDegrees(bankDegrees));
        poseStack.mulPose(Axis.YP.rotationDegrees(twirlDegrees));

        // A tiny rise/fall while moving gives the glide a less ghostly,
        // perfectly-linear silhouette without interfering with server AI.
        float playfulLift =
                (float) Math.sin(phaseAge * 0.14F) * 0.018F * speed01;
        poseStack.translate(0.0F, playfulLift, 0.0F);
    }

    private static float playfulTwirlProgress(float phaseAge) {
        final float cycleTicks = 260.0F;
        final float rollTicks = 24.0F;

        float cycle = phaseAge % cycleTicks;
        if (cycle < 0.0F) {
            cycle += cycleTicks;
        }

        if (cycle >= rollTicks) {
            return 0.0F;
        }

        float t = cycle / rollTicks;
        return t * t * (3.0F - 2.0F * t);
    }

    private static float smoothTravelGate(float horizontalSpeed) {
        // Below this she just hovers/banks gently; above it the twirl can play.
        float t = (horizontalSpeed - 0.08F) / 0.10F;
        t = Math.max(0.0F, Math.min(t, 1.0F));
        return t * t * (3.0F - 2.0F * t);
    }

    @Override
    protected int getBlockLightLevel(SprenEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    protected int getSkyLightLevel(SprenEntity entity, BlockPos pos) {
        return 15;
    }
}
