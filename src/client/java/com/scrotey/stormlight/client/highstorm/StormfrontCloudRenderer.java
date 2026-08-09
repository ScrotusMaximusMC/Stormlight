package com.scrotey.stormlight.client.highstorm;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.scrotey.stormlight.Stormlight;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Optional;
import java.util.OptionalDouble;

public final class StormfrontCloudRenderer {
    /*
     * The distant storm is broad but shallow. As it reaches the
     * player, it becomes narrower but considerably deeper.
     */
    private static final int FAR_HALF_COLUMNS = 65;
    private static final int NEAR_HALF_COLUMNS = 15;

    private static final int FAR_HALF_LAYERS = 4;
    private static final int MID_HALF_LAYERS = 8;
    private static final int NEAR_HALF_LAYERS = 15;

    private static final int MIN_ROW = 2;
    private static final int MAX_ROW = 7;
    /*
     * Lowers the entire cloud formation without adding another
     * complete row directly over the player.
     */
    private static final float CLOUD_VERTICAL_OFFSET = -1.0F;

    private static final float COLUMN_SPACING = 28.0F;
    private static final float LAYER_SPACING = 32.0F;

    /*
     * One visible internal lightning event every twelve seconds.
     */
    private static final long LIGHTNING_PERIOD_TICKS =
            12L * 20L;

    /*
     * Camera-relative position of the cloud mass.
     * Positive X is east; negative X is west.
     */
    private static final float START_DISTANCE = 800.0F;
    private static final float IMPACT_DISTANCE = 0.0F;
    private static final float DEPARTURE_DISTANCE = -800.0F;

    private static final RenderPipeline STORMFRONT_PIPELINE =
            RenderPipelines.register(
                    RenderPipeline.builder(
                                    RenderPipelines.DEBUG_FILLED_SNIPPET
                            )
                            .withLocation(
                                    Stormlight.id(
                                            "pipeline/stormfront_clouds"
                                    )
                            )
                            .build()
            );

    /*
     * Large enough for several hundred cloud cuboids.
     */
    private static final StagedVertexBuffer STAGED_BUFFER =
            new StagedVertexBuffer(
                    () -> "Stormlight stormfront clouds",
                    4 * 1024 * 1024
            );

    private static final Vector4f COLOR_MODULATOR =
            new Vector4f(
                    1.0F,
                    1.0F,
                    1.0F,
                    1.0F
            );

    private static final Vector3f MODEL_OFFSET =
            new Vector3f();

    private static final Matrix4f TEXTURE_MATRIX =
            new Matrix4f();

    /*
     * Extraction and drawing can eventually occur on
     * different threads, so the saved state is immutable.
     */
    private static volatile StormfrontRenderState renderState;

    private StormfrontCloudRenderer() {
    }

    public static void register() {
        LevelExtractionEvents.END_EXTRACTION.register(
                StormfrontCloudRenderer::extract
        );

        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(
                StormfrontCloudRenderer::renderAndDraw
        );
    }

    private static void extract(
            LevelExtractionContext context
    ) {
        Minecraft client = Minecraft.getInstance();

        if (client.level == null
                || client.player == null
                || !ClientHighstormState.isStormfrontVisible()
                || !client.level.dimension()
                .equals(Level.OVERWORLD)) {
            renderState = null;
            return;
        }

        renderState =
                new StormfrontRenderState(
                        ClientHighstormState
                                .getApproachProgress(),
                        ClientHighstormState
                                .getPassingProgress(),
                        ClientHighstormState
                                .isHighstorm(),
                        ClientHighstormState
                                .isPassing(),
                        client.level.getGameTime()
                );
    }

