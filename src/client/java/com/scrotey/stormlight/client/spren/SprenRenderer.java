package com.scrotey.stormlight.client.spren;

import com.mojang.blaze3d.vertex.PoseStack;
import com.scrotey.stormlight.Stormlight;
import com.scrotey.stormlight.spren.SprenEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** Full-bright, solid cuboid sprite; articulation is handled by the model. */
public final class SprenRenderer extends MobRenderer<SprenEntity, SprenRenderState, HonorSprenModel> {
    private static final Identifier TEXTURE = Stormlight.id("textures/entity/honorspren.png");

    public SprenRenderer(EntityRendererProvider.Context context) {
        super(context, new HonorSprenModel(context.bakeLayer(ModSprenModelLayers.HONORSPREN)), 0.0F);
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
