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
    private static final float CLOUD_VERTICAL_OFFSET = 16.0F;

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
     * Extra geometry is reserved for the active storm's lowest row. Rather
     * than shrinking every cloud cell, selected cells grow a handful of
     * smaller, darker fragments beneath them. This breaks up the large-box
     * silhouette at a fraction of the cost of subdividing the whole mass.
     */
    private static final int UNDERBELLY_CHANCE_OUT_OF_16 = 9;
    private static final int UNDERBELLY_EDGE_MARGIN = 2;
    private static final int UNDERBELLY_SUBDIVISIONS = 2;

    /*
     * The detailed cloud field is deliberately finite, but raising its
     * underside makes that edge easier to see. During the active Highstorm,
     * a second, extremely sparse canopy of enormous slabs continues far past
     * it. The canopy bends downward toward the horizon like a shallow bowl,
     * hiding the clear sky beyond the detailed formation without paying for
     * thousands more ordinary cloud cells.
     */
    private static final int FAR_CANOPY_HALF_CELLS = 6;
    private static final float FAR_CANOPY_SPACING = 250.0F;
    private static final float FAR_CANOPY_INNER_RADIUS = 390.0F;
    private static final float FAR_CANOPY_CENTRE_Y = 66.0F;
    private static final float FAR_CANOPY_MAX_DROP = 54.0F;

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
     * Three independent electrical systems cross the storm at different
     * rates. Their prime-numbered periods overlap irregularly, producing
     * frequent local flickers without turning the entire sky into a regular
     * global strobe. Together they begin roughly one new cluster per second
     * during the active Highstorm.
     */
    private static final long LIGHTNING_FAST_PERIOD_TICKS = 43L;
    private static final long LIGHTNING_MEDIUM_PERIOD_TICKS = 67L;
    private static final long LIGHTNING_SLOW_PERIOD_TICKS = 97L;

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
         * Several independently timed pockets can illuminate different parts
         * of the formation at once. This makes electrical activity visible
         * across the enormous storm without flashing the whole sky globally.
         */
        LightningState lightning =
                calculateLightningState(
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
                    lightning,
                    worldAnchored,
                    worldCentreLayer,
                    worldCentreColumn,
                    cameraX,
                    cameraZ
            );
        }

        /*
         * The far canopy belongs only to the world-anchored active storm.
         * Approach and departure keep their existing cinematic silhouettes.
         */
        if (worldAnchored) {
            renderFarCanopy(
                    matrix,
                    builder,
                    state.gameTime(),
                    lightning,
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

    private static LightningState calculateLightningState(
            long gameTime,
            float cloudIntensity
    ) {
        return new LightningState(
                calculateFlash(
                        gameTime,
                        LIGHTNING_FAST_PERIOD_TICKS,
                        11L,
                        173,
                        cloudIntensity,
                        1.00F
                ),
                calculateFlash(
                        gameTime,
                        LIGHTNING_MEDIUM_PERIOD_TICKS,
                        29L,
                        -431,
                        cloudIntensity,
                        0.90F
                ),
                calculateFlash(
                        gameTime,
                        LIGHTNING_SLOW_PERIOD_TICKS,
                        53L,
                        887,
                        cloudIntensity,
                        1.12F
                )
        );
    }

    private static LightningFlash calculateFlash(
            long gameTime,
            long period,
            long timeOffset,
            int salt,
            float cloudIntensity,
            float channelStrength
    ) {
        long shiftedTime = gameTime + timeOffset;

        long tickInWindow =
                Math.floorMod(
                        shiftedTime,
                        period
                );

        long flashWindow =
                Math.floorDiv(
                        shiftedTime,
                        period
                );

        int flashSeed =
                hash(
                        (int) (
                                flashWindow
                                        ^ (flashWindow >>> 32)
                        ),
                        salt,
                        salt * 31
                );

        float flashStrength;

        /*
         * A sharp primary pulse, a second irregular flicker, and an
         * occasional weak after-flash. The seed varies their strength so
         * neighbouring clusters do not repeat the same rhythm.
         */
        if (tickInWindow <= 3L) {
            flashStrength =
                    1.0F
                            - tickInWindow / 4.0F;
        } else if (tickInWindow >= 7L
                && tickInWindow <= 10L) {
            flashStrength =
                    (0.55F
                            + randomFraction(
                            hash(flashSeed, 97, -211)
                    ) * 0.25F)
                            * (
                            1.0F
                                    - (
                                    tickInWindow - 7L
                            ) / 4.0F
                    );
        } else if (tickInWindow >= 14L
                && tickInWindow <= 16L
                && (flashSeed & 3) != 0) {
            flashStrength =
                    0.38F
                            * (
                            1.0F
                                    - (
                                    tickInWindow - 14L
                            ) / 3.0F
                    );
        } else {
            flashStrength = 0.0F;
        }

        /*
         * Early approach lightning is present but subdued. It grows much
         * more quickly in the latter half of the approach and fades with the
         * departing storm.
         */
        float phaseStrength =
                smoothStep(cloudIntensity);

        float strengthVariation =
                0.78F
                        + randomFraction(
                        hash(flashSeed, -337, 619)
                ) * 0.34F;

        return new LightningFlash(
                flashStrength
                        * phaseStrength
                        * channelStrength
                        * strengthVariation,
                randomFraction(
                        hash(flashSeed, 271, -509)
                ) * 1.50F - 0.75F,
                MIN_ROW
                        + randomFraction(
                        hash(flashSeed, -221, 347)
                ) * (MAX_ROW - MIN_ROW) * 0.82F,
                randomFraction(
                        hash(flashSeed, 733, -947)
                )
        );
    }

    private static float calculatePocketGlow(
            LightningFlash flash,
            int column,
            int row,
            int playerFacingDepth,
            int halfLayerRange,
            int halfColumnRange
    ) {
        if (flash.strength() <= 0.0F) {
            return 0.0F;
        }

        float targetColumn =
                flash.columnFactor()
                        * halfColumnRange;

        float horizontalHalo =
                Math.max(
                        0.0F,
                        1.0F
                                - Math.abs(column - targetColumn)
                                / 19.0F
                );

        float verticalHalo =
                Math.max(
                        0.0F,
                        1.0F
                                - Math.abs(row - flash.row())
                                / 4.2F
                );

        float horizontalCore =
                Math.max(
                        0.0F,
                        1.0F
                                - Math.abs(column - targetColumn)
                                / 7.0F
                );

        float verticalCore =
                Math.max(
                        0.0F,
                        1.0F
                                - Math.abs(row - flash.row())
                                / 1.7F
                );

        float targetDepth =
                flash.depthFactor()
                        * halfLayerRange
                        * 2.0F;

        float depthFalloff =
                Math.max(
                        0.28F,
                        1.0F
                                - Math.abs(
                                playerFacingDepth - targetDepth
                        ) / Math.max(
                                1.0F,
                                halfLayerRange * 1.35F
                        )
                );

        float broadGlow =
                horizontalHalo
                        * verticalHalo
                        * 0.46F;

        float electricCore =
                horizontalCore
                        * verticalCore
                        * 0.68F;

        return flash.strength()
                * depthFalloff
                * (broadGlow + electricCore);
    }

    private static float calculateCanopyGlow(
            LightningFlash flash,
            float centreX,
            float centreZ
    ) {
        if (flash.strength() <= 0.0F) {
            return 0.0F;
        }

        float targetX =
                (flash.depthFactor() * 2.0F - 1.0F)
                        * NEAR_HALF_LAYERS
                        * LAYER_SPACING;

        float targetZ =
                flash.columnFactor()
                        * NEAR_HALF_COLUMNS
                        * COLUMN_SPACING;

        float deltaX = centreX - targetX;
        float deltaZ = centreZ - targetZ;

        float distance =
                Mth.sqrt(
                        deltaX * deltaX
                                + deltaZ * deltaZ
                );

        float halo =
                Math.max(
                        0.0F,
                        1.0F - distance / 900.0F
                );

        return flash.strength()
                * halo
                * 0.16F;
    }

    private record LightningFlash(
            float strength,
            float columnFactor,
            float row,
            float depthFactor
    ) {
    }

    private record LightningState(
            LightningFlash fast,
            LightningFlash medium,
            LightningFlash slow
    ) {
        private float glowAt(
                int column,
                int row,
                int playerFacingDepth,
                int halfLayerRange,
                int halfColumnRange
        ) {
            return Math.min(
                    1.0F,
                    calculatePocketGlow(
                            fast,
                            column,
                            row,
                            playerFacingDepth,
                            halfLayerRange,
                            halfColumnRange
                    )
                            + calculatePocketGlow(
                            medium,
                            column,
                            row,
                            playerFacingDepth,
                            halfLayerRange,
                            halfColumnRange
                    )
                            + calculatePocketGlow(
                            slow,
                            column,
                            row,
                            playerFacingDepth,
                            halfLayerRange,
                            halfColumnRange
                    )
            );
        }

        private float canopyGlowAt(
                float centreX,
                float centreZ
        ) {
            return Math.min(
                    0.28F,
                    calculateCanopyGlow(
                            fast,
                            centreX,
                            centreZ
                    )
                            + calculateCanopyGlow(
                            medium,
                            centreX,
                            centreZ
                    )
                            + calculateCanopyGlow(
                            slow,
                            centreX,
                            centreZ
                    )
            );
        }
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
            LightningState lightning,
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

                float lightningGlow =
                        lightning.glowAt(
                                column,
                                row,
                                playerFacingDepth,
                                halfLayerRange,
                                halfColumnRange
                        );

                /*
                 * Keep the cloud itself charcoal, then add a restrained
                 * violet-blue halo and a much brighter electric-blue core.
                 * Separate RGB channels are essential here: the former code
                 * could only brighten clouds toward white.
                 */
                baseGrey =
                        Math.max(
                                0.035F,
                                Math.min(
                                        0.24F,
                                        baseGrey
                                )
                        );

                float red =
                        clampColour(
                                baseGrey
                                        + lightningGlow * 0.22F
                        );

                float green =
                        clampColour(
                                baseGrey
                                        + lightningGlow * 0.48F
                        );

                float blue =
                        clampColour(
                                baseGrey
                                        + lightningGlow * 0.98F
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
                        red,
                        green,
                        blue
                );

                /*
                 * Give the active storm a finer, ragged underside without
                 * multiplying the resolution of all six cloud rows. Detail is
                 * omitted from the fading edge rings, where small geometry
                 * would be expensive and visually prone to popping.
                 */
                if (worldAnchored
                        && row == MIN_ROW
                        && Math.abs(column)
                        <= halfColumnRange
                        - UNDERBELLY_EDGE_MARGIN
                        && Math.abs(logicalLayer)
                        <= halfLayerRange
                        - UNDERBELLY_EDGE_MARGIN) {
                    renderUnderbellyDetail(
                            matrix,
                            builder,
                            centreX,
                            centreY,
                            centreZ,
                            xSize,
                            ySize,
                            zSize,
                            red,
                            green,
                            blue,
                            seedColumn,
                            seedLayer
                    );
                }
            }
        }
    }

    private static void renderUnderbellyDetail(
            Matrix4fc matrix,
            VertexConsumer builder,
            float parentCentreX,
            float parentCentreY,
            float parentCentreZ,
            float parentXSize,
            float parentYSize,
            float parentZSize,
            float parentRed,
            float parentGreen,
            float parentBlue,
            int seedColumn,
            int seedLayer
    ) {
        int detailHash =
                hash(
                        seedColumn + 419,
                        MIN_ROW - 211,
                        seedLayer + 733
                );

        if ((detailHash & 15)
                >= UNDERBELLY_CHANCE_OUT_OF_16) {
            return;
        }

        float parentBottom =
                parentCentreY
                        - parentYSize / 2.0F;

        float sectionX =
                parentXSize
                        / UNDERBELLY_SUBDIVISIONS;

        float sectionZ =
                parentZSize
                        / UNDERBELLY_SUBDIVISIONS;

        for (int subZ = 0;
             subZ < UNDERBELLY_SUBDIVISIONS;
             subZ++) {
            for (int subX = 0;
                 subX < UNDERBELLY_SUBDIVISIONS;
                 subX++) {
                int subHash =
                        hash(
                                seedColumn
                                        * UNDERBELLY_SUBDIVISIONS
                                        + subX,
                                1_007 + subZ,
                                seedLayer
                                        * UNDERBELLY_SUBDIVISIONS
                                        + subZ
                        );

                /*
                 * Each detailed parent produces two to four fragments. The
                 * missing quarters create broken edges rather than a regular
                 * checkerboard beneath every large cloud cell.
                 */
                if ((subHash & 7) < 2) {
                    continue;
                }

                float xSize =
                        sectionX
                                * (0.72F
                                + randomFraction(
                                subHash
                        ) * 0.42F);

                float zSize =
                        sectionZ
                                * (0.72F
                                + randomFraction(
                                hash(
                                        subHash,
                                        337,
                                        -149
                                )
                        ) * 0.42F);

                float ySize =
                        5.0F
                                + randomFraction(
                                hash(
                                        subHash,
                                        -613,
                                        271
                                )
                        ) * 8.0F;

                float localX =
                        (subX + 0.5F)
                                * sectionX
                                - parentXSize / 2.0F;

                float localZ =
                        (subZ + 0.5F)
                                * sectionZ
                                - parentZSize / 2.0F;

                float xJitter =
                        (randomFraction(
                                hash(
                                        subHash,
                                        71,
                                        881
                                )
                        ) - 0.5F) * sectionX * 0.42F;

                float zJitter =
                        (randomFraction(
                                hash(
                                        subHash,
                                        -457,
                                        503
                                )
                        ) - 0.5F) * sectionZ * 0.42F;

                /*
                 * Sink each fragment by a different amount, while leaving its
                 * top slightly embedded in the parent to avoid visible seams.
                 */
                float overlap =
                        1.5F
                                + randomFraction(
                                hash(
                                        subHash,
                                        947,
                                        -829
                                )
                        ) * 2.5F;

                float centreX =
                        parentCentreX
                                + localX
                                + xJitter;

                float centreY =
                        parentBottom
                                - ySize / 2.0F
                                + overlap;

                float centreZ =
                        parentCentreZ
                                + localZ
                                + zJitter;

                float colourScale =
                        0.76F
                                + randomFraction(
                                hash(
                                        subHash,
                                        193,
                                        -367
                                )
                        ) * 0.18F;

                renderCloudBox(
                        matrix,
                        builder,
                        centreX - xSize / 2.0F,
                        centreY - ySize / 2.0F,
                        centreZ - zSize / 2.0F,
                        centreX + xSize / 2.0F,
                        centreY + ySize / 2.0F,
                        centreZ + zSize / 2.0F,
                        clampColour(parentRed * colourScale),
                        clampColour(parentGreen * colourScale),
                        clampColour(parentBlue * colourScale)
                );

            }
        }
    }

    private static void renderFarCanopy(
            Matrix4fc matrix,
            VertexConsumer builder,
            long gameTime,
            LightningState lightning,
            double cameraX,
            double cameraZ
    ) {
        int centreCellX =
                Mth.floor(
                        cameraX / FAR_CANOPY_SPACING
                );

        int centreCellZ =
                Mth.floor(
                        cameraZ / FAR_CANOPY_SPACING
                );

        float maximumRadius =
                FAR_CANOPY_HALF_CELLS
                        * FAR_CANOPY_SPACING;

        for (int localZ = -FAR_CANOPY_HALF_CELLS;
             localZ <= FAR_CANOPY_HALF_CELLS;
             localZ++) {
            for (int localX = -FAR_CANOPY_HALF_CELLS;
                 localX <= FAR_CANOPY_HALF_CELLS;
                 localX++) {
                int worldCellX = centreCellX + localX;
                int worldCellZ = centreCellZ + localZ;

                float centreX =
                        (float) (
                                worldCellX
                                        * (double) FAR_CANOPY_SPACING
                                        - cameraX
                        );

                float centreZ =
                        (float) (
                                worldCellZ
                                        * (double) FAR_CANOPY_SPACING
                                        - cameraZ
                        );

                float horizontalDistance =
                        Mth.sqrt(
                                centreX * centreX
                                        + centreZ * centreZ
                        );

                /*
                 * Leave the inner field to the detailed cloud cells. The
                 * oversized slabs overlap this boundary generously, so no
                 * circular seam is exposed while the player moves.
                 */
                if (horizontalDistance
                        < FAR_CANOPY_INNER_RADIUS) {
                    continue;
                }

                int canopyHash =
                        hash(
                                worldCellX,
                                2_003,
                                worldCellZ
                        );

                float radialProgress =
                        Mth.clamp(
                                (horizontalDistance
                                        - FAR_CANOPY_INNER_RADIUS)
                                        / (maximumRadius
                                        - FAR_CANOPY_INNER_RADIUS),
                                0.0F,
                                1.0F
                        );

                /*
                 * Smooth bowl profile: nearly level beside the detailed
                 * field, then increasingly low toward the distant horizon.
                 */
                float bowlProgress =
                        smoothStep(radialProgress);

                float yJitter =
                        (randomFraction(
                                hash(
                                        worldCellX + 307,
                                        -1_117,
                                        worldCellZ - 461
                                )
                        ) - 0.5F) * 10.0F;

                float turbulence =
                        (float) Math.sin(
                                gameTime * 0.018F
                                        + worldCellX * 0.73F
                                        + worldCellZ * 1.19F
                        ) * 1.5F;

                float centreY =
                        FAR_CANOPY_CENTRE_Y
                                - bowlProgress
                                * FAR_CANOPY_MAX_DROP
                                + yJitter
                                + turbulence;

                /*
                 * Broad overlap makes this a continuous ceiling despite its
                 * very low cell count. Small deterministic variation keeps
                 * the underside from reading as one perfectly flat plane.
                 */
                float xSize =
                        FAR_CANOPY_SPACING
                                * (1.28F
                                + randomFraction(
                                hash(
                                        canopyHash,
                                        811,
                                        -277
                                )
                        ) * 0.18F);

                float zSize =
                        FAR_CANOPY_SPACING
                                * (1.28F
                                + randomFraction(
                                hash(
                                        canopyHash,
                                        -569,
                                        983
                                )
                        ) * 0.18F);

                float ySize =
                        30.0F
                                + randomFraction(
                                hash(
                                        canopyHash,
                                        1_243,
                                        -719
                                )
                        ) * 18.0F;

                float grey =
                        0.055F
                                + randomFraction(
                                hash(
                                        canopyHash,
                                        -1_409,
                                        1_607
                                )
                        ) * 0.035F;

                /*
                 * The sparse distant canopy receives a broad, localised blue
                 * response. It is intentionally weaker than the detailed
                 * electric cores and never illuminates the whole horizon at
                 * once.
                 */
                float canopyGlow =
                        lightning.canopyGlowAt(
                                centreX,
                                centreZ
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
                        clampColour(
                                grey + canopyGlow * 0.18F
                        ),
                        clampColour(
                                grey + canopyGlow * 0.42F
                        ),
                        clampColour(
                                grey + canopyGlow * 0.90F
                        )
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
        renderCloudBox(
                matrix,
                builder,
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ,
                grey,
                grey,
                grey
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
            float red,
            float green,
            float blue
    ) {
        /*
         * Separate face brightness gives each cloud cell
         * the same thick, block-like depth as fancy clouds.
         */
        float westRed = clampColour(red * 0.90F);
        float westGreen = clampColour(green * 0.90F);
        float westBlue = clampColour(blue * 0.90F);

        float eastRed = clampColour(red * 0.58F);
        float eastGreen = clampColour(green * 0.58F);
        float eastBlue = clampColour(blue * 0.58F);

        float sideRed = clampColour(red * 0.76F);
        float sideGreen = clampColour(green * 0.76F);
        float sideBlue = clampColour(blue * 0.76F);

        float topRed = clampColour(red * 1.12F);
        float topGreen = clampColour(green * 1.12F);
        float topBlue = clampColour(blue * 1.12F);

        float bottomRed = clampColour(red * 0.45F);
        float bottomGreen = clampColour(green * 0.45F);
        float bottomBlue = clampColour(blue * 0.45F);

        // West face, facing the approaching player.
        addVertex(
                builder,
                matrix,
                minX,
                minY,
                minZ,
                westRed,
                westGreen,
                westBlue
        );

        addVertex(
                builder,
                matrix,
                minX,
                minY,
                maxZ,
                westRed,
                westGreen,
                westBlue
        );

        addVertex(
                builder,
                matrix,
                minX,
                maxY,
                maxZ,
                westRed,
                westGreen,
                westBlue
        );

        addVertex(
                builder,
                matrix,
                minX,
                maxY,
                minZ,
                westRed,
                westGreen,
                westBlue
        );

        // East face.
        addVertex(
                builder,
                matrix,
                maxX,
                minY,
                maxZ,
                eastRed,
                eastGreen,
                eastBlue
        );

        addVertex(
                builder,
                matrix,
                maxX,
                minY,
                minZ,
                eastRed,
                eastGreen,
                eastBlue
        );

        addVertex(
                builder,
                matrix,
                maxX,
                maxY,
                minZ,
                eastRed,
                eastGreen,
                eastBlue
        );

        addVertex(
                builder,
                matrix,
                maxX,
                maxY,
                maxZ,
                eastRed,
                eastGreen,
                eastBlue
        );

        // North face.
        addVertex(
                builder,
                matrix,
                maxX,
                minY,
                minZ,
                sideRed,
                sideGreen,
                sideBlue
        );

        addVertex(
                builder,
                matrix,
                minX,
                minY,
                minZ,
                sideRed,
                sideGreen,
                sideBlue
        );

        addVertex(
                builder,
                matrix,
                minX,
                maxY,
                minZ,
                sideRed,
                sideGreen,
                sideBlue
        );

        addVertex(
                builder,
                matrix,
                maxX,
                maxY,
                minZ,
                sideRed,
                sideGreen,
                sideBlue
        );

        // South face.
        addVertex(
                builder,
                matrix,
                minX,
                minY,
                maxZ,
                sideRed,
                sideGreen,
                sideBlue
        );

        addVertex(
                builder,
                matrix,
                maxX,
                minY,
                maxZ,
                sideRed,
                sideGreen,
                sideBlue
        );

        addVertex(
                builder,
                matrix,
                maxX,
                maxY,
                maxZ,
                sideRed,
                sideGreen,
                sideBlue
        );

        addVertex(
                builder,
                matrix,
                minX,
                maxY,
                maxZ,
                sideRed,
                sideGreen,
                sideBlue
        );

        // Top face.
        addVertex(
                builder,
                matrix,
                minX,
                maxY,
                maxZ,
                topRed,
                topGreen,
                topBlue
        );

        addVertex(
                builder,
                matrix,
                maxX,
                maxY,
                maxZ,
                topRed,
                topGreen,
                topBlue
        );

        addVertex(
                builder,
                matrix,
                maxX,
                maxY,
                minZ,
                topRed,
                topGreen,
                topBlue
        );

        addVertex(
                builder,
                matrix,
                minX,
                maxY,
                minZ,
                topRed,
                topGreen,
                topBlue
        );

        // Bottom face.
        addVertex(
                builder,
                matrix,
                minX,
                minY,
                minZ,
                bottomRed,
                bottomGreen,
                bottomBlue
        );

        addVertex(
                builder,
                matrix,
                maxX,
                minY,
                minZ,
                bottomRed,
                bottomGreen,
                bottomBlue
        );

        addVertex(
                builder,
                matrix,
                maxX,
                minY,
                maxZ,
                bottomRed,
                bottomGreen,
                bottomBlue
        );

        addVertex(
                builder,
                matrix,
                minX,
                minY,
                maxZ,
                bottomRed,
                bottomGreen,
                bottomBlue
        );
    }

    private static void addVertex(
            VertexConsumer builder,
            Matrix4fc matrix,
            float x,
            float y,
            float z,
            float red,
            float green,
            float blue
    ) {
        builder.addVertex(
                        matrix,
                        x,
                        y,
                        z
                )
                .setColor(
                        red,
                        green,
                        blue,
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