    private static void renderAndDraw(
            LevelRenderContext context
    ) {
        StormfrontRenderState state = renderState;

        if (state == null) {
            return;
        }

        VertexFormat formatBinding =
                STORMFRONT_PIPELINE
                        .getVertexFormatBinding(0);

        if (formatBinding == null) {
            return;
        }

        PrimitiveTopology primitive =
                STORMFRONT_PIPELINE
                        .getPrimitiveTopology();

        StagedVertexBuffer.Draw draw =
                STAGED_BUFFER.appendDraw(
                        formatBinding,
                        primitive,
                        primitive
                                == PrimitiveTopology.QUADS
                                ? RenderSystem
                                .getProjectionType()
                                .vertexSorting()
                                : null
                );

        renderStormfront(
                context,
                draw,
                state
        );

        STAGED_BUFFER.upload();

        StagedVertexBuffer.ExecuteInfo info =
                STAGED_BUFFER.getExecuteInfo(draw);

        if (info != null) {
            drawToScreen(
                    Minecraft.getInstance(),
                    info
            );
        }

        STAGED_BUFFER.endFrame();
    }

    private static void renderStormfront(
            LevelRenderContext context,
            StagedVertexBuffer.Draw draw,
            StormfrontRenderState state
    ) {
        PoseStack poseStack = context.poseStack();

        poseStack.pushPose();

        VertexConsumer builder =
                STAGED_BUFFER.getVertexBuilder(draw);

        Matrix4fc matrix =
                poseStack.last().pose();

        float cloudIntensity;
        float distance;

        int halfColumnRange;
        int halfLayerRange;
        float layerPositionOffset;

        if (state.passing()) {
            float passingProgress =
                    state.passingProgress();

            float smoothProgress =
                    smoothStep(passingProgress);

            /*
             * The departing cloud mass travels west while becoming
             * broader and shallower again in the distance.
             */
            distance =
                    IMPACT_DISTANCE
                            + (
                            DEPARTURE_DISTANCE
                                    - IMPACT_DISTANCE
                    ) * smoothProgress;

            cloudIntensity =
                    1.0F - passingProgress;

            halfColumnRange =
                    Math.round(
                            NEAR_HALF_COLUMNS
                                    + (
                                    FAR_HALF_COLUMNS
                                            - NEAR_HALF_COLUMNS
                            ) * smoothProgress
                    );

            halfLayerRange =
                    selectDepartingLayerRange(
                            passingProgress
                    );

            /*
             * The retained layers are on the eastern side of the
             * permanent formation. Gradually remove their positional
             * bias as the departing wall becomes shallow again.
             */
            layerPositionOffset =
                    -(
                            NEAR_HALF_LAYERS
                                    - FAR_HALF_LAYERS
                    )
                            * LAYER_SPACING
                            * smoothProgress;
        } else if (state.highstorm()) {
            /*
             * During the Highstorm, the cloud mass is deepest and
             * remains centred over the player.
             */
            distance = IMPACT_DISTANCE;
            cloudIntensity = 1.0F;

            halfColumnRange =
                    NEAR_HALF_COLUMNS;

            halfLayerRange =
                    NEAR_HALF_LAYERS;

            layerPositionOffset = 0.0F;
        } else {
            float approachProgress =
                    state.approachProgress();

            float smoothProgress =
                    smoothStep(approachProgress);

            /*
             * The approaching wall narrows as perspective requires
             * less total width, while gaining additional depth.
             */
            distance =
                    START_DISTANCE
                            + (
                            IMPACT_DISTANCE
                                    - START_DISTANCE
                    ) * smoothProgress;

            cloudIntensity =
                    approachProgress;

            halfColumnRange =
                    Math.round(
                            FAR_HALF_COLUMNS
                                    + (
                                    NEAR_HALF_COLUMNS
                                            - FAR_HALF_COLUMNS
                            ) * smoothProgress
                    );

            halfLayerRange =
                    selectApproachingLayerRange(
                            approachProgress
                    );

            /*
             * The initial shallow formation is centred on its travel
             * position. This compensation disappears as layers are
             * appended eastwards behind the visible front.
             */
            layerPositionOffset =
                    (
                            NEAR_HALF_LAYERS
                                    - FAR_HALF_LAYERS
                    )
                            * LAYER_SPACING
                            * (1.0F - smoothProgress);
        }

        /*
         * Calculates a single lightning event for the entire cloud mass.
         * Most frames return zero, meaning no flash is active.
         */
        float lightningStrength =
                calculateLightningStrength(
                        state.gameTime(),
                        cloudIntensity
                );

        /*
         * Every possible layer has a permanent index from west to east.
         * Approaching layers grow eastwards; passing layers shrink by
         * losing their western side.
         */
        for (int logicalLayer = -halfLayerRange;
             logicalLayer <= halfLayerRange;
             logicalLayer++) {
            int depthLayer =
                    logicalLayer + halfLayerRange;

            int canonicalLayer;

            if (state.passing()) {
                canonicalLayer =
                        NEAR_HALF_LAYERS * 2
                                - halfLayerRange * 2
                                + depthLayer;
            } else {
                canonicalLayer = depthLayer;
            }

            int playerFacingDepth =
                    state.passing()
                            ? halfLayerRange * 2 - depthLayer
                            : depthLayer;

            renderCloudLayer(
                    matrix,
                    builder,
                    distance,
                    layerPositionOffset,
                    cloudIntensity,
                    state.gameTime(),
                    canonicalLayer,
                    playerFacingDepth,
                    halfLayerRange,
                    halfColumnRange,
                    lightningStrength
            );
        }

        poseStack.popPose();
    }

