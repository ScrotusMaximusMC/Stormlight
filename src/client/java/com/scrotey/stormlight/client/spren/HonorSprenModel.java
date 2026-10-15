package com.scrotey.stormlight.client.spren;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** A little solid, cuboid windsprite with fins and a springy crest. */
public class HonorSprenModel extends EntityModel<SprenRenderState> {
    private final ModelPart sprite;
    private final ModelPart leftFin;
    private final ModelPart rightFin;
    private final ModelPart crest;
    private final ModelPart crestTip;
    private final ModelPart tail;
    private final ModelPart tailTip;
    private final ModelPart[] eyes = new ModelPart[2];
    private final ModelPart[] feet = new ModelPart[2];

    public HonorSprenModel(ModelPart root) {
        super(root);
        sprite = root.getChild("sprite");
        leftFin = sprite.getChild("left_fin");
        rightFin = sprite.getChild("right_fin");
        crest = sprite.getChild("crest");
        crestTip = crest.getChild("tip");
        tail = sprite.getChild("tail");
        tailTip = tail.getChild("tip");
        for (int i = 0; i < 2; i++) {
            eyes[i] = sprite.getChild("eye_" + i);
            feet[i] = sprite.getChild("foot_" + i);
        }
        // Sample existing blue palette swatches, keeping every cube face crisp
        // and avoiding the humanoid face/clothing regions of the old atlas.
        root.visit(new PoseStack(), (pose, path, index, cube) -> {
            boolean dark = path.contains("eye_") && !path.contains("glint")
                    || path.contains("smile");
            boolean pale = path.contains("glint") || path.contains("belly")
                    || path.contains("cheek");
            float u = (dark ? 176.5F : pale ? 25.5F : 180.5F) / 256.0F;
            float v = (dark ? 28.5F : pale ? 1.5F : 24.5F) / 256.0F;
            for (int p = 0; p < cube.polygons.length; p++) {
                ModelPart.Polygon polygon = cube.polygons[p];
                ModelPart.Vertex[] vertices = polygon.vertices().clone();
                for (int n = 0; n < vertices.length; n++) vertices[n] = vertices[n].remap(u, v);
                cube.polygons[p] = new ModelPart.Polygon(vertices, polygon.normal());
            }
        });
    }

