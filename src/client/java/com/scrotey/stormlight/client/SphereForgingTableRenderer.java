package com.scrotey.stormlight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.scrotey.stormlight.block.custom.SphereForgingTableBlock;
import com.scrotey.stormlight.block.entity.SphereForgingTableBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Animated parts of the forge, in pixels of the forge's local frame (see {@link SphereForgingTableBlock}). */
public class SphereForgingTableRenderer implements BlockEntityRenderer<SphereForgingTableBlockEntity, SphereForgingTableRenderer.State> {
    private static final Identifier WHITE = Identifier.fromNamespaceAndPath("stormlight", "textures/misc/white.png");
    private static final Identifier IRON = Identifier.withDefaultNamespace("textures/block/iron_block.png");
    private static final Identifier COPPER = Identifier.withDefaultNamespace("textures/block/copper_block.png");
    private static final int MOLTEN = 0xffffa23a, MOLTEN_CORE = 0xffffe08a, SET_GLASS = 0xff8fa79a;
    private static final float POOL_FLOOR = 17.5F, POOL_TOP = 31, NOZZLE_Y = 15, MOULD_X = 39, MOULD_Y = 6, MID_Z = 16;
    private static final float[][] LUMPS = {{22, 10}, {27.5F, 15}, {22.5F, 21.5F}, {27, 22}, {25, 9}, {21.5F, 15.5F}};
    private final ItemModelResolver items;

    public SphereForgingTableRenderer(BlockEntityRendererProvider.Context context) {
        items = context.itemModelResolver();
    }

    public static class State extends BlockEntityRenderState {
        Direction direction;
        boolean lit, pouring;
        float liquid, melt, pour, heat, valve;
        int unmelted;
        final ItemStackRenderState glass = new ItemStackRenderState();
        final ItemStackRenderState mould = new ItemStackRenderState();
        final ItemStackRenderState result = new ItemStackRenderState();
    }

    @Override public State createRenderState() { return new State(); }
    @Override public boolean shouldRenderOffScreen() { return true; }

    @Override public void extractRenderState(SphereForgingTableBlockEntity forge, State s, float partial,
                                             Vec3 camera, ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(forge, s, partial, camera, overlay);
        s.direction = forge.getBlockState().getValue(SphereForgingTableBlock.FACING);
        s.lit = forge.isLit();
        s.pouring = forge.isPouring();
        s.pour = forge.getPour(partial);
        s.heat = s.pouring ? 1 : forge.getHeat(partial);
        s.valve = forge.getValve(partial);
        s.unmelted = forge.getUnmelted();
        s.melt = s.unmelted > 0 ? forge.getMeltProgress() : 0;
        // The pool drains while pouring and swells as the bottom lump of glass melts into it.
        s.liquid = forge.getMolten() + forge.getPourCost() * (1 - s.pour) + s.melt;
        int seed = (int) forge.getBlockPos().asLong();
        items.updateForTopItem(s.glass, new ItemStack(Items.GLASS), ItemDisplayContext.FIXED, forge.getLevel(), null, seed);
        items.updateForTopItem(s.mould, forge.getMould(), ItemDisplayContext.FIXED, forge.getLevel(), null, seed + 1);
        items.updateForTopItem(s.result, forge.getResult(), ItemDisplayContext.FIXED, forge.getLevel(), null, seed + 2);
    }