    private static float smoothStep(
            float progress
    ) {
        return progress
                * progress
                * (3.0F - 2.0F * progress);
    }

    private static float calculateLightningStrength(
            long gameTime,
            float cloudIntensity
    ) {
        long tickInWindow =
                Math.floorMod(
                        gameTime,
                        LIGHTNING_PERIOD_TICKS
                );

        float flashStrength;

        /*
         * Strong initial flash lasting four ticks.
         */
        if (tickInWindow <= 3L) {
            flashStrength =
                    1.0F
                            - tickInWindow / 4.0F;
        }

        /*
         * A short pause followed by a weaker five-tick flicker.
         */
        else if (tickInWindow >= 7L
                && tickInWindow <= 11L) {
            flashStrength =
                    0.75F
                            * (
                            1.0F
                                    - (
                                    tickInWindow - 7L
                            ) / 5.0F
                    );
        } else {
            return 0.0F;
        }

        return flashStrength * cloudIntensity;
    }

    private static int selectApproachingLayerRange(
            float progress
    ) {
        if (progress < 0.35F) {
            return FAR_HALF_LAYERS;
        }

        if (progress < 0.70F) {
            return MID_HALF_LAYERS;
        }

        return NEAR_HALF_LAYERS;
    }

    private static int selectDepartingLayerRange(
            float progress
    ) {
        if (progress < 0.35F) {
            return NEAR_HALF_LAYERS;
        }

        if (progress < 0.70F) {
            return MID_HALF_LAYERS;
        }

        return FAR_HALF_LAYERS;
    }

