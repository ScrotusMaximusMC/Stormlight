package com.scrotey.stormlight.client.spren;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** A simple silhouette with hinged hair and hem, using the existing texture. */
public class HonorSprenModel extends EntityModel<SprenRenderState> {
    private final int renderPass;
    private final java.util.Set<ModelPart> softParts = new java.util.HashSet<>();
    private final java.util.Set<ModelPart> faintParts = new java.util.HashSet<>();
    private final ModelPart figure;
    private final ModelPart head;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart[] hair = new ModelPart[3];
    private final ModelPart[] hairTips = new ModelPart[3];
    private final ModelPart[] sideLocks = new ModelPart[2];
    private final ModelPart[] hem = new ModelPart[4];

    public HonorSprenModel(ModelPart root) {
        this(root, 0);
    }

    /** Pass 0 is solid; passes 1 and 2 contain only the fading ends. */
    public HonorSprenModel(ModelPart root, int renderPass) {
        super(root);
        this.renderPass = renderPass;
        for (ModelPart part : root.getAllParts()) {
            if (part.hasChild("fade_1")) softParts.add(part.getChild("fade_1"));
            if (part.hasChild("fade_2")) faintParts.add(part.getChild("fade_2"));
        }
        figure = root.getChild("figure");
        head = figure.getChild("head");
        leftArm = figure.getChild("left_arm");
        rightArm = figure.getChild("right_arm");
        leftLeg = figure.getChild("left_leg");
        rightLeg = figure.getChild("right_leg");
        for (int i = 0; i < hair.length; i++) {
            hair[i] = head.getChild("hair_" + i);
            hairTips[i] = hair[i].getChild("tip");
        }
        for (int i = 0; i < sideLocks.length; i++) {
            sideLocks[i] = head.getChild("side_" + i).getChild("tip");
        }
        ModelPart dress = figure.getChild("dress");
        for (int i = 0; i < hem.length; i++) {
            hem[i] = dress.getChild("hem_" + i);
        }
        // Palette swatches from the existing texture. Face details are geometry,
        // so no part of the old face atlas can leak onto the clothing or hair.
        root.visit(new PoseStack(), (pose, path, index, cube) -> {
            boolean hairPart = path.contains("hair") || path.contains("side_");
            boolean pupil = path.contains("eye_pupil");
            boolean mist = path.contains("fade_");
            float u = (pupil ? 176.5F : hairPart && !mist ? 180.5F : 25.5F) / 256.0F;
            float v = (pupil ? 28.5F : hairPart && !mist ? 24.5F : 1.5F) / 256.0F;
            for (int p = 0; p < cube.polygons.length; p++) {
                ModelPart.Polygon polygon = cube.polygons[p];
                ModelPart.Vertex[] vertices = polygon.vertices().clone();
                for (int n = 0; n < vertices.length; n++) vertices[n] = vertices[n].remap(u, v);
                cube.polygons[p] = new ModelPart.Polygon(vertices, polygon.normal());
            }
        });
        applyPassMask();
    }

    private void applyPassMask() {
        for (ModelPart part : root.getAllParts()) {
            int partPass = softParts.contains(part) ? 1 : faintParts.contains(part) ? 2 : 0;
            // skipDraw hides this part's cubes, but still traverses its children.
            part.skipDraw = partPass != renderPass;
        }
    }

    public static LayerDefinition getLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition figure = mesh.getRoot().addOrReplaceChild("figure",
                CubeListBuilder.create(), PartPose.offset(0, 16, 0));

