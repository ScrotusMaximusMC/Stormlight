package com.scrotey.stormlight;

import com.scrotey.stormlight.worldgen.biome.ShatteredPlains;
import com.scrotey.stormlight.worldgen.biome.ShatteredPlainsBiomeSource;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;

/*
 * The game test server uses a flat world, so these build a real overworld
 * (and nether) biome source from the registries and sample it directly.
 */
public class ShatteredPlainsGameTests {
    private static final long SEED = 8_675_309L;
    /* 64k x 64k blocks sampled every 256 blocks. */
    private static final int HALF_EXTENT = 32_768;
    private static final int STEP = 256;
    private static final int SURFACE_Y = 96;

    private record Sampled(MultiNoiseBiomeSource source, RandomState randomState) {
        boolean isPlains(int blockX, int blockZ) {
            return source.getNoiseBiome(QuartPos.fromBlock(blockX), QuartPos.fromBlock(SURFACE_Y),
                    QuartPos.fromBlock(blockZ), randomState.sampler()).is(ShatteredPlains.BIOME);
        }
    }

    private static Sampled sample(GameTestHelper h, ResourceKey<MultiNoiseBiomeSourceParameterList> preset,
            ResourceKey<NoiseGeneratorSettings> settings) {
        var registries = h.getLevel().registryAccess();
        var source = MultiNoiseBiomeSource.createFromPreset(
                registries.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).getOrThrow(preset));
        var randomState = RandomState.create(registries, settings, SEED);
        ((ShatteredPlainsBiomeSource) source).stormlight$attachShatteredPlains(randomState);
        return new Sampled(source, randomState);
    }

    @GameTest
    public void overworldCanGenerateShatteredPlains(GameTestHelper h) {
        var overworld = sample(h, MultiNoiseBiomeSourceParameterLists.OVERWORLD, NoiseGeneratorSettings.OVERWORLD);
        h.assertTrue(overworld.source().possibleBiomes().stream().anyMatch(b -> b.is(ShatteredPlains.BIOME)),
                "Shattered Plains is a possible overworld biome");
        var biome = h.getLevel().registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ShatteredPlains.BIOME);
        h.assertTrue(biome.is(BiomeTags.IS_OVERWORLD), "Tagged as an overworld biome");

        var nether = sample(h, MultiNoiseBiomeSourceParameterLists.NETHER, NoiseGeneratorSettings.NETHER);
        h.assertTrue(nether.source().possibleBiomes().stream().noneMatch(b -> b.is(ShatteredPlains.BIOME)),
                "Nether is untouched");
        h.succeed();
    }

    @GameTest(maxTicks = 2400)
    public void shatteredPlainsAreRareAndHuge(GameTestHelper h) {
        var overworld = sample(h, MultiNoiseBiomeSourceParameterLists.OVERWORLD, NoiseGeneratorSettings.OVERWORLD);
        int cells = 2 * HALF_EXTENT / STEP;
        boolean[][] plains = new boolean[cells][cells];
        int count = 0;
        for (int i = 0; i < cells; i++) {
            for (int j = 0; j < cells; j++) {
                plains[i][j] = overworld.isPlains(i * STEP - HALF_EXTENT, j * STEP - HALF_EXTENT);
                if (plains[i][j]) count++;
            }
        }
        double coverage = (double) count / (cells * cells);
        h.assertTrue(coverage > 0.01 && coverage < 0.12, "Covers a small share of the world, was " + coverage);

        // Widest single region, by flood fill.
        int widest = 0;
        boolean[][] seen = new boolean[cells][cells];
        var queue = new java.util.ArrayDeque<int[]>();
        for (int i = 0; i < cells; i++) {
            for (int j = 0; j < cells; j++) {
                if (!plains[i][j] || seen[i][j]) continue;
                int minI = i, maxI = i, minJ = j, maxJ = j;
                seen[i][j] = true;
                queue.add(new int[]{i, j});
                while (!queue.isEmpty()) {
                    int[] p = queue.poll();
                    minI = Math.min(minI, p[0]); maxI = Math.max(maxI, p[0]);
                    minJ = Math.min(minJ, p[1]); maxJ = Math.max(maxJ, p[1]);
                    for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                        int a = p[0] + d[0], b = p[1] + d[1];
                        if (a >= 0 && b >= 0 && a < cells && b < cells && plains[a][b] && !seen[a][b]) {
                            seen[a][b] = true;
                            queue.add(new int[]{a, b});
                        }
                    }
                }
                widest = Math.max(widest, Math.max(maxI - minI, maxJ - minJ) * STEP);
            }
        }
        Stormlight.LOGGER.info("Shattered Plains test sample: coverage {}, widest region {} blocks", coverage, widest);
        h.assertTrue(widest >= 3000, "At least one region spans 3000+ blocks, widest was " + widest);
        h.succeed();
    }

    /* A point with plains at least 600 blocks out in every direction, past the blend band. */
    private static int[] deepInside(GameTestHelper h, Sampled overworld) {
        for (int x = -HALF_EXTENT; x < HALF_EXTENT; x += STEP) {
            search:
            for (int z = -HALF_EXTENT; z < HALF_EXTENT; z += STEP) {
                for (int r = 0; r <= 600; r += 150) {
                    for (int a = 0; a < 16; a++) {
                        double angle = a * Math.PI / 8;
                        if (!overworld.isPlains(x + (int) (r * Math.cos(angle)), z + (int) (r * Math.sin(angle)))) {
                            continue search;
                        }
                    }
                }
                return new int[]{x, z};
            }
        }
        throw h.assertionException("No deep Shattered Plains found");
    }

    @GameTest(maxTicks = 2400)
    public void plainsArePlateausCutByChasms(GameTestHelper h) {
        var overworld = sample(h, MultiNoiseBiomeSourceParameterLists.OVERWORLD, NoiseGeneratorSettings.OVERWORLD);
        var settings = h.getLevel().registryAccess().lookupOrThrow(Registries.NOISE_SETTINGS)
                .getOrThrow(NoiseGeneratorSettings.OVERWORLD);
        var generator = new NoiseBasedChunkGenerator(overworld.source(), settings);
        var heightAccessor = LevelHeightAccessor.create(-64, 384);
        int[] centre = deepInside(h, overworld);

        // A 512 x 512 block patch, every 4 blocks.
        int size = 128;
        int[][] heights = new int[size][size];
        int plateau = 0, chasm = 0, steepDrops = 0, flooded = 0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                int x = centre[0] - 256 + i * 4, z = centre[1] - 256 + j * 4;
                heights[i][j] = generator.getBaseHeight(x, z,
                        Heightmap.Types.OCEAN_FLOOR_WG, heightAccessor, overworld.randomState());
                // Plateaus a little above sea level (63), chasms far below it.
                if (heights[i][j] >= 64 && heights[i][j] <= 90) plateau++;
                if (heights[i][j] <= 40) chasm++;
                // 30+ block drop within 8 blocks
                if (i >= 2 && Math.abs(heights[i][j] - heights[i - 2][j]) >= 30) steepDrops++;
                // Any fluid on top shows up as a higher WORLD_SURFACE than OCEAN_FLOOR.
                if (heights[i][j] < 63 && generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                        heightAccessor, overworld.randomState()) != heights[i][j]) flooded++;
            }
        }
        int total = size * size;
        Stormlight.LOGGER.info("Shattered Plains terrain at {},{}: plateau {}, chasm {}, steep {}, flooded {}",
                centre[0], centre[1], (double) plateau / total, (double) chasm / total, steepDrops, flooded);
        h.assertValueEqual(flooded, 0, "Columns with water in the chasms");
        h.assertTrue(plateau > total * 0.55, "Mostly plateau tops, was " + (double) plateau / total);
        h.assertTrue(chasm > total * 0.05, "Chasms cut the plateaus, was " + (double) chasm / total);
        h.assertTrue(steepDrops > 100, "Chasm walls are steep, steep samples " + steepDrops);
        h.succeed();
    }

    @GameTest
    public void unattachedSourceIsVanilla(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        var source = MultiNoiseBiomeSource.createFromPreset(registries.lookupOrThrow(
                Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
        var unattached = new Sampled(source, RandomState.create(registries, NoiseGeneratorSettings.OVERWORLD, SEED));
        for (int x = -HALF_EXTENT; x < HALF_EXTENT; x += 1024) {
            for (int z = -HALF_EXTENT; z < HALF_EXTENT; z += 1024) {
                h.assertTrue(!unattached.isPlains(x, z), "Climate alone never picks the Shattered Plains");
            }
        }
        h.succeed();
    }
}