    private static void renderCloudLayer(
            Matrix4fc matrix,
            VertexConsumer builder,
            float distance,
            float layerPositionOffset,
            float cloudIntensity,
            long gameTime,
            int canonicalLayer,
            int playerFacingDepth,
            int halfLayerRange,
            int halfColumnRange,
            float lightningStrength
    ) {

        /*
         * Each lightning event selects one area of the storm wall.
         * These values are calculated once per layer, not once per box.
         */
        float lightningColumn = 0.0F;
        float lightningRow = 0.0F;

        if (lightningStrength > 0.0F) {
            long flashWindow =
                    Math.floorDiv(
                            gameTime,
                            LIGHTNING_PERIOD_TICKS
                    );

            int flashSeed =
                    hash(
                            (int) (
                                    flashWindow
                                            ^ (flashWindow >>> 32)
                            ),
                            173,
                            -91
                    );

            /*
             * Keep flashes within the central 60% of the formation so
             * they are considerably more likely to appear on screen.
             */
            lightningColumn =
                    (
                            randomFraction(flashSeed)
                                    * 2.0F
                                    - 1.0F
                    ) * halfColumnRange * 0.60F;

            lightningRow =
                    MIN_ROW
                            + randomFraction(
                            hash(
                                    flashSeed,
                                    -221,
                                    347
                            )
                    ) * (
                            MAX_ROW
                                    - MIN_ROW
                    ) * 0.70F;
        }

        for (int row = MIN_ROW;
             row <= MAX_ROW;
             row++) {
            for (int column = -halfColumnRange;
                 column <= halfColumnRange;
                 column++) {
                int mainHash =
                        hash(
                                column,
                                row,
                                canonicalLayer
                        );

                /*
                 * The central layer has fewer gaps so the cloud
                 * ceiling remains visually solid.
                 */
                int gapThreshold =
                        playerFacingDepth == 0
                                ? 3
                                : 5;

                if ((mainHash & 15) < gapThreshold) {
                    continue;
                }

                float xJitter =
                        randomFraction(
                                mainHash
                        ) * 10.0F - 5.0F;

                float yJitter =
                        randomFraction(
                                hash(
                                        column + 83,
                                        row - 29,
                                        canonicalLayer + 7
                                )
                        ) * 10.0F - 5.0F;

                float zJitter =
                        randomFraction(
                                hash(
                                        column - 41,
                                        row + 97,
                                        canonicalLayer + 13
                                )
                        ) * 14.0F - 7.0F;

                /*
                 * Larger, overlapping cells replace the numerous
                 * small boxes used by the former 20-layer version.
                 */
                float xSize =
                        36.0F
                                + randomFraction(
                                hash(
                                        column + 11,
                                        row + 17,
                                        canonicalLayer + 19
                                )
                        ) * 20.0F;

                float ySize =
                        22.0F
                                + randomFraction(
                                hash(
                                        column - 23,
                                        row + 31,
                                        canonicalLayer + 37
                                )
                        ) * 14.0F;

                float zSize =
                        38.0F
                                + randomFraction(
                                hash(
                                        column + 47,
                                        row - 53,
                                        canonicalLayer + 43
                                )
                        ) * 22.0F;

                float turbulence =
                        (float) Math.sin(
                                gameTime * 0.045F
                                        + column * 0.38F
                                        + row * 0.27F
                                        + canonicalLayer * 1.7F
                        ) * (
                                0.7F
                                        + cloudIntensity * 1.3F
                        );

                float centreX =
                        distance
                                + (
                                canonicalLayer
                                        - NEAR_HALF_LAYERS
                        ) * LAYER_SPACING
                                + layerPositionOffset
                                + xJitter;

                float centreY =
                        row * 16.0F
                                + CLOUD_VERTICAL_OFFSET
                                + yJitter
                                + turbulence;

                float centreZ =
                        column
                                * COLUMN_SPACING
                                + zJitter
                                + turbulence * 1.5F;

                /*
                 * Very dark charcoal cloud colour. Individual boxes still vary
                 * slightly, preventing the wall from becoming a flat black shape.
                 */
                float baseGrey =
                        0.085F
                                + randomFraction(
                                hash(
                                        column + 101,
                                        row + 59,
                                        canonicalLayer + 71
                                )
                        ) * 0.065F;

                /*
                 * Outer layers are slightly darker, helping create depth.
                 */
                baseGrey -=
                        Math.abs(
                                canonicalLayer
                                        - NEAR_HALF_LAYERS
                        )
                                / (float) NEAR_HALF_LAYERS
                                * 0.035F;

                baseGrey -=
                        cloudIntensity * 0.015F;

                /*
                 * Briefly illuminate a local pocket of cloud during lightning.
                 * The glow fades across columns, rows and depth layers.
                 */
                if (lightningStrength > 0.0F) {
                    float horizontalFalloff =
                            Math.max(
                                    0.0F,
                                    1.0F
                                            - Math.abs(
                                            column - lightningColumn
                                    ) / 16.0F
                            );

                    float verticalFalloff =
                            Math.max(
                                    0.0F,
                                    1.0F
                                            - Math.abs(
                                            row - lightningRow
                                    ) / 3.5F
                            );

                    /*
                     * The negative-X outer layer is the player-facing surface while
                     * the stormfront approaches from the east. Make that layer the
                     * brightest, then fade the flash into the cloud's depth.
                     */
                    float distanceFromFront =
                            playerFacingDepth;

                    float depthFalloff =
                            Math.max(
                                    0.30F,
                                    1.0F
                                            - distanceFromFront
                                            / Math.max(
                                            1.0F,
                                            halfLayerRange * 2.0F
                                    ) * 0.70F
                            );

                    baseGrey +=
                            lightningStrength
                                    * horizontalFalloff
                                    * verticalFalloff
                                    * depthFalloff
                                    * 0.85F;
                }

                /*
                 * Avoid completely black ordinary clouds and excessively white
                 * lightning flashes.
                 */
                baseGrey =
                        Math.max(
                                0.035F,
                                Math.min(
                                        0.90F,
                                        baseGrey
                                )
                        );

                renderCloudBox(
                        matrix,
                        builder,
                        centreX - xSize / 2.0F,
                        centreY - ySize / 2.0F,
                        centreZ - zSize / 2.0F,
                        centreX + xSize / 2.0F,
                        centreY + ySize / 2.0F,
                        centreZ + zSize / 2.0F,
                        baseGrey
                );
            }
        }
    }