        // Pale face with small blue eyes, centred on the model.
        PartDefinition head = figure.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-3, -6, -3, 6, 6, 6), PartPose.ZERO);
        head.addOrReplaceChild("hair_cap", CubeListBuilder.create()
                .addBox(-3.25F, -7.0F, -3.0F, 6.5F, 1.5F, 6.25F), PartPose.ZERO);
        // A broad, offset crown lifts the centre without adding a hat brim.
        head.addOrReplaceChild("hair_crown", CubeListBuilder.create()
                .addBox(-2.75F, -7.7F, -2.5F, 5.25F, 0.9F, 5.25F), PartPose.ZERO);
        head.addOrReplaceChild("hair_sweep", CubeListBuilder.create()
                .addBox(-3.1F, -5.8F, -3.18F, 2.1F, 0.85F, 0.4F), PartPose.ZERO);
        for (int i = 0; i < 2; i++) {
            float x = i == 0 ? -1.25F : 1.25F;
            head.addOrReplaceChild("eye_pupil_" + i, CubeListBuilder.create()
                    .addBox(-0.28F, -0.35F, -0.04F, 0.56F, 0.70F, 0.08F),
                    PartPose.offset(x, -3.1F, -3.045F));
            head.addOrReplaceChild("eye_white_" + i, CubeListBuilder.create()
                    .addBox(-0.10F, -0.10F, -0.025F, 0.20F, 0.20F, 0.05F),
                    PartPose.offset(x - 0.10F, -3.32F, -3.10F));
        }
        // Side coverage joins the crown to the back locks; no projecting brim.
        for (int i = 0; i < 2; i++) {
            PartDefinition side = head.addOrReplaceChild("side_" + i,
                    CubeListBuilder.create().texOffs(23, 29)
                            .addBox(-0.5F, 0, -2.5F, 1, 5, 5),
                    PartPose.offset(i == 0 ? -3 : 3, -6, 0));
            PartDefinition sideTip = side.addOrReplaceChild("tip", CubeListBuilder.create()
                            .addBox(-0.5F, 0, -1, 1, 1.5F, 2), PartPose.offset(0, 5, 1.5F));
            addFade(sideTip, -0.5F, 1.5F, -1, 1, 1.5F, 2);
        }

        // Three broad locks, each with a second joint. No rigid fringe sculpture.
        for (int i = 0; i < 3; i++) {
            PartDefinition lock = head.addOrReplaceChild("hair_" + i,
                    CubeListBuilder.create().texOffs(6, 19)
                            .addBox(-1, 0, 0, 2, 4, 1),
                    PartPose.offset((i - 1) * 2.0F, -5.5F, 3.0F));
            PartDefinition tip = lock.addOrReplaceChild("tip", CubeListBuilder.create()
                            .addBox(-1, 0, 0, 2, 2, 1), PartPose.offset(0, 4, 0));
            addFade(tip, -1, 2, 0, 2, i == 1 ? 3 : 2, 1);
        }

        figure.addOrReplaceChild("body", CubeListBuilder.create().texOffs(24, 31)
                .addBox(-2.5F, 0, -1, 5, 4, 3)
                .texOffs(24, 31).addBox(-2, 4, -1, 4, 3, 3), PartPose.ZERO);
        figure.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(16, 29)
                .addBox(-1, -1, -1, 2, 8, 2), PartPose.offset(3.5F, 1, 0.5F));
        figure.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(16, 29)
                .addBox(-1, -1, -1, 2, 8, 2), PartPose.offset(-3.5F, 1, 0.5F));

        PartDefinition dress = figure.addOrReplaceChild("dress",
                CubeListBuilder.create().texOffs(23, 31)
                        .addBox(-2.5F, 0, -2, 5, 3, 4), PartPose.offset(0, 7, 0.5F));
        // Each panel has an opaque upper section and two translucent end bands.
        PartDefinition front = dress.addOrReplaceChild("hem_0", CubeListBuilder.create()
                .addBox(-2.5F, 0, -0.5F, 5, 2, 1), PartPose.offset(0, 3, -1.5F));
        PartDefinition back = dress.addOrReplaceChild("hem_1", CubeListBuilder.create()
                .addBox(-2.5F, 0, -0.5F, 5, 2, 1), PartPose.offset(0, 3, 1.5F));
        PartDefinition left = dress.addOrReplaceChild("hem_2", CubeListBuilder.create()
                .addBox(-0.5F, 0, -2, 1, 2, 4), PartPose.offset(2, 3, 0));
        PartDefinition right = dress.addOrReplaceChild("hem_3", CubeListBuilder.create()
                .addBox(-0.5F, 0, -2, 1, 2, 4), PartPose.offset(-2, 3, 0));
        addFade(front, -2.5F, 2, -0.5F, 5, 2, 1);
        addFade(back, -2.5F, 2, -0.5F, 5, 2, 1);
        addFade(left, -0.5F, 2, -2, 1, 2, 4);
        addFade(right, -0.5F, 2, -2, 1, 2, 4);
        PartDefinition leftLeg = figure.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .addBox(-1, 0, -1, 2, 3.5F, 2), PartPose.offset(1.25F, 11, 0.5F));
        PartDefinition rightLeg = figure.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .addBox(-1, 0, -1, 2, 3.5F, 2), PartPose.offset(-1.25F, 11, 0.5F));
        addFade(leftLeg, -1, 3.5F, -1, 2, 2.5F, 2);
        addFade(rightLeg, -1, 3.5F, -1, 2, 2.5F, 2);
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void addFade(PartDefinition parent, float x, float y, float z,
                                float width, float height, float depth) {
        float half = height / 2;
        parent.addOrReplaceChild("fade_1", CubeListBuilder.create()
                .addBox(x, y, z, width, half, depth), PartPose.ZERO);
        // Slight taper makes the final boundary less square without extra wisps.
        parent.addOrReplaceChild("fade_2", CubeListBuilder.create()
                .addBox(x + width * 0.06F, y + half, z + depth * 0.06F,
                        width * 0.88F, half, depth * 0.88F), PartPose.ZERO);
    }

    @Override
    public void setupAnim(SprenRenderState state) {
        super.setupAnim(state);
        // Reset the entire hierarchy so repeated calls never accumulate rotations.
        figure.getAllParts().forEach(ModelPart::resetPose);
        applyPassMask();
        float t = state.ageInTicks + state.whimsyOffset;
        float flight = smooth(0.025F, 0.28F, state.horizontalSpeed);
        float resting = 1.0F - flight;
        float curiosity = gesture(t, 300, 35, 16, 30, 20) * resting;
        float handGesture = gesture(t, 420, 170, 18, 24, 24) * resting;

        // A calm hover, with readable pauses between small gestures.
        figure.y += wave(t, 0.065F, 0) * 0.30F;
        figure.zRot = wave(t, 0.045F, 0.7F) * 0.025F;
        head.xRot = -flight * 0.24F - curiosity * 0.12F;
        head.yRot = wave(t, 0.028F, 1.2F) * 0.07F + curiosity * 0.22F;
        head.zRot = resting * 0.065F + curiosity * 0.17F;

        // Retain the tested outward arm signs from the previous model.
        leftArm.zRot = -0.10F - flight * 0.35F - handGesture * 0.60F;
        rightArm.zRot = 0.10F + flight * 0.35F;
        leftArm.xRot = flight * 0.45F - resting * 0.12F - handGesture * 0.65F;
        rightArm.xRot = flight * 0.45F + resting * 0.17F + curiosity * 0.18F;
        leftArm.xRot += wave(t, 0.075F, 0) * 0.035F;
        rightArm.xRot += wave(t, 0.075F, 1.8F) * 0.035F;
        leftLeg.xRot = flight * 0.42F + wave(t, 0.07F, 0.4F) * 0.035F;
        rightLeg.xRot = resting * 0.22F + flight * 0.36F + wave(t, 0.07F, 2) * 0.035F;

        leftLeg.zRot = -resting * 0.025F;
        rightLeg.zRot = resting * 0.055F;
        rightLeg.y -= resting * 0.30F;

        // Roots and tips use delayed waves, giving a travelling bend instead of
        // moving all the hair as a solid block. Airflow increases with speed.
        for (int i = 0; i < hair.length; i++) {
            float phase = i * 0.75F;
            hair[i].xRot = 0.10F + flight * 0.42F
                    + wave(t, 0.10F, phase) * (0.055F + flight * 0.04F);
            hair[i].zRot = wave(t, 0.073F, phase) * 0.055F;
            hairTips[i].xRot = 0.08F + flight * 0.20F
                    + wave(t - 4, 0.10F, phase) * (0.11F + flight * 0.07F);
            hairTips[i].zRot = wave(t - 5, 0.073F, phase) * 0.09F;
        }
        for (int i = 0; i < sideLocks.length; i++) {
            sideLocks[i].xRot = 0.07F + flight * 0.30F
                    + wave(t - 3, 0.10F, i * 1.4F) * 0.06F;
            sideLocks[i].zRot = wave(t, 0.073F, i * 1.4F) * 0.035F;
        }
        float ripple = 0.045F + flight * 0.035F;
        hem[0].xRot = -0.12F + flight * 0.18F + wave(t, 0.10F, 0) * ripple;
        hem[1].xRot = 0.12F + flight * 0.24F + wave(t - 4, 0.10F, 0) * ripple;
        hem[2].zRot = -0.12F + wave(t, 0.085F, 1) * ripple;
        hem[3].zRot = 0.12F + wave(t - 4, 0.085F, 1) * ripple;
        hem[2].xRot = flight * 0.18F;
        hem[3].xRot = flight * 0.18F;
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


