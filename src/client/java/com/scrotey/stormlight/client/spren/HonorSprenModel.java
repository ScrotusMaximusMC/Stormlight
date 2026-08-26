package com.scrotey.stormlight.client.spren;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class HonorSprenModel extends EntityModel<SprenRenderState> {
    private final ModelPart head_group;
    private final ModelPart hair_group;
    private final ModelPart body_group;
    private final ModelPart left_arm_group;
    private final ModelPart right_arm_group;
    private final ModelPart skirt_group;
    private final ModelPart left_leg_group;
    private final ModelPart right_leg_group;

    public HonorSprenModel(ModelPart root) {
        super(root);
        this.head_group = root.getChild("head_group");
        this.hair_group = this.head_group.getChild("hair_group");
        this.body_group = root.getChild("body_group");
        this.left_arm_group = root.getChild("left_arm_group");
        this.right_arm_group = root.getChild("right_arm_group");
        this.skirt_group = root.getChild("skirt_group");
        this.left_leg_group = root.getChild("left_leg_group");
        this.right_leg_group = root.getChild("right_leg_group");
    }

    public static LayerDefinition getLayerDefinition() {
        MeshDefinition modelData = new MeshDefinition();
        PartDefinition modelPartData = modelData.getRoot();
        PartDefinition head_group = modelPartData.addOrReplaceChild("head_group", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -6.0F, -3.0F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.5F, 16.0F, 0.0F));

        PartDefinition hair_group = head_group.addOrReplaceChild("hair_group", CubeListBuilder.create().texOffs(40, 42).addBox(2.5F, -6.0F, 3.0F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(40, 22).addBox(1.5F, -6.0F, 3.0F, 1.0F, 9.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(0, 33).addBox(0.5F, -6.0F, 3.0F, 1.0F, 10.0F, 2.0F, new CubeDeformation(0.0F))
        .texOffs(6, 33).addBox(-1.5F, -6.0F, 3.0F, 1.0F, 10.0F, 2.0F, new CubeDeformation(0.0F))
        .texOffs(16, 26).addBox(-0.5F, -7.0F, 3.0F, 1.0F, 11.0F, 3.0F, new CubeDeformation(0.0F))
        .texOffs(40, 32).addBox(-2.5F, -6.0F, 3.0F, 1.0F, 9.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(44, 6).addBox(-3.5F, -6.0F, 3.0F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(0, 19).addBox(-3.5F, -7.0F, -3.0F, 7.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
        .texOffs(46, 49).addBox(-2.5F, -8.0F, -1.0F, 5.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
        .texOffs(12, 40).addBox(2.5F, -6.0F, 1.0F, 1.0F, 6.0F, 2.0F, new CubeDeformation(0.0F))
        .texOffs(44, 15).addBox(2.5F, -6.0F, -1.0F, 1.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
        .texOffs(44, 32).addBox(2.5F, -6.0F, -4.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(44, 43).addBox(2.5F, -6.0F, -3.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(4, 45).addBox(2.5F, -6.0F, -2.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(18, 40).addBox(-3.5F, -6.0F, 1.0F, 1.0F, 6.0F, 2.0F, new CubeDeformation(0.0F))
        .texOffs(44, 21).addBox(-3.5F, -6.0F, -1.0F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
        .texOffs(0, 45).addBox(-3.5F, -6.0F, -2.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(44, 37).addBox(-3.5F, -6.0F, -3.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(12, 33).addBox(-3.5F, -6.0F, -4.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(44, 26).addBox(1.0F, -7.0F, -4.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(44, 41).addBox(-1.0F, -7.0F, -4.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(44, 29).addBox(-3.0F, -7.0F, -4.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition body_group = modelPartData.addOrReplaceChild("body_group", CubeListBuilder.create().texOffs(0, 26).addBox(-2.5F, 0.0F, -1.0F, 5.0F, 4.0F, 3.0F, new CubeDeformation(0.0F))
        .texOffs(26, 22).addBox(-2.0F, 4.0F, -1.0F, 4.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.5F, 16.0F, 0.0F));

        PartDefinition left_arm_group = modelPartData.addOrReplaceChild("left_arm_group", CubeListBuilder.create().texOffs(24, 29).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 8.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(2.0F, 17.0F, 0.5F));

        PartDefinition right_arm_group = modelPartData.addOrReplaceChild("right_arm_group", CubeListBuilder.create().texOffs(32, 29).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 8.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.0F, 17.0F, 0.5F));

        PartDefinition skirt_group = modelPartData.addOrReplaceChild("skirt_group", CubeListBuilder.create().texOffs(25, 16).addBox(-2.5F, -1.0F, -1.5F, 5.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
        .texOffs(42, 56).addBox(-3.0F, 0.0F, -2.0F, 6.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
        .texOffs(0, 57).addBox(-3.5F, 1.0F, -2.0F, 7.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
        .texOffs(0, 48).addBox(-4.0F, 3.0F, -2.5F, 8.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
        .texOffs(15, 57).addBox(-4.0F, 2.0F, -2.5F, 8.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.5F, 24.0F, 0.5F));

        PartDefinition left_leg_group = modelPartData.addOrReplaceChild("left_leg_group", CubeListBuilder.create().texOffs(24, 39).addBox(-1.0F, 3.0F, 0.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F))
        .texOffs(23, 38).addBox(-1.0F, 8.0F, -1.0F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition right_leg_group = modelPartData.addOrReplaceChild("right_leg_group", CubeListBuilder.create().texOffs(32, 39).addBox(-4.0F, 3.0F, 0.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F))
        .texOffs(23, 38).addBox(-4.0F, 8.0F, -1.0F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));
        return LayerDefinition.create(modelData, 64, 64);
    }
    @Override
    public void setupAnim(SprenRenderState state) {
        super.setupAnim(state);

        head_group.resetPose();
        hair_group.resetPose();
        body_group.resetPose();
        left_arm_group.resetPose();
        right_arm_group.resetPose();
        skirt_group.resetPose();
        left_leg_group.resetPose();
        right_leg_group.resetPose();

        float age = state.ageInTicks;
        float speed01 = Math.min(state.horizontalSpeed / 0.35F, 1.0F);
        float phaseAge = age + state.whimsyOffset;

        // Layer several slow rhythms together so no body part ever settles
        // into one visibly repeating "robot" loop.
        float slowA = (float) Math.sin(phaseAge * 0.071F);
        float slowB = (float) Math.sin(phaseAge * 0.043F + 1.7F);
        float mediumA = (float) Math.sin(phaseAge * 0.137F + 0.6F);
        float mediumB = (float) Math.sin(phaseAge * 0.109F + 2.1F);

        float bob =
                slowA * (0.075F + speed01 * 0.025F)
                        + slowB * 0.025F;

        float twirlProgress = playfulTwirlProgress(phaseAge);
        float twirlFlourish =
                twirlProgress > 0.0F
                        ? (float) Math.sin(twirlProgress * Math.PI)
                        : 0.0F;

        // Even at rest she should look alive, not posed.
        head_group.y += bob;
        body_group.y += bob * 0.85F;
        left_arm_group.y += bob;
        right_arm_group.y += bob;
        skirt_group.y += bob * 0.8F;
        left_leg_group.y += bob * 0.75F;
        right_leg_group.y += bob * 0.75F;

        // BODY: tiny asymmetric breathing/sway continuously.
        body_group.zRot =
                slowA * 0.025F
                        + slowB * 0.015F;
        body_group.yRot =
                slowB * 0.018F;

        /*
         * HEAD:
         * continuous curiosity rather than a fixed flying stare.
         * It still counter-tilts upward during travel, but gently looks around,
         * nods and cocks to one side at different rhythms.
         */
        float headLookSide =
                slowB * 0.10F
                        + mediumA * 0.035F;
        float headNod =
                slowA * 0.025F
                        + mediumB * 0.015F;
        float headCock =
                (float) Math.sin(phaseAge * 0.052F + 2.8F) * 0.045F;

        head_group.xRot =
                -speed01 * 0.28F
                        + headNod;
        head_group.yRot = headLookSide;
        head_group.zRot = headCock;

        /*
         * ARMS:
         * preserve the user's established signs:
         *   positive X = trailing backwards
         *   left negative Z / right positive Z = spreading outward
         *
         * The arms now constantly breathe, rise/fall independently and
         * respond more strongly as she travels.
         */
        float travelArmPitch = speed01 * 0.48F;

        float leftArmPitchLife =
                (float) Math.sin(phaseAge * 0.163F + 0.2F) * (0.08F + speed01 * 0.07F)
                        + (float) Math.sin(phaseAge * 0.071F + 2.0F) * 0.035F;

        float rightArmPitchLife =
                (float) Math.sin(phaseAge * 0.147F + 2.4F) * (0.08F + speed01 * 0.07F)
                        + (float) Math.sin(phaseAge * 0.083F + 0.7F) * 0.035F;

        left_arm_group.xRot =
                travelArmPitch + leftArmPitchLife;
        right_arm_group.xRot =
                travelArmPitch + rightArmPitchLife;

        float baseArmOpen =
                0.14F
                        + speed01 * 0.48F
                        + twirlFlourish * 0.62F;

        float leftArmOpenLife =
                (float) Math.sin(phaseAge * 0.121F + 0.5F) * (0.055F + speed01 * 0.025F);
        float rightArmOpenLife =
                (float) Math.sin(phaseAge * 0.113F + 2.2F) * (0.055F + speed01 * 0.025F);

        // DO NOT flip these signs: this is the user's tested outward direction.
        left_arm_group.zRot =
                -baseArmOpen + leftArmOpenLife;
        right_arm_group.zRot =
                baseArmOpen + rightArmOpenLife;

        // Small opposing yaw adds depth and lets the hands describe little arcs
        // through the air instead of moving on flat hinges.
        float armYawBase =
                speed01 * 0.09F
                        + twirlFlourish * 0.14F;

        left_arm_group.yRot =
                -armYawBase
                        + (float) Math.sin(phaseAge * 0.095F + 1.1F) * 0.045F;
        right_arm_group.yRot =
                armYawBase
                        + (float) Math.sin(phaseAge * 0.087F + 2.9F) * 0.045F;

        /*
         * LEGS:
         * still trail backwards in flight, but they now drift independently
         * rather than holding the same swept-back pose.
         */
        float legTrail = speed01 * 0.50F;

        float leftLegLife =
                (float) Math.sin(phaseAge * 0.126F + 0.4F) * (0.055F + speed01 * 0.035F)
                        + slowB * 0.018F;
        float rightLegLife =
                (float) Math.sin(phaseAge * 0.119F + 2.5F) * (0.055F + speed01 * 0.035F)
                        - slowB * 0.018F;

        left_leg_group.xRot = legTrail + leftLegLife;
        right_leg_group.xRot = legTrail + rightLegLife;

        left_leg_group.zRot =
                (float) Math.sin(phaseAge * 0.091F + 0.8F) * 0.025F;
        right_leg_group.zRot =
                (float) Math.sin(phaseAge * 0.097F + 2.6F) * 0.025F;

        /*
         * Hair and dress remain secondary motion. They use different timings
         * from the limbs so the entire figure doesn't pulse in unison.
         */
        hair_group.xRot =
                speed01 * 0.40F
                        + (float) Math.sin(phaseAge * 0.101F + 0.8F) * 0.040F
                        + (float) Math.sin(phaseAge * 0.227F) * 0.020F * speed01;
        hair_group.zRot =
                (float) Math.sin(phaseAge * 0.067F + 2.0F) * 0.032F
                        + (float) Math.sin(phaseAge * 0.157F) * 0.016F * speed01;

        skirt_group.xRot =
                -speed01 * 0.18F
                        + (float) Math.sin(phaseAge * 0.089F + 2.0F) * 0.024F;
        skirt_group.zRot =
                (float) Math.sin(phaseAge * 0.061F + 1.3F) * 0.022F;
        skirt_group.yRot =
                (float) Math.sin(phaseAge * 0.077F + 2.6F) * 0.018F;
    }

    /**
     * One short playful twirl roughly every 13 seconds. Outside the twirl
     * window this returns zero. During the window it eases from 0 -> 1.
     */
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

        // Smoothstep makes the start and finish less robotic.
        return t * t * (3.0F - 2.0F * t);
    }

}