    private static void renderCloudBox(
            Matrix4fc matrix,
            VertexConsumer builder,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ,
            float grey
    ) {
        /*
         * Separate face brightness gives each cloud cell
         * the same thick, block-like depth as fancy clouds.
         */
        float westShade = clampColour(grey * 0.90F);
        float eastShade = clampColour(grey * 0.58F);
        float sideShade = clampColour(grey * 0.76F);
        float topShade = clampColour(grey * 1.12F);
        float bottomShade = clampColour(grey * 0.45F);

        // West face, facing the approaching player.
        addVertex(
                builder,
                matrix,
                minX,
                minY,
                minZ,
                westShade
        );

        addVertex(
                builder,
                matrix,
                minX,
                minY,
                maxZ,
                westShade
        );

        addVertex(
                builder,
                matrix,
                minX,
                maxY,
                maxZ,
                westShade
        );

        addVertex(
                builder,
                matrix,
                minX,
                maxY,
                minZ,
                westShade
        );

        // East face.
        addVertex(
                builder,
                matrix,
                maxX,
                minY,
                maxZ,
                eastShade
        );

        addVertex(
                builder,
                matrix,
                maxX,
                minY,
                minZ,
                eastShade
        );

        addVertex(
                builder,
                matrix,
                maxX,
                maxY,
                minZ,
                eastShade
        );

        addVertex(
                builder,
                matrix,
                maxX,
                maxY,
                maxZ,
                eastShade
        );

        // North face.
        addVertex(
                builder,
                matrix,
                maxX,
                minY,
                minZ,
                sideShade
        );

        addVertex(
                builder,
                matrix,
                minX,
                minY,
                minZ,
                sideShade
        );

        addVertex(
                builder,
                matrix,
                minX,
                maxY,
                minZ,
                sideShade
        );

        addVertex(
                builder,
                matrix,
                maxX,
                maxY,
                minZ,
                sideShade
        );

        // South face.
        addVertex(
                builder,
                matrix,
                minX,
                minY,
                maxZ,
                sideShade
        );

        addVertex(
                builder,
                matrix,
                maxX,
                minY,
                maxZ,
                sideShade
        );

        addVertex(
                builder,
                matrix,
                maxX,
                maxY,
                maxZ,
                sideShade
        );

        addVertex(
                builder,
                matrix,
                minX,
                maxY,
                maxZ,
                sideShade
        );

        // Top face.
        addVertex(
                builder,
                matrix,
                minX,
                maxY,
                maxZ,
                topShade
        );

        addVertex(
                builder,
                matrix,
                maxX,
                maxY,
                maxZ,
                topShade
        );

        addVertex(
                builder,
                matrix,
                maxX,
                maxY,
                minZ,
                topShade
        );

        addVertex(
                builder,
                matrix,
                minX,
                maxY,
                minZ,
                topShade
        );

        // Bottom face.
        addVertex(
                builder,
                matrix,
                minX,
                minY,
                minZ,
                bottomShade
        );

        addVertex(
                builder,
                matrix,
                maxX,
                minY,
                minZ,
                bottomShade
        );

        addVertex(
                builder,
                matrix,
                maxX,
                minY,
                maxZ,
                bottomShade
        );

        addVertex(
                builder,
                matrix,
                minX,
                minY,
                maxZ,
                bottomShade
        );
    }

