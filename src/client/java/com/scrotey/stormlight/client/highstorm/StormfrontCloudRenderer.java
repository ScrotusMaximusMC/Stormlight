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
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
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

    /*
     * Above sea level the clouds retain their original camera-relative
     * behaviour. Below this height, the formation is raised by exactly the
     * distance the player has descended, keeping it fixed above Y=64 instead
     * of allowing it to enter caves. Unlike a heightmap lookup, this cannot
     * react to trees, overhangs, mountains, or individual roof blocks.
     */
    private static final float CLOUD_ANCHOR_Y = 64.0F;

    /*
     * The centre heightmap column supplies the actual mountain surface used
     * to anchor the clouds. The eight surrounding columns only confirm that
     * the player is genuinely buried beneath broad terrain rather than below
     * a tree trunk, roof, or narrow overhang. MOTION_BLOCKING_NO_LEAVES keeps
     * leaf canopies out of the calculation.
     *
     * Entering cave mode needs three supporting neighbours. Once active, two
     * are sufficient until the shallower leave depth is reached. This small
     * amount of hysteresis prevents flickering around cave entrances.
     */
    private static final int CAVE_SAMPLE_RADIUS = 8;
    private static final float CAVE_ENTER_DEPTH = 12.0F;
    private static final float CAVE_LEAVE_DEPTH = 7.0F;
    private static final int CAVE_ENTER_SUPPORTS = 3;
    private static final int CAVE_LEAVE_SUPPORTS = 2;
    private static final float CAVE_OFFSET_SMOOTHING = 0.18F;

    private static boolean caveAnchoring;
    private static float smoothedCaveExtraOffset;
    private static long lastOffsetUpdateTick = Long.MIN_VALUE;

    private static final float COLUMN_SPACING = 28.0F;
    private static final float LAYER_SPACING = 32.0F;

    /*
     * Gives both outer faces of the stormwall a rounded profile.
     *
     * The middle cloud rows protrude furthest, while the curve fades
     * through the first few layers so it joins the main cloud mass
     * smoothly.
     */
    private static final float STORMFACE_MAX_BULGE = 48.0F;
    private static final float STORMFACE_CURVE_DEPTH = 4.0F;

    /*
     * One visible internal lightning event every twelve seconds.
     */
    private static final long LIGHTNING_PERIOD_TICKS =
            12L * 20L;

    /*
     * Camera-relative position of the approaching and departing cloud mass.
     * Positive X is east; negative X is west. During the active Highstorm,
     * X and Z instead come from a permanent world-aligned procedural grid.
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
            resetCaveAnchor();
            return;
        }

        float playerY = (float) client.player.getY();

        float seaLevelOffset =
                Math.max(
                        0.0F,
                        CLOUD_ANCHOR_Y
                                - playerY
                );

        int playerX = Mth.floor(client.player.getX());
        int playerZ = Mth.floor(client.player.getZ());

        int[] surfaceHeights = sampleSurfaceHeights(
                client,
                playerX,
                playerZ
        );

        int centreSurface = surfaceHeights[4];
        float centreDepth = centreSurface - playerY;

        int enterSupports = countBuriedNeighbours(
                surfaceHeights,
                playerY,
                CAVE_ENTER_DEPTH
        );

        int leaveSupports = countBuriedNeighbours(
                surfaceHeights,
                playerY,
                CAVE_LEAVE_DEPTH
        );

        if (caveAnchoring) {
            if (centreDepth <= CAVE_LEAVE_DEPTH
                    || leaveSupports
                    < CAVE_LEAVE_SUPPORTS) {
                caveAnchoring = false;
            }
        } else if (centreDepth >= CAVE_ENTER_DEPTH
                && enterSupports
                >= CAVE_ENTER_SUPPORTS) {
            caveAnchoring = true;
        }

        float targetCaveExtraOffset = 0.0F;

        if (caveAnchoring) {
            float caveSurfaceOffset =
                    Math.max(
                            0.0F,
                            centreSurface - playerY
                    );

            targetCaveExtraOffset =
                    Math.max(
                            0.0F,
                            caveSurfaceOffset - seaLevelOffset
                    );
        }

        long gameTime = client.level.getGameTime();

        if (gameTime != lastOffsetUpdateTick) {
            smoothedCaveExtraOffset +=
                    (targetCaveExtraOffset
                            - smoothedCaveExtraOffset)
                            * CAVE_OFFSET_SMOOTHING;

            if (Math.abs(
                    targetCaveExtraOffset
                            - smoothedCaveExtraOffset
            ) < 0.01F) {
                smoothedCaveExtraOffset =
                        targetCaveExtraOffset;
            }

            lastOffsetUpdateTick = gameTime;
        }

        float verticalOffset =
                seaLevelOffset
                        + smoothedCaveExtraOffset;

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
                        gameTime,
                        verticalOffset
                );
    }

    private static int[] sampleSurfaceHeights(
            Minecraft client,
            int centreX,
            int centreZ
    ) {
        int[] heights = new int[9];
        int index = 0;

        for (int zOffset = -CAVE_SAMPLE_RADIUS;
             zOffset <= CAVE_SAMPLE_RADIUS;
             zOffset += CAVE_SAMPLE_RADIUS) {
            for (int xOffset = -CAVE_SAMPLE_RADIUS;
                 xOffset <= CAVE_SAMPLE_RADIUS;
                 xOffset += CAVE_SAMPLE_RADIUS) {
                heights[index++] =
                        client.level.getHeight(
                                Heightmap.Types
                                        .MOTION_BLOCKING_NO_LEAVES,
                                centreX + xOffset,
                                centreZ + zOffset
                        );
            }
        }

        return heights;
    }

    private static int countBuriedNeighbours(
            int[] surfaceHeights,
            float playerY,
            float minimumDepth
    ) {
        int buriedNeighbours = 0;

        for (int index = 0;
             index < surfaceHeights.length;
             index++) {
            // Index four is the centre column and is tested separately.
            if (index == 4) {
                continue;
            }

            if (surfaceHeights[index] - playerY
                    >= minimumDepth) {
                buriedNeighbours++;
            }
        }

        return buriedNeighbours;
    }

    private static void resetCaveAnchor() {
        caveAnchoring = false;
        smoothedCaveExtraOffset = 0.0F;
        lastOffsetUpdateTick = Long.MIN_VALUE;
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

        /*
         * Below Y=64, counteract the player's descent. In a genuine cave
         * above sea level, add the smoothed terrain-surface adjustment.
         */
        poseStack.translate(
                0.0F,
                state.verticalOffset(),
                0.0F
        );

        VertexConsumer builder =
                STAGED_BUFFER.getVertexBuilder(draw);

        Matrix4fc matrix =
                poseStack.last().pose();

        float cloudIntensity;
        float distance;

        int halfColumnRange;
        int halfLayerRange;
        float layerPositionOffset;

        /*
         * The cinematic approach and departure remain camera-relative. Only
         * the fully active storm becomes world-anchored, so walking beneath it
         * reveals new formations instead of carrying the whole ceiling along.
         */
        boolean worldAnchored =
                state.highstorm()
                        && !state.passing();

        /*
         * Use the interpolated render camera rather than the tick-position of
         * the player. This keeps world-fixed clouds perfectly smooth while
         * walking, sprinting, flying, or using third-person view.
         */
        double cameraX =
                context.levelState()
                        .cameraRenderState
                        .pos
                        .x;

        double cameraZ =
                context.levelState()
                        .cameraRenderState
                        .pos
                        .z;

        int worldCentreLayer =
                Mth.floor(
                        cameraX
                                / LAYER_SPACING
                );

        int worldCentreColumn =
                Mth.floor(
                        cameraZ
                                / COLUMN_SPACING
                );

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
                    lightningStrength,
                    worldAnchored,
                    worldCentreLayer,
                    worldCentreColumn,
                    cameraX,
                    cameraZ
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
            float lightningStrength,
            boolean worldAnchored,
            int worldCentreLayer,
            int worldCentreColumn,
            double cameraX,
            double cameraZ
    ) {

        /*
         * The complete storm has permanent layers 0–30:
         *
         * 0  = western outer face, seen during approach
         * 30 = eastern outer face, seen during departure
         *
         * The curvature fades into the first four layers on either side,
         * preventing the cloud wall from becoming a thin curved shell.
         */
        float westSurfaceStrength =
                calculateSurfaceStrength(
                        canonicalLayer
                );

        float eastSurfaceStrength =
                calculateSurfaceStrength(
                        NEAR_HALF_LAYERS * 2
                                - canonicalLayer
                );

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
            /*
             * Produces a symmetrical vertical curve:
             *
             * rows 4–5 protrude furthest
             * rows 3 and 6 protrude moderately
             * rows 2 and 7 remain recessed
             */
            float middleRow =
                    (MIN_ROW + MAX_ROW) * 0.5F;

            float halfRowRange =
                    (MAX_ROW - MIN_ROW) * 0.5F;

            float distanceFromMiddle =
                    Math.abs(row - middleRow)
                            / halfRowRange;

            /*
             * Parabolic profile ranging from approximately 1.0 in the
             * middle to 0.0 at the top and bottom.
             */
            float rowCurve =
                    1.0F
                            - distanceFromMiddle
                            * distanceFromMiddle;

            float rowBulge =
                    STORMFACE_MAX_BULGE
                            * rowCurve;

            /*
             * The western face curves towards negative X.
             * The eastern face curves towards positive X.
             */
            float stormfaceOffset =
                    worldAnchored
                            ? 0.0F
                            : -rowBulge * westSurfaceStrength
                            + rowBulge * eastSurfaceStrength;

            for (int column = -halfColumnRange;
                 column <= halfColumnRange;
                 column++) {
                int logicalLayer =
                        canonicalLayer
                                - NEAR_HALF_LAYERS;

                /*
                 * These are permanent procedural addresses during the active
                 * storm. Their hashes and world positions do not change as the
                 * player moves; only the nearby render window changes.
                 */
                int seedColumn =
                        worldAnchored
                                ? worldCentreColumn + column
                                : column;

                int seedLayer =
                        worldAnchored
                                ? worldCentreLayer
                                + logicalLayer
                                : canonicalLayer;

                int mainHash =
                        hash(
                                seedColumn,
                                row,
                                seedLayer
                        );

                /*
                 * The central layer has fewer gaps so the cloud
                 * ceiling remains visually solid.
                 */
                int gapThreshold =
                        worldAnchored
                                ? 4
                                : playerFacingDepth == 0
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
                                        seedColumn + 83,
                                        row - 29,
                                        seedLayer + 7
                                )
                        ) * 10.0F - 5.0F;

                float zJitter =
                        randomFraction(
                                hash(
                                        seedColumn - 41,
                                        row + 97,
                                        seedLayer + 13
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
                                        seedColumn + 11,
                                        row + 17,
                                        seedLayer + 19
                                )
                        ) * 20.0F;

                float ySize =
                        22.0F
                                + randomFraction(
                                hash(
                                        seedColumn - 23,
                                        row + 31,
                                        seedLayer + 37
                                )
                        ) * 14.0F;

                float zSize =
                        38.0F
                                + randomFraction(
                                hash(
                                        seedColumn + 47,
                                        row - 53,
                                        seedLayer + 43
                                )
                        ) * 22.0F;

                /*
                 * Two reduced outer rings conceal cells entering and leaving
                 * the finite render window. Everything inside those rings is
                 * full-sized and remains visually unchanged while travelling.
                 */
                float windowEdgeScale =
                        worldAnchored
                                ? calculateWindowEdgeScale(
                                column,
                                logicalLayer,
                                halfColumnRange,
                                halfLayerRange
                        )
                                : 1.0F;

                xSize *= windowEdgeScale;
                ySize *= windowEdgeScale;
                zSize *= windowEdgeScale;

                float turbulence =
                        (float) Math.sin(
                                gameTime * 0.045F
                                        + seedColumn * 0.38F
                                        + row * 0.27F
                                        + seedLayer * 1.7F
                        ) * (
                                0.7F
                                        + cloudIntensity * 1.3F
                        );

                float gridX =
                        worldAnchored
                                ? (float) (
                                seedLayer
                                        * (double) LAYER_SPACING
                                        - cameraX
                        )
                                : distance
                                + logicalLayer
                                * LAYER_SPACING
                                + layerPositionOffset;

                float centreX =
                        gridX
                                + stormfaceOffset
                                + xJitter;

                float centreY =
                        row * 16.0F
                                + CLOUD_VERTICAL_OFFSET
                                + yJitter
                                + turbulence;

                float gridZ =
                        worldAnchored
                                ? (float) (
                                seedColumn
                                        * (double) COLUMN_SPACING
                                        - cameraZ
                        )
                                : column
                                * COLUMN_SPACING;

                float centreZ =
                        gridZ
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
                                        seedColumn + 101,
                                        row + 59,
                                        seedLayer + 71
                                )
                        ) * 0.065F;

                /*
                 * Outer layers are slightly darker, helping create depth.
                 */
                if (!worldAnchored) {
                    baseGrey -=
                            Math.abs(
                                    canonicalLayer
                                            - NEAR_HALF_LAYERS
                            )
                                    / (float) NEAR_HALF_LAYERS
                                    * 0.035F;
                }

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

    private static float calculateSurfaceStrength(
            int depthFromSurface
    ) {
        /*
         * 1.0 on the outermost layer, fading smoothly to 0.0
         * after STORMFACE_CURVE_DEPTH layers.
         */
        float strength =
                Math.max(
                        0.0F,
                        1.0F
                                - depthFromSurface
                                / STORMFACE_CURVE_DEPTH
                );

        return strength
                * strength
                * (3.0F - 2.0F * strength);
    }

    private static float calculateWindowEdgeScale(
            int column,
            int logicalLayer,
            int halfColumnRange,
            int halfLayerRange
    ) {
        int columnsFromEdge =
                halfColumnRange
                        - Math.abs(column);

        int layersFromEdge =
                halfLayerRange
                        - Math.abs(logicalLayer);

        int cellsFromEdge =
                Math.min(
                        columnsFromEdge,
                        layersFromEdge
                );

        return Mth.clamp(
                (cellsFromEdge + 1.0F) / 3.0F,
                0.0F,
                1.0F
        );
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
            long gameTime,
            float verticalOffset
    ) {
    }
}