package com.scrotey.stormlight.client.spren;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.scrotey.stormlight.Stormlight;
import com.scrotey.stormlight.spren.SprenEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** Keeps the existing entity behaviour, texture, brightness and overall scale. */
public final class SprenRenderer extends MobRenderer<SprenEntity, SprenRenderState, HonorSprenModel> {
    private static final Identifier TEXTURE = Stormlight.id("textures/entity/honorspren.png");

    public SprenRenderer(EntityRendererProvider.Context context) {
        super(context, new HonorSprenModel(context.bakeLayer(ModSprenModelLayers.HONORSPREN)), 0.0F);
        addLayer(new FadingEndsLayer(this, context));
    }

    /** Separate models prevent deferred rendering from sharing visibility masks. */
    private static final class FadingEndsLayer extends RenderLayer<SprenRenderState, HonorSprenModel> {
        private final HonorSprenModel soft;
        private final HonorSprenModel faint;

        FadingEndsLayer(SprenRenderer parent, EntityRendererProvider.Context context) {
            super(parent);
            soft = new HonorSprenModel(context.bakeLayer(ModSprenModelLayers.HONORSPREN), 1);
            faint = new HonorSprenModel(context.bakeLayer(ModSprenModelLayers.HONORSPREN), 2);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                           SprenRenderState state, float yaw, float pitch) {
            if (state.isInvisible) return;
            collector.order(1).submitModel(soft, state, poseStack,
                    RenderTypes.entityTranslucent(TEXTURE), light, OverlayTexture.NO_OVERLAY,
                    0xA8FFFFFF, null, state.outlineColor, null);
            collector.order(1).submitModel(faint, state, poseStack,
                    RenderTypes.entityTranslucent(TEXTURE), light, OverlayTexture.NO_OVERLAY,
                    0x48FFFFFF, null, state.outlineColor, null);
        }
    }

    @Override
    public SprenRenderState createRenderState() {
        return new SprenRenderState();
    }

    @Override
    public void extractRenderState(SprenEntity entity, SprenRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        Vec3 motion = entity.getDeltaMovement();
        state.motionX = (float) motion.x;
        state.motionY = (float) motion.y;
        state.motionZ = (float) motion.z;
        state.horizontalSpeed = (float) Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        state.whimsyOffset = (entity.getId() * 47) % 260;
        state.twirlDirection = (entity.getId() & 1) == 0 ? 1.0F : -1.0F;
    }

    @Override
    public Identifier getTextureLocation(SprenRenderState state) {
        return TEXTURE;
    }

    @Override
    protected void scale(SprenRenderState state, PoseStack poseStack) {
        poseStack.scale(0.45F, 0.45F, 0.45F);
        float flight = Math.max(0, Math.min(1, state.horizontalSpeed / 0.30F));
        flight = flight * flight * (3 - 2 * flight);
        // Gentle whole-body flight lean; all parts retain their shared pivot.
        poseStack.mulPose(Axis.XP.rotationDegrees(30.0F * flight));
        // No clock-driven full spins: the articulated model supplies the motion.
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