    private static void addVertex(
            VertexConsumer builder,
            Matrix4fc matrix,
            float x,
            float y,
            float z,
            float grey
    ) {
        builder.addVertex(
                        matrix,
                        x,
                        y,
                        z
                )
                .setColor(
                        grey,
                        grey,
                        grey,
                        1.0F
                );
    }

    private static void drawToScreen(
            Minecraft client,
            StagedVertexBuffer.ExecuteInfo info
    ) {
        GpuBufferSlice dynamicTransforms =
                RenderSystem.getDynamicUniforms()
                        .writeTransform(
                                RenderSystem
                                        .getModelViewMatrixCopy(),
                                COLOR_MODULATOR,
                                MODEL_OFFSET,
                                TEXTURE_MATRIX
                        );

        RenderTarget mainTarget =
                client.gameRenderer
                        .mainRenderTarget();

        GpuTextureView colourTexture =
                mainTarget.getColorTextureView();

        if (colourTexture == null) {
            return;
        }

        try (RenderPass renderPass =
                     RenderSystem.getDevice()
                             .createCommandEncoder()
                             .createRenderPass(
                                     () ->
                                             "Stormlight stormfront rendering",
                                     colourTexture,
                                     Optional.empty(),
                                     mainTarget
                                             .getDepthTextureView(),
                                     OptionalDouble.empty()
                             )) {
            renderPass.setPipeline(
                    STORMFRONT_PIPELINE
            );

            RenderSystem.bindDefaultUniforms(
                    renderPass
            );

            renderPass.setUniform(
                    "DynamicTransforms",
                    dynamicTransforms
            );

            renderPass.setVertexBuffer(
                    0,
                    info.vertexBuffer().slice()
            );

            renderPass.setIndexBuffer(
                    info.indexBuffer(),
                    info.indexType()
            );

            renderPass.drawIndexed(
                    info.indexCount(),
                    1,
                    info.firstIndex(),
                    info.baseVertex(),
                    0
            );
        }
    }

    private static int hash(
            int x,
            int y,
            int layer
    ) {
        int value =
                x * 73_428_767
                        ^ y * 91_276_931
                        ^ layer * 43_826_341;

        value ^= value >>> 13;
        value *= 1_274_126_177;
        value ^= value >>> 16;

        return value;
    }

    private static float randomFraction(
            int value
    ) {
        return (value & 0xFFFF)
                / 65535.0F;
    }

    private static float clampColour(
            float colour
    ) {
        return Math.max(
                0.0F,
                Math.min(
                        1.0F,
                        colour
                )
        );
    }

    public static void close() {
        STAGED_BUFFER.close();
    }

    private record StormfrontRenderState(
            float approachProgress,
            float passingProgress,
            boolean highstorm,
            boolean passing,
            long gameTime
    ) {
    }
}