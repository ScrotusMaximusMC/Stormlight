package com.scrotey.stormlight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.scrotey.stormlight.block.custom.GemcuttingTableBlock;
import com.scrotey.stormlight.block.entity.GemcuttingTableBlockEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GemcuttingTableRenderer implements BlockEntityRenderer<GemcuttingTableBlockEntity, GemcuttingTableRenderer.State> {
    private static final Identifier IRON = Identifier.withDefaultNamespace("textures/block/iron_block.png");
    private static final Identifier COPPER = Identifier.withDefaultNamespace("textures/block/copper_block.png");
    private static final Identifier WHITE = Identifier.fromNamespaceAndPath("stormlight", "textures/misc/white.png");
    private static final int[] COLORS = {0xff8de9f3, 0xff8c193c, 0xffec2949, 0xff305bdf, 0xff20c976};
    private static final float[] RADII = {1.9F, 2.4F, 2.1F, 2.2F, 1.7F};
    private final ModelPart jaw = box(0, 0, 0, 1.5F, 3.5F, 7);
    private final ModelPart shaft = box(0, -0.45F, -0.45F, 1, 0.9F, 0.9F);
    private final ModelPart rim = box(-0.5F, 2.35F, -1.15F, 1, 0.65F, 2.3F);
    private final ModelPart spoke = box(-0.3F, 0, -0.22F, 0.6F, 2.6F, 0.44F);
    private final ModelPart hub = box(-0.7F, -0.7F, -0.7F, 1.4F, 1.4F, 1.4F);
    public GemcuttingTableRenderer(BlockEntityRendererProvider.Context context) {}
    public static class State extends BlockEntityRenderState {
        Direction direction;
        float clamp;
        int kind, strikes;
        boolean hasGem;
    }
    @Override public State createRenderState() { return new State(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public void extractRenderState(GemcuttingTableBlockEntity table, State state, float partial,
                                             Vec3 camera, ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(table, state, partial, camera, overlay);
        state.direction = table.getBlockState().getValue(GemcuttingTableBlock.FACING);
        state.clamp = table.getClamp(partial);
        state.kind = table.getGemKind();
        state.strikes = table.getStrikes();
        state.hasGem = !table.getGem().isEmpty();
    }
    @Override public void submit(State s, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        float rotation = switch (s.direction) { case SOUTH -> -90; case WEST -> 180; case NORTH -> 90; default -> 0; };
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        pose.translate(-0.5, 0, -0.5);
        float radius = RADII[s.kind];
        float ease = s.clamp * s.clamp * (3 - 2 * s.clamp);
        float jawX = 12.5F + (5.5F + radius * 2 - 12.5F) * ease;
        pose.pushPose();
        pose.translate(jawX / 16, 16F / 16, 4F / 16);
        part(jaw, IRON, s, pose, collector);
        pose.popPose();
        pose.pushPose();
        pose.translate((jawX + 1.5F) / 16, 18.5F / 16, 7.5F / 16);
        pose.scale(18 - jawX - 1.5F, 1, 1);
        part(shaft, IRON, s, pose, collector);
        pose.popPose();
        pose.pushPose();
        // The wheel is outboard of the screw support, with clearance above the stone slab.
        pose.translate(18F / 16, 18.5F / 16, 7.5F / 16);
        pose.mulPose(Axis.XP.rotationDegrees(ease * 540));
        part(hub, IRON, s, pose, collector);
        for (int i = 0; i < 8; i++) {
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(i * 45));
            part(rim, COPPER, s, pose, collector);
            if (i % 2 == 0) part(spoke, IRON, s, pose, collector);
            pose.popPose();
        }
        pose.popPose();
        if (s.hasGem) {
            pose.pushPose();
            pose.translate((5.5F + radius) / 16, 18F / 16, 7.5F / 16);
            pose.scale(1F / 16, 1F / 16, 1F / 16);
            final int kind = s.kind, hits = s.strikes, light = s.lightCoords;
            collector.submitCustomGeometry(pose, RenderTypes.entityCutout(WHITE),
                    (matrix, vertices) -> gem(matrix, vertices, kind, hits, light));
            pose.popPose();
        }
        pose.popPose();
    }
    private static ModelPart box(float x, float y, float z, float dx, float dy, float dz) {
        return new ModelPart(List.of(new ModelPart.Cube(0, 0, x, y, z, dx, dy, dz,
                0, 0, 0, false, 16, 16, Set.of(Direction.values()))), Map.of());
    }
    private static void part(ModelPart part, Identifier texture, State s, PoseStack pose, SubmitNodeCollector out) {
        out.submitModelPart(part, pose, RenderTypes.entitySolid(texture), s.lightCoords,
                OverlayTexture.NO_OVERLAY, null);
    }
    private static void gem(PoseStack.Pose pose, VertexConsumer out, int kind, int hits, int light) {
        float radius = RADII[kind], height = kind == 4 ? 3 : 2.3F;
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4, b = (i + 1) * Math.PI / 4;
            Vector3f lowA = new Vector3f((float)Math.cos(a)*radius, -0.4F, (float)Math.sin(a)*radius);
            Vector3f lowB = new Vector3f((float)Math.cos(b)*radius, -0.4F, (float)Math.sin(b)*radius);
            Vector3f topA = new Vector3f(lowA.x*0.55F, height, lowA.z*0.55F);
            Vector3f topB = new Vector3f(lowB.x*0.55F, height, lowB.z*0.55F);
            int color = shade(COLORS[kind], 0.7F + (i % 3) * 0.14F);
            quad(pose, out, lowA, lowB, topB, topA, color, light);
            quad(pose, out, topA, topB, new Vector3f(0,height,0), new Vector3f(0,height,0), COLORS[kind], light);
            quad(pose, out, lowB, lowA, new Vector3f(0,-2,0), new Vector3f(0,-2,0), shade(color,0.8F), light);
            if (hits >= 2 && i < hits) {
                // Jagged ribbons lie just above each facet; more appear every two strikes.
                Vector3f normal = new Vector3f(lowB).sub(lowA).cross(new Vector3f(topA).sub(lowA)).normalize().negate();
                Vector3f start = new Vector3f(topA).lerp(topB,0.3F).fma(0.018F,normal);
                Vector3f end = new Vector3f(lowA).lerp(lowB,0.65F).fma(0.018F,normal);
                Vector3f mid = new Vector3f(start).lerp(end,0.5F).add(new Vector3f(lowB).sub(lowA).mul(0.15F));
                ribbon(pose,out,start,mid,normal,light);
                ribbon(pose,out,mid,end,normal,light);
            }
        }
    }
    private static void ribbon(PoseStack.Pose pose, VertexConsumer out, Vector3f a, Vector3f b, Vector3f normal, int light) {
        Vector3f offset = new Vector3f(b).sub(a).cross(normal).normalize().mul(0.045F);
        quad(pose,out,new Vector3f(a).sub(offset),new Vector3f(b).sub(offset),
                new Vector3f(b).add(offset),new Vector3f(a).add(offset),0xff172128,light);
    }
    private static int shade(int color, float f) {
        return 0xff000000 | ((int)(((color>>16)&255)*f)<<16) | ((int)(((color>>8)&255)*f)<<8) | (int)((color&255)*f);
    }
    private static void quad(PoseStack.Pose pose, VertexConsumer out, Vector3f a, Vector3f b, Vector3f c, Vector3f d, int color, int light) {
        Vector3f n = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a)).normalize().negate();
        for (Vector3f v : new Vector3f[]{d,c,b,a}) out.addVertex(pose,v.x,v.y,v.z).setColor(color)
                .setUv(0.5F,0.5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,n.x,n.y,n.z);
    }
}
