package com.scrotey.stormlight.worldgen.feature;

import com.mojang.serialization.Codec;
import com.scrotey.stormlight.worldgen.biome.ModBiomes;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Shapes the Shattered Plains into large, nearly-flat polygonal plateaus.
 *
 * Instead of accepting whatever terrain the vanilla Overworld noise happens
 * to provide, this feature pulls terrain toward a standard plateau elevation
 * while using deterministic Voronoi cells to define the plateau/chasm layout.
 * Each plateau gets subtle +/-5 block height variation, while per-column height
 * adjustment is capped so the biome still blends into neighbouring terrain.
 *
 * The calculation uses only world X/Z coordinates and only writes inside the
 * current 16x16 chunk. That makes the pattern continuous across chunk borders
 * without unsafe neighbouring-chunk reads during world generation.
 */
public final class ShatteredPlainsSurfaceFeature
        extends Feature<NoneFeatureConfiguration> {

    // Average distance between plateau centres. Larger = broader plateaus.
    private static final int PLATE_SIZE = 160;

    // Width of the chasm band around Voronoi boundaries, in roughly blocks.
    private static final double CHASM_THRESHOLD = 12.0D;

    // Core plateau target. Individual Voronoi plates vary by up to +/-5
    // blocks, giving the biome gentle large-scale height variation without
    // Minecraft-looking stair steps.
    private static final int BASE_PLATEAU_Y = 82;
    private static final int PLATE_HEIGHT_VARIATION = 11;

    // We still respect the vanilla terrain near biome boundaries by limiting
    // how aggressively any single column can be pulled toward the standard
    // plateau height. This prevents giant retaining walls at biome edges.
    private static final int MAX_HEIGHT_ADJUSTMENT = 12;

    // Chasms are relative to the local plateau top rather than an absolute Y.
    private static final int BASE_CHASM_DEPTH = 46;
    private static final int CHASM_DEPTH_VARIATION = 11;

    private static final int SURFACE_DEPTH = 4;

    public ShatteredPlainsSurfaceFeature(
            Codec<NoneFeatureConfiguration> codec
    ) {
        super(codec);
    }

    @Override
    public boolean place(
            FeaturePlaceContext<NoneFeatureConfiguration> context
    ) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();

        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        int maxX = minX + 15;
        int maxZ = minZ + 15;

        boolean changed = false;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int sampleY = level.getHeight(
                        Heightmap.Types.WORLD_SURFACE_WG,
                        x,
                        z
                ) - 1;

                BlockPos samplePos = new BlockPos(x, sampleY, z);

                if (!level.getBiome(samplePos).is(ModBiomes.SHATTERED_PLAINS)) {
                    continue;
                }

                PlateSample plate = samplePlate(x, z);

                int desiredPlateauY = BASE_PLATEAU_Y
                        + plate.heightOffset();

                int targetY = blendTowardPlateau(
                        sampleY,
                        desiredPlateauY
                );

                changed |= normaliseColumn(level, x, z, targetY);

                if (plate.chasm()) {
                    int depth = BASE_CHASM_DEPTH
                            + positiveMod(
                                    hash(x, z, 0x6D2B79F5),
                                    CHASM_DEPTH_VARIATION
                            );

                    int floorY = Math.max(-52, targetY - depth);

                    changed |= carveChasmColumn(
                            level,
                            x,
                            z,
                            targetY,
                            floorY
                    );
                }
            }
        }

        // Add broad red-sand scuffs after the plateaus have been formed.
        int sandPatchCount = 4 + random.nextInt(4);
        for (int i = 0; i < sandPatchCount; i++) {
            changed |= paintPatch(
                    level,
                    minX + random.nextInt(16),
                    minZ + random.nextInt(16),
                    2 + random.nextInt(4),
                    minX,
                    maxX,
                    minZ,
                    maxZ,
                    Blocks.RED_SAND.defaultBlockState(),
                    false,
                    random
            );
        }

        // Small raised packed-mud deposits stand in for accumulated crem.
        int cremPatchCount = 1 + random.nextInt(3);
        for (int i = 0; i < cremPatchCount; i++) {
            changed |= paintPatch(
                    level,
                    minX + random.nextInt(16),
                    minZ + random.nextInt(16),
                    1 + random.nextInt(2),
                    minX,
                    maxX,
                    minZ,
                    maxZ,
                    Blocks.PACKED_MUD.defaultBlockState(),
                    true,
                    random
            );
        }

        return changed;
    }

    private static boolean normaliseColumn(
            WorldGenLevel level,
            int x,
            int z,
            int targetY
    ) {
        int worldTop = level.getHeight(
                Heightmap.Types.WORLD_SURFACE_WG,
                x,
                z
        ) - 1;

        int solidY = findSolidGround(level, x, z, worldTop);

        // Remove hills, snow, trees-in-progress, or water that project above
        // the intended plateau top.
        for (int y = worldTop; y > targetY; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getBlockState(pos).isAir()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
        }

        // If vanilla gave this biome ocean/lowland, reclaim it into a raised
        // landmass. Filling from the first solid block also removes trapped
        // water beneath the new plateau surface.
        if (solidY < targetY) {
            for (int y = solidY + 1; y <= targetY; y++) {
                BlockPos pos = new BlockPos(x, y, z);
                BlockState state = y >= targetY - (SURFACE_DEPTH - 1)
                        ? Blocks.RED_SANDSTONE.defaultBlockState()
                        : Blocks.STONE.defaultBlockState();
                level.setBlock(pos, state, 2);
            }
        }

        // Re-skin the top even when the existing terrain was already taller
        // than our target and has just been cut down.
        for (int depth = 0; depth < SURFACE_DEPTH; depth++) {
            int y = targetY - depth;
            level.setBlock(
                    new BlockPos(x, y, z),
                    Blocks.RED_SANDSTONE.defaultBlockState(),
                    2
            );
        }

        return true;
    }

    private static int findSolidGround(
            WorldGenLevel level,
            int x,
            int z,
            int startY
    ) {
        // Shattered Plains only generates in the Overworld. A floor of -60
        // keeps this scan above bedrock while safely handling deep oceans.
        for (int y = startY; y >= -60; y--) {
            BlockState state = level.getBlockState(new BlockPos(x, y, z));
            if (!state.isAir() && state.getFluidState().isEmpty()) {
                return y;
            }
        }

        return -60;
    }

    private static boolean carveChasmColumn(
            WorldGenLevel level,
            int x,
            int z,
            int topY,
            int floorY
    ) {
        for (int y = topY; y > floorY; y--) {
            level.setBlock(
                    new BlockPos(x, y, z),
                    Blocks.AIR.defaultBlockState(),
                    2
            );
        }

        // A hard sandstone floor keeps the first prototype visually coherent.
        level.setBlock(
                new BlockPos(x, floorY, z),
                Blocks.RED_SANDSTONE.defaultBlockState(),
                2
        );

        return true;
    }

    private static PlateSample samplePlate(int x, int z) {
        int cellX = Math.floorDiv(x, PLATE_SIZE);
        int cellZ = Math.floorDiv(z, PLATE_SIZE);

        double nearest = Double.MAX_VALUE;
        double secondNearest = Double.MAX_VALUE;
        int nearestCellX = 0;
        int nearestCellZ = 0;

        for (int gx = cellX - 1; gx <= cellX + 1; gx++) {
            for (int gz = cellZ - 1; gz <= cellZ + 1; gz++) {
                long h = hash(gx, gz, 0x51ED270B);

                int jitterX = 18 + positiveMod(h, PLATE_SIZE - 36);
                int jitterZ = 18 + positiveMod(h >>> 24, PLATE_SIZE - 36);

                double seedX = (double) gx * PLATE_SIZE + jitterX;
                double seedZ = (double) gz * PLATE_SIZE + jitterZ;

                double dx = x - seedX;
                double dz = z - seedZ;
                double distance = (dx * dx) + (dz * dz);

                if (distance < nearest) {
                    secondNearest = nearest;
                    nearest = distance;
                    nearestCellX = gx;
                    nearestCellZ = gz;
                } else if (distance < secondNearest) {
                    secondNearest = distance;
                }
            }
        }

        double boundaryDistance =
                Math.sqrt(secondNearest) - Math.sqrt(nearest);

        long nearestHash = hash(
                nearestCellX,
                nearestCellZ,
                0x1B873593
        );

        // Every plateau gets a stable height offset in the range -5..+5.
        // This gives broad, natural variation across the biome while keeping
        // each individual plateau essentially level.
        int heightOffset = -5 + positiveMod(
                nearestHash,
                PLATE_HEIGHT_VARIATION
        );

        return new PlateSample(
                heightOffset,
                boundaryDistance <= CHASM_THRESHOLD
        );
    }

    private static int blendTowardPlateau(
            int vanillaSurfaceY,
            int desiredPlateauY
    ) {
        int delta = desiredPlateauY - vanillaSurfaceY;

        int clampedDelta = Math.max(
                -MAX_HEIGHT_ADJUSTMENT,
                Math.min(MAX_HEIGHT_ADJUSTMENT, delta)
        );

        return vanillaSurfaceY + clampedDelta;
    }

    private static boolean paintPatch(
            WorldGenLevel level,
            int centreX,
            int centreZ,
            int radius,
            int minX,
            int maxX,
            int minZ,
            int maxZ,
            BlockState patchState,
            boolean makeLump,
            RandomSource random
    ) {
        boolean changed = false;
        int radiusSq = radius * radius;

        for (int x = Math.max(minX, centreX - radius);
             x <= Math.min(maxX, centreX + radius);
             x++) {
            for (int z = Math.max(minZ, centreZ - radius);
                 z <= Math.min(maxZ, centreZ + radius);
                 z++) {

                int dx = x - centreX;
                int dz = z - centreZ;

                if ((dx * dx) + (dz * dz) > radiusSq
                        || random.nextFloat() < 0.12F) {
                    continue;
                }

                int topY = level.getHeight(
                        Heightmap.Types.WORLD_SURFACE_WG,
                        x,
                        z
                ) - 1;

                BlockPos topPos = new BlockPos(x, topY, z);

                if (!level.getBiome(topPos).is(ModBiomes.SHATTERED_PLAINS)) {
                    continue;
                }

                BlockState current = level.getBlockState(topPos);
                if (!current.is(Blocks.RED_SANDSTONE)
                        && !current.is(Blocks.RED_SAND)
                        && !current.is(Blocks.PACKED_MUD)) {
                    continue;
                }

                level.setBlock(topPos, patchState, 2);
                changed = true;

                if (makeLump
                        && Math.abs(dx) <= 1
                        && Math.abs(dz) <= 1
                        && random.nextFloat() < 0.32F) {
                    BlockPos above = topPos.above();
                    if (level.getBlockState(above).isAir()) {
                        level.setBlock(
                                above,
                                Blocks.PACKED_MUD.defaultBlockState(),
                                2
                        );
                    }
                }
            }
        }

        return changed;
    }

    private static long hash(int x, int z, int salt) {
        long h = ((long) x * 0x9E3779B97F4A7C15L)
                ^ ((long) z * 0xC2B2AE3D27D4EB4FL)
                ^ salt;

        h ^= h >>> 30;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 27;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 31;

        return h;
    }

    private static int positiveMod(long value, int modulus) {
        return (int) Math.floorMod(value, (long) modulus);
    }

    private record PlateSample(int heightOffset, boolean chasm) {
    }
}