    @Override public void submit(State s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        float rotation = switch (s.direction) { case SOUTH -> -90; case WEST -> 180; case NORTH -> 90; default -> 0; };
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        pose.translate(-0.5, 0, -0.5);
        pose.scale(1F / 16, 1F / 16, 1F / 16);

        float surface = POOL_FLOOR + (POOL_TOP - POOL_FLOOR) * Math.min(1, s.liquid / SphereForgingTableBlockEntity.CAPACITY);
        if (s.liquid > 0.01F) {
            int color = s.lit ? MOLTEN : SET_GLASS;
            int light = s.lit ? LightCoordsUtil.FULL_BRIGHT : s.lightCoords;
            cuboid(pose, out, s.lit ? RenderTypes.entityTranslucentEmissive(WHITE) : RenderTypes.entityTranslucent(WHITE),
                    18.5F, POOL_FLOOR, 5.5F, 31.5F, surface, 26.5F, color, light);
        }
        for (int i = 0; i < s.unmelted; i++) {
            // Lumps bob in the pool; the bottom one shrinks as it melts.
            float size = i == 0 ? 1 - s.melt * 0.8F : 1;
            pose.pushPose();
            pose.translate(LUMPS[i][0], Math.max(POOL_FLOOR, surface - 0.5F) + 1.5F * size, LUMPS[i][1]);
            pose.mulPose(Axis.YP.rotationDegrees(23 + i * 37));
            pose.scale(16 * 0.45F * size, 16 * 0.45F * size, 16 * 0.45F * size);
            s.glass.submit(pose, out, s.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }

        // Spigot valve: a fixed stem with a copper cross-handle that quarter-turns when opened.
        cuboid(pose, out, RenderTypes.entitySolid(IRON), 35.5F, 19.5F, 15.5F, 36.5F, 21, 16.5F, 0xffffffff, s.lightCoords);
        pose.pushPose();
        pose.translate(36, 21, MID_Z);
        float ease = s.valve * s.valve * (3 - 2 * s.valve);
        pose.mulPose(Axis.YP.rotationDegrees(90 * ease));
        cuboid(pose, out, RenderTypes.entitySolid(COPPER), -3, 0, -0.5F, 3, 0.8F, 0.5F, 0xffffffff, s.lightCoords);
        pose.popPose();

        float target = MOULD_Y + 2.4F + 2.6F * Math.min(1, s.pour * 1.25F);
        if (s.pouring) {
            // The stream reaches down over the first few ticks and breaks off from the top at the end.
            float top = s.pour > 0.85F ? NOZZLE_Y - (NOZZLE_Y - target) * (s.pour - 0.85F) / 0.15F : NOZZLE_Y;
            float bottom = s.pour < 0.12F ? NOZZLE_Y - (NOZZLE_Y - target) * s.pour / 0.12F : target;
            float w = 0.35F + 0.15F * (float) Math.sin(s.pour * 40);
            cuboid(pose, out, RenderTypes.entityTranslucentEmissive(WHITE),
                    MOULD_X - w, bottom, MID_Z - w, MOULD_X + w, top, MID_Z + w, MOLTEN_CORE, LightCoordsUtil.FULL_BRIGHT);
        }

        ItemStackRenderState shown = s.result.isEmpty() ? s.mould : s.result;
        if (!shown.isEmpty()) {
            pose.pushPose();
            pose.translate(MOULD_X, MOULD_Y + 2.4F, MID_Z);
            pose.mulPose(Axis.YP.rotationDegrees(180));
            pose.scale(16 * 0.36F, 16 * 0.36F, 16 * 0.36F);
            shown.submit(pose, out, s.heat > 0 ? LightCoordsUtil.FULL_BRIGHT : s.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        if (s.heat > 0) {
            // A glowing bead of glass gathers around the gem, then cools to clear.
            float radius = 2.6F * (s.pouring ? Math.min(1, s.pour * 1.25F) : 1);
            int alpha = (int) (200 * s.heat);
            int color = alpha << 24 | (MOLTEN & 0xffffff);
            pose.pushPose();
            pose.translate(MOULD_X, MOULD_Y + 2.4F, MID_Z);
            final float r = radius;
            out.submitCustomGeometry(pose, RenderTypes.entityTranslucentEmissive(WHITE),
                    (matrix, vertices) -> bead(matrix, vertices, r, color));
            pose.popPose();
        }
        pose.popPose();
    }

    private static void cuboid(PoseStack pose, SubmitNodeCollector out, RenderType type, float x0, float y0, float z0,
                               float x1, float y1, float z1, int color, int light) {
        out.submitCustomGeometry(pose, type, (matrix, v) -> {
            quad(matrix, v, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, 0, 1, 0, color, light);
            quad(matrix, v, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, 0, -1, 0, color, light);
            quad(matrix, v, x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0, 0, 0, -1, color, light);
            quad(matrix, v, x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1, 0, 0, 1, color, light);
            quad(matrix, v, x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1, -1, 0, 0, color, light);
            quad(matrix, v, x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0, 1, 0, 0, color, light);
        });
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer v, float ax, float ay, float az, float bx, float by,
                             float bz, float cx, float cy, float cz, float dx, float dy, float dz,
                             float nx, float ny, float nz, int color, int light) {
        vertex(pose, v, ax, ay, az, 0, 0, nx, ny, nz, color, light);
        vertex(pose, v, bx, by, bz, 0, 1, nx, ny, nz, color, light);
        vertex(pose, v, cx, cy, cz, 1, 1, nx, ny, nz, color, light);
        vertex(pose, v, dx, dy, dz, 1, 0, nx, ny, nz, color, light);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer v, float x, float y, float z, float u, float w,
                               float nx, float ny, float nz, int color, int light) {
        v.addVertex(pose, x, y, z).setColor(color).setUv(u, w).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(pose, nx, ny, nz);
    }

    private static void bead(PoseStack.Pose pose, VertexConsumer v, float radius, int color) {
        int rings = 6, segments = 10;
        for (int ring = 0; ring < rings; ring++) {
            double p0 = Math.PI * ring / rings - Math.PI / 2, p1 = Math.PI * (ring + 1) / rings - Math.PI / 2;
            for (int seg = 0; seg < segments; seg++) {
                double t0 = 2 * Math.PI * seg / segments, t1 = 2 * Math.PI * (seg + 1) / segments;
                float[] a = point(p0, t0, radius), b = point(p1, t0, radius), c = point(p1, t1, radius), d = point(p0, t1, radius);
                float[] n = point((p0 + p1) / 2, (t0 + t1) / 2, 1);
                vertex(pose, v, a[0], a[1], a[2], 0, 0, n[0], n[1], n[2], color, LightCoordsUtil.FULL_BRIGHT);
                vertex(pose, v, b[0], b[1], b[2], 0, 1, n[0], n[1], n[2], color, LightCoordsUtil.FULL_BRIGHT);
                vertex(pose, v, c[0], c[1], c[2], 1, 1, n[0], n[1], n[2], color, LightCoordsUtil.FULL_BRIGHT);
                vertex(pose, v, d[0], d[1], d[2], 1, 0, n[0], n[1], n[2], color, LightCoordsUtil.FULL_BRIGHT);
            }
        }
    }

    private static float[] point(double phi, double theta, float r) {
        return new float[]{(float) (Math.cos(phi) * Math.cos(theta) * r), (float) (Math.sin(phi) * r),
                (float) (Math.cos(phi) * Math.sin(theta) * r)};
    }
}
