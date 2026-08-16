package com.scrotey.stormlight.worldgen.feature;

import com.mojang.serialization.Codec;
import com.scrotey.stormlight.Stormlight;
import com.scrotey.stormlight.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Map;
import java.util.Optional;

/**
 * Places the authored stormlight:chrysalis structure template.
 *
 * Natural chrysalis placement is now driven by the post-Highstorm spawner,
 * not by chunk generation. The Feature registration remains so the existing
 * template/data plumbing stays usable, but no biome attaches it naturally.
 */
public final class ChrysalisFeature
        extends Feature<NoneFeatureConfiguration> {

    private static final Identifier TEMPLATE_ID =
            Stormlight.id("chrysalis");

    private static final int BURIAL_DEPTH = 4;
    private static final int MAX_SURFACE_VARIATION = 6;
    private static final int FOOTPRINT_MARGIN = 1;

    private static final Rotation[] ROTATIONS = {
            Rotation.NONE,
            Rotation.CLOCKWISE_90,
            Rotation.CLOCKWISE_180,
            Rotation.COUNTERCLOCKWISE_90
    };

    public ChrysalisFeature(
            Codec<NoneFeatureConfiguration> codec
    ) {
        super(codec);
    }

    @Override
    public boolean place(
            FeaturePlaceContext<NoneFeatureConfiguration> context
    ) {
        return tryPlaceAt(
                context.level().getLevel(),
                context.origin(),
                context.random()
        ).isPresent();
    }

    /**
     * Fast preflight used by the post-Highstorm search. It tests all four
     * rotations and does not modify the world.
     */
    public static boolean canPlaceAt(
            ServerLevel level,
            BlockPos origin
    ) {
        Optional<StructureTemplate> optionalTemplate = getTemplate(level);
        if (optionalTemplate.isEmpty()) {
            return false;
        }

        StructureTemplate template = optionalTemplate.get();
        Vec3i size = template.getSize();

        if (!hasValidSize(size)) {
            return false;
        }

        for (Rotation rotation : ROTATIONS) {
            if (findValidSurfaceAirY(
                    level,
                    origin,
                    size,
                    rotation
            ) != Integer.MIN_VALUE) {
                return true;
            }
        }

        return false;
    }

    /**
     * Attempts to place a chrysalis at the supplied X/Z location. The method
     * tries all four rotations, beginning from a random rotation, and returns
     * the final chrysalis and Gemheart positions when successful.
     */
    public static Optional<PlacementResult> tryPlaceAt(
            ServerLevel level,
            BlockPos origin,
            RandomSource random
    ) {
        Optional<StructureTemplate> optionalTemplate = getTemplate(level);
        if (optionalTemplate.isEmpty()) {
            return Optional.empty();
        }

        StructureTemplate template = optionalTemplate.get();
        Vec3i size = template.getSize();

        if (!hasValidSize(size)) {
            Stormlight.LOGGER.error(
                    "Structure template {} is empty.",
                    TEMPLATE_ID
            );
            return Optional.empty();
        }

        int rotationStart = random.nextInt(ROTATIONS.length);

        for (int rotationOffset = 0;
             rotationOffset < ROTATIONS.length;
             rotationOffset++) {

            Rotation rotation = ROTATIONS[
                    (rotationStart + rotationOffset) % ROTATIONS.length
            ];

            int surfaceAirY = findValidSurfaceAirY(
                    level,
                    origin,
                    size,
                    rotation
            );

            if (surfaceAirY == Integer.MIN_VALUE) {
                continue;
            }

            Optional<PlacementResult> result = placeTemplate(
                    level,
                    origin,
                    surfaceAirY,
                    template,
                    size,
                    rotation,
                    random
            );

            if (result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();
    }

    /**
     * Validates a candidate using surface information captured while the
     * distant terrain chunks were definitely loaded.
     *
     * This method performs no world height lookups. The returned site stores
     * both the authoritative surface Y and the exact rotation that passed.
     */
    public static Optional<ValidatedSite> validateCapturedSurface(
            ServerLevel level,
            BlockPos origin,
            Map<Long, SurfaceSample> samples
    ) {
        Optional<StructureTemplate> optionalTemplate = getTemplate(level);
        if (optionalTemplate.isEmpty()) {
            return Optional.empty();
        }

        Vec3i size = optionalTemplate.get().getSize();
        if (!hasValidSize(size)) {
            return Optional.empty();
        }

        for (Rotation rotation : ROTATIONS) {
            int surfaceAirY = findValidCapturedSurfaceAirY(
                    origin,
                    size,
                    rotation,
                    samples
            );

            if (surfaceAirY != Integer.MIN_VALUE) {
                return Optional.of(
                        new ValidatedSite(
                                new BlockPos(
                                        origin.getX(),
                                        surfaceAirY,
                                        origin.getZ()
                                ),
                                rotation
                        )
                );
            }
        }

        return Optional.empty();
    }

    /**
     * Places a chrysalis at a site that has already passed the complete
     * flatness / fluid / Badlands validation.
     *
     * The stored Y is authoritative. Do not recalculate terrain height here.
     */
    public static Optional<PlacementResult> tryPlaceAtValidatedSite(
            ServerLevel level,
            ValidatedSite site,
            RandomSource random
    ) {
        Optional<StructureTemplate> optionalTemplate = getTemplate(level);
        if (optionalTemplate.isEmpty()) {
            return Optional.empty();
        }

        StructureTemplate template = optionalTemplate.get();
        Vec3i size = template.getSize();

        if (!hasValidSize(size)) {
            Stormlight.LOGGER.error(
                    "Structure template {} is empty.",
                    TEMPLATE_ID
            );
            return Optional.empty();
        }

        return placeTemplate(
                level,
                site.surfaceOrigin(),
                site.surfaceOrigin().getY(),
                template,
                size,
                site.rotation(),
                random
        );
    }

    private static Optional<PlacementResult> placeTemplate(
            ServerLevel level,
            BlockPos origin,
            int surfaceAirY,
            StructureTemplate template,
            Vec3i size,
            Rotation rotation,
            RandomSource random
    ) {
        BlockPos pivot = new BlockPos(
                size.getX() / 2,
                0,
                size.getZ() / 2
        );

        BlockPos worldCentre = new BlockPos(
                origin.getX(),
                surfaceAirY - BURIAL_DEPTH,
                origin.getZ()
        );

        BlockPos placementOrigin =
                worldCentre.offset(
                        -pivot.getX(),
                        0,
                        -pivot.getZ()
                );

        StructurePlaceSettings settings =
                new StructurePlaceSettings()
                        .setMirror(Mirror.NONE)
                        .setRotation(rotation)
                        .setRotationPivot(pivot)
                        .setIgnoreEntities(false)
                        .setRandom(random)
                        .addProcessor(
                                BlockIgnoreProcessor.STRUCTURE_AND_AIR
                        );

        boolean placed = template.placeInWorld(
                level,
                placementOrigin,
                placementOrigin,
                settings,
                random,
                2
        );

        if (!placed) {
            return Optional.empty();
        }

        Block chosenGemheart = randomGemheart(random);

        var cavityMarkers = template.filterBlocks(
                placementOrigin,
                settings,
                ModBlocks.CAVITY_PLACEHOLDER,
                true
        );

        for (StructureTemplate.StructureBlockInfo info : cavityMarkers) {
            if (!level.isInWorldBounds(info.pos())) {
                continue;
            }

            level.setBlock(
                    info.pos(),
                    Blocks.AIR.defaultBlockState(),
                    2
            );
        }

        var gemheartMarkers = template.filterBlocks(
                placementOrigin,
                settings,
                ModBlocks.GEMHEART_PLACEHOLDER,
                true
        );

        if (gemheartMarkers.isEmpty()) {
            Stormlight.LOGGER.error(
                    "Chrysalis template {} genuinely contains no Gemheart placeholder.",
                    TEMPLATE_ID
            );
            return Optional.empty();
        }

        BlockPos gemheartPos = null;

        for (StructureTemplate.StructureBlockInfo info : gemheartMarkers) {
            if (!level.isInWorldBounds(info.pos())) {
                Stormlight.LOGGER.error(
                        "Gemheart marker for chrysalis {} transformed outside world bounds at {}.",
                        TEMPLATE_ID,
                        info.pos()
                );
                continue;
            }

            level.setBlock(
                    info.pos(),
                    chosenGemheart.defaultBlockState(),
                    2
            );

            if (level.getBlockState(info.pos()).is(chosenGemheart)) {
                if (gemheartPos == null) {
                    gemheartPos = info.pos().immutable();
                }
            } else {
                Stormlight.LOGGER.error(
                        "Failed to replace Gemheart placeholder at {}. Current block is {}.",
                        info.pos(),
                        level.getBlockState(info.pos()).getBlock()
                );
            }
        }

        if (gemheartPos == null) {
            Stormlight.LOGGER.error(
                    "Chrysalis template {} has {} Gemheart marker(s), but none could be replaced successfully.",
                    TEMPLATE_ID,
                    gemheartMarkers.size()
            );
            return Optional.empty();
        }

        return Optional.of(
                new PlacementResult(
                        worldCentre.immutable(),
                        gemheartPos
                )
        );
    }

    private static Optional<StructureTemplate> getTemplate(
            ServerLevel level
    ) {
        StructureTemplateManager templateManager =
                level.getStructureManager();

        Optional<StructureTemplate> optionalTemplate =
                templateManager.get(TEMPLATE_ID);

        if (optionalTemplate.isEmpty()) {
            Stormlight.LOGGER.error(
                    "Could not find structure template {}. Expected data/stormlight/structure/chrysalis.nbt",
                    TEMPLATE_ID
            );
        }

        return optionalTemplate;
    }

    private static boolean hasValidSize(Vec3i size) {
        return size.getX() > 0
                && size.getY() > 0
                && size.getZ() > 0;
    }

    /**
     * Returns the air Y directly above a suitable flat Badlands surface, or
     * Integer.MIN_VALUE when the site should be rejected.
     *
     * Every column in the rotated footprint plus a one-block safety margin
     * must be dry Badlands terrain and the total surface variation may not
     * exceed two blocks.
     */
    private static int findValidSurfaceAirY(
            ServerLevel level,
            BlockPos origin,
            Vec3i templateSize,
            Rotation rotation
    ) {
        boolean quarterTurn =
                rotation == Rotation.CLOCKWISE_90
                        || rotation == Rotation.COUNTERCLOCKWISE_90;

        int footprintX = quarterTurn
                ? templateSize.getZ()
                : templateSize.getX();
        int footprintZ = quarterTurn
                ? templateSize.getX()
                : templateSize.getZ();

        int minDx = -(footprintX / 2) - FOOTPRINT_MARGIN;
        int maxDx = footprintX - (footprintX / 2) - 1
                + FOOTPRINT_MARGIN;
        int minDz = -(footprintZ / 2) - FOOTPRINT_MARGIN;
        int maxDz = footprintZ - (footprintZ / 2) - 1
                + FOOTPRINT_MARGIN;

        int lowestSurfaceY = Integer.MAX_VALUE;
        int highestSurfaceY = Integer.MIN_VALUE;
        long totalSurfaceY = 0L;
        int samples = 0;

        for (int dx = minDx; dx <= maxDx; dx++) {
            for (int dz = minDz; dz <= maxDz; dz++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;

                int surfaceAirY = level.getHeight(
                        Heightmap.Types.WORLD_SURFACE,
                        x,
                        z
                );
                int surfaceY = surfaceAirY - 1;

                BlockPos surfacePos = new BlockPos(
                        x,
                        surfaceY,
                        z
                );

                if (!level.getFluidState(surfacePos).isEmpty()) {
                    return Integer.MIN_VALUE;
                }

                Holder<Biome> biome = level.getBiome(surfacePos);
                if (!isBadlands(biome)) {
                    return Integer.MIN_VALUE;
                }

                lowestSurfaceY = Math.min(
                        lowestSurfaceY,
                        surfaceY
                );
                highestSurfaceY = Math.max(
                        highestSurfaceY,
                        surfaceY
                );

                if (highestSurfaceY - lowestSurfaceY
                        > MAX_SURFACE_VARIATION) {
                    return Integer.MIN_VALUE;
                }

                totalSurfaceY += surfaceY;
                samples++;
            }
        }

        if (samples == 0) {
            return Integer.MIN_VALUE;
        }

        int averageSurfaceY = Math.round(
                (float) totalSurfaceY / samples
        );

        return averageSurfaceY + 1;
    }

    private static int findValidCapturedSurfaceAirY(
            BlockPos origin,
            Vec3i templateSize,
            Rotation rotation,
            Map<Long, SurfaceSample> samples
    ) {
        boolean quarterTurn =
                rotation == Rotation.CLOCKWISE_90
                        || rotation == Rotation.COUNTERCLOCKWISE_90;

        int footprintX = quarterTurn
                ? templateSize.getZ()
                : templateSize.getX();
        int footprintZ = quarterTurn
                ? templateSize.getX()
                : templateSize.getZ();

        int minDx = -(footprintX / 2) - FOOTPRINT_MARGIN;
        int maxDx = footprintX - (footprintX / 2) - 1
                + FOOTPRINT_MARGIN;
        int minDz = -(footprintZ / 2) - FOOTPRINT_MARGIN;
        int maxDz = footprintZ - (footprintZ / 2) - 1
                + FOOTPRINT_MARGIN;

        int lowestSurfaceY = Integer.MAX_VALUE;
        int highestSurfaceY = Integer.MIN_VALUE;
        long totalSurfaceY = 0L;
        int sampleCount = 0;

        for (int dx = minDx; dx <= maxDx; dx++) {
            for (int dz = minDz; dz <= maxDz; dz++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;

                SurfaceSample sample =
                        samples.get(horizontalKey(x, z));

                // The centre candidate is already required to be Badlands by
                // ChrysalisSpawner.  For the actual structure footprint we only
                // care that the captured terrain exists and is dry; insisting
                // every edge column is technically the same biome rejected far
                // too many otherwise excellent Badlands plateau sites.
                if (sample == null || !sample.dry()) {
                    return Integer.MIN_VALUE;
                }

                int surfaceY = sample.surfaceAirY() - 1;

                lowestSurfaceY = Math.min(
                        lowestSurfaceY,
                        surfaceY
                );
                highestSurfaceY = Math.max(
                        highestSurfaceY,
                        surfaceY
                );

                if (highestSurfaceY - lowestSurfaceY
                        > MAX_SURFACE_VARIATION) {
                    return Integer.MIN_VALUE;
                }

                totalSurfaceY += surfaceY;
                sampleCount++;
            }
        }

        if (sampleCount == 0) {
            return Integer.MIN_VALUE;
        }

        int averageSurfaceY = Math.round(
                (float) totalSurfaceY / sampleCount
        );

        return averageSurfaceY + 1;
    }

    private static long horizontalKey(int x, int z) {
        return BlockPos.asLong(x, 0, z);
    }

    private static boolean isBadlands(Holder<Biome> biome) {
        return biome.is(Biomes.BADLANDS)
                || biome.is(Biomes.ERODED_BADLANDS)
                || biome.is(Biomes.WOODED_BADLANDS);
    }

    private static Block randomGemheart(RandomSource random) {
        return switch (random.nextInt(5)) {
            case 0 -> ModBlocks.DIAMOND_GEMHEART;
            case 1 -> ModBlocks.GARNET_GEMHEART;
            case 2 -> ModBlocks.RUBY_GEMHEART;
            case 3 -> ModBlocks.SAPPHIRE_GEMHEART;
            default -> ModBlocks.EMERALD_GEMHEART;
        };
    }

    public record SurfaceSample(
            int surfaceAirY,
            boolean dry,
            boolean badlands
    ) {
    }

    public record ValidatedSite(
            BlockPos surfaceOrigin,
            Rotation rotation
    ) {
    }

    public record PlacementResult(
            BlockPos chrysalisPos,
            BlockPos gemheartPos
    ) {
    }
}