    public static LayerDefinition getLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition sprite = mesh.getRoot().addOrReplaceChild("sprite",
                CubeListBuilder.create().addBox(-4, -4, -3.5F, 8, 8, 7),
                PartPose.offset(0, 17, 0));
        sprite.addOrReplaceChild("belly", CubeListBuilder.create()
                .addBox(-2, 1.5F, -3.65F, 4, 2, 0.25F), PartPose.ZERO);
        for (int i = 0; i < 2; i++) {
            float side = i == 0 ? -1 : 1;
            PartDefinition eye = sprite.addOrReplaceChild("eye_" + i,
                    CubeListBuilder.create().addBox(-0.75F, -1, -0.125F, 1.5F, 2, 0.25F),
                    PartPose.offset(side * 1.75F, -0.75F, -3.6F));
            eye.addOrReplaceChild("glint", CubeListBuilder.create()
                    .addBox(-0.5F, -0.75F, -0.21F, 0.5F, 0.5F, 0.125F), PartPose.ZERO);
            sprite.addOrReplaceChild("cheek_" + i, CubeListBuilder.create()
                    .addBox(-0.5F, 0, -0.125F, 1, 0.5F, 0.25F),
                    PartPose.offset(side * 2.75F, 0.75F, -3.6F));
            sprite.addOrReplaceChild("foot_" + i, CubeListBuilder.create()
                    .addBox(-1, 0, -1.5F, 2, 1.5F, 2.5F),
                    PartPose.offset(side * 2, 3.5F, 0));
        }
        sprite.addOrReplaceChild("smile", CubeListBuilder.create()
                .addBox(-0.5F, 0.5F, -3.8F, 1, 0.5F, 0.25F), PartPose.ZERO);
        sprite.addOrReplaceChild("left_fin", CubeListBuilder.create()
                .addBox(0, -0.5F, -1, 3, 1, 3), PartPose.offset(3.5F, 0.5F, 0));
        sprite.addOrReplaceChild("right_fin", CubeListBuilder.create()
                .addBox(-3, -0.5F, -1, 3, 1, 3), PartPose.offset(-3.5F, 0.5F, 0));
        PartDefinition crest = sprite.addOrReplaceChild("crest", CubeListBuilder.create()
                .addBox(-1, -2, -1, 2, 2, 2), PartPose.offset(0, -4, 0));
        crest.addOrReplaceChild("tip", CubeListBuilder.create()
                .addBox(-0.5F, -1.5F, -0.5F, 1, 2, 1), PartPose.offset(0.5F, -2, 0));
        PartDefinition tail = sprite.addOrReplaceChild("tail", CubeListBuilder.create()
                .addBox(-1, -1, 0, 2, 2, 3), PartPose.offset(0, 1.5F, 3));
        tail.addOrReplaceChild("tip", CubeListBuilder.create()
                .addBox(-1.5F, -0.5F, 0, 3, 1, 2), PartPose.offset(0, 0, 3));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(SprenRenderState state) {
        super.setupAnim(state);
        sprite.getAllParts().forEach(ModelPart::resetPose);
        float t = state.ageInTicks + state.whimsyOffset;
        float flight = smooth(0.025F, 0.28F, state.horizontalSpeed);
        float idle = 1 - flight;
        float curious = gesture(t, 240, 35, 12, 24, 16) * idle;
        float wave = gesture(t, 310, 135, 10, 26, 14) * idle;
        float happy = gesture(t, 390, 265, 12, 30, 18) * idle;
        float flap = wave(t, 0.32F, 0) * (0.16F + flight * 0.32F);

        sprite.y += wave(t, 0.11F, 0) * 0.55F
                - happy * Math.abs(wave(t, 0.24F, 0)) * 1.5F;
        sprite.zRot = wave(t, 0.07F, 0) * 0.045F
                + curious * 0.22F * state.twirlDirection;
        sprite.yRot = curious * wave(t, 0.09F, 0) * 0.28F;
        sprite.xRot = flight * 0.32F - Math.max(-0.15F, Math.min(0.15F, state.motionY))
                - curious * 0.10F;
        // Small squash/stretch makes the idle bounce read as playful, not misty.
        float bounce = happy * wave(t, 0.48F, 0) * 0.055F;
        sprite.yScale = 1 + bounce;
        sprite.xScale = 1 - bounce * 0.5F;
        sprite.zScale = sprite.xScale;
        leftFin.zRot = -0.18F - flap - wave * (0.8F + wave(t, 0.55F, 0) * 0.3F);
        rightFin.zRot = 0.18F + flap + happy * 0.45F;
        leftFin.xRot = rightFin.xRot = flight * 0.35F;
        crest.zRot = wave(t, 0.14F, 0) * 0.12F - curious * 0.28F;
        crest.xRot = flight * 0.45F;
        crestTip.zRot = wave(t - 3, 0.14F, 0) * 0.23F;
        crestTip.xRot = flight * 0.25F;
        tail.yRot = wave(t, 0.16F + flight * 0.08F, 0) * (0.22F + happy * 0.3F);
        tail.xRot = -0.15F - flight * 0.3F;
        tailTip.yRot = wave(t - 4, 0.16F + flight * 0.08F, 0) * 0.35F;
        for (int i = 0; i < 2; i++) {
            feet[i].xRot = flight * 0.65F + happy * wave(t, 0.24F, i * (float) Math.PI) * 0.45F;
            // A quick blink and occasional double blink; highlights follow the eye.
            float blink = Math.max(gesture(t, 97, 12, 1.5F, 1, 2),
                    gesture(t, 291, 20, 1.5F, 1, 2));
            eyes[i].yScale = 1 - blink * 0.92F;
        }
    }

    private static float wave(float time, float frequency, float phase) {
        return (float) Math.sin(time * frequency + phase);
    }

    private static float smooth(float low, float high, float value) {
        float x = Math.max(0, Math.min(1, (value - low) / (high - low)));
        return x * x * (3 - 2 * x);
    }

    private static float gesture(float time, float period, float start,
                                 float rise, float hold, float fall) {
        float local = ((time % period) + period) % period - start;
        return smooth(0, rise, local) * (1 - smooth(rise + hold, rise + hold + fall, local));
    }
}
