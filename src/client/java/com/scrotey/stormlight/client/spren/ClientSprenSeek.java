package com.scrotey.stormlight.client.spren;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.PoseStack;
import com.scrotey.stormlight.Stormlight;
import com.scrotey.stormlight.network.SprenSeekStatePayload;
import com.scrotey.stormlight.particle.ModParticles;
import com.scrotey.stormlight.spren.SprenSeekOres;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

public final class ClientSprenSeek {
    private static final RenderType THROUGH_WALL_LINES = RenderType.create(
            "stormlight_spren_seek_outline",
            RenderSetup.builder(RenderPipelines.register(
                    RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                            .withLocation(Stormlight.id(
                                    "spren_seek_outline"))
                            .withDepthStencilState(
                                    new DepthStencilState(
                                            CompareOp.ALWAYS_PASS,
                                            false))
                            .build()
            )).createRenderSetup()
    );

    private static ClientLevel activeLevel;
    private static int phase;
    private static int sprenId;
    private static BlockPos ore;

    private ClientSprenSeek() {
    }

    public static void register() {
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            if (phase != 2 || ore == null || activeLevel == null) {
                return;
            }
            Vec3 camera = context.levelState().cameraRenderState.pos;
            PoseStack pose = context.poseStack();
            pose.pushPose();
            pose.translate(ore.getX() - camera.x,
                    ore.getY() - camera.y,
                    ore.getZ() - camera.z);
            context.submitNodeCollector().submitShapeOutline(
                    pose, Shapes.block(), THROUGH_WALL_LINES,
                    0xFF8DEBFF, 2.5F, false);
            pose.popPose();
        });
    }

    public static void update(Minecraft client,
            SprenSeekStatePayload payload) {
        activeLevel = client.level;
        phase = payload.phase();
        sprenId = payload.sprenId();
        ore = phase == 2 ? payload.ore() : null;
        if (phase == 1 && client.player != null
                && client.level != null) {
            Vec3 start = client.player.position()
                    .add(0, 1.5, 0);
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4;
                client.level.addParticle(ModParticles.SPREN_SEEK_TRAIL,
                        start.x + Math.cos(angle) * 0.35,
                        start.y, start.z + Math.sin(angle) * 0.35,
                        0, 0, 0);
            }
        }
    }

    public static void tick(Minecraft client) {
        if (client.level == null || client.level != activeLevel) {
            clear();
            return;
        }
        if (phase == 1) {
            Entity spren = client.level.getEntity(sprenId);
            if (spren != null) {
                client.level.addParticle(
                        ModParticles.SPREN_SEEK_TRAIL,
                        spren.getX(), spren.getY() + 0.3,
                        spren.getZ(), 0, 0, 0);
            }
        } else if (phase == 2 && ore != null
                && client.level.hasChunkAt(ore)
                && !SprenSeekOres.isOre(
                        client.level.getBlockState(ore))) {
            clear();
        }
    }

    private static void clear() {
        activeLevel = null;
        phase = 0;
        sprenId = 0;
        ore = null;
    }
}
