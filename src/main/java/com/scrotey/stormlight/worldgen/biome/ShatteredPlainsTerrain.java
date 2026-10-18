package com.scrotey.stormlight.worldgen.biome;

import com.mojang.serialization.MapCodec;
import com.scrotey.stormlight.Stormlight;

import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;

import java.util.Set;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import org.jspecify.annotations.Nullable;

/*
 * Shapes the Shattered Plains terrain: flat-topped plateaus split by a
 * network of deep, steep-walled chasms.
 *
 * Vanilla terrain height comes from three named density functions:
 * overworld/offset (surface height), overworld/factor (how flat or steep)
 * and overworld/jaggedness (mountain peaks). Caves, aquifers and the
 * preliminary surface estimate are all built on top of these, so instead
 * of replacing the final density, each of the three is wrapped to blend
 * towards plains values wherever the Shattered Plains mask is strong.
 * Everything downstream (caves included) then follows the new shape.
 *
 * The wrapping happens while a RandomState wires its noise router (see
 * RandomStateMixin and DensityFunctionsHolderHolderMixin). That's the one
 * point where the named holders are still identifiable and the level's
 * seeded noises are available.
 */
public final class ShatteredPlainsTerrain {

    private static final Set<String> OVERWORLD_VARIANTS =
            Set.of("overworld", "overworld_large_biomes", "overworld_amplified");

    private static final ResourceKey<NormalNoise.NoiseParameters> HEIGHT_NOISE = noise("shattered_plains_height");
    private static final ResourceKey<NormalNoise.NoiseParameters> WARP_X_NOISE = noise("shattered_plains_warp_x");
    private static final ResourceKey<NormalNoise.NoiseParameters> WARP_Z_NOISE = noise("shattered_plains_warp_z");
    private static final ResourceKey<NormalNoise.NoiseParameters> CHASM_WIDTH_NOISE = noise("shattered_plains_chasm_width");

    /* Vanilla's overworld/depth is (y_gradient + offset) and crosses zero at y = 128 + 128 * offset. */
    private static final double OFFSET_ZERO_Y = 128.0;
    private static final double OFFSET_BLOCKS_PER_UNIT = 128.0;

    /* Vanilla flat terrain sits around 5.5; higher is flatter with sharper edges. */
    private static final double PLAINS_FACTOR = 6.0;

    /* A little above sea level (63). */
    private static final double PLATEAU_Y = 74.0;
    /* Gentle rolling across the plains, plus a fixed step per plateau. */
    private static final double PLATEAU_ROLL = 4.0;
    private static final double PLATEAU_STEP = 6.0;
    /* Well below sea level; the chasms are kept dry (see dryChasms). */
    private static final double CHASM_FLOOR_Y = 22.0;
    private static final double CHASM_FLOOR_ROLL = 6.0;

    /* Plateaus are cells of a jittered grid; chasms run along the cell borders. */
    private static final double PLATEAU_CELL = 96.0;
    private static final double CELL_JITTER = 0.8;
    private static final double WARP_BLOCKS = 24.0;
    /* Half-width of a chasm floor-to-floor, in blocks. */
    private static final double CHASM_MIN_HALF_WIDTH = 4.0;
    private static final double CHASM_MAX_HALF_WIDTH = 9.0;
    /* Horizontal run of a chasm wall from rim to floor. */
    private static final double WALL_RUN = 4.0;

    /* How far past the biome threshold the terrain takes to fully turn into plains (~150 blocks). */
    private static final double MASK_BLEND = 0.12;
    private static final double CONTINENTALNESS_BLEND = 0.12;

    private static final ThreadLocal<@Nullable Wiring> WIRING = new ThreadLocal<>();

    private ShatteredPlainsTerrain() {
    }

    private static ResourceKey<NormalNoise.NoiseParameters> noise(String path) {
        return ResourceKey.create(Registries.NOISE, Stormlight.id(path));
    }

    /*
     * The functions shared by the three wrappers in one RandomState, so
     * each column's weight and surface are worked out once.
     */
    private static final class Wiring {
        final DensityFunction weight;
        final DensityFunction surface;
        /* Whether this router had any overworld terrain function to wrap. */
        boolean used;

        Wiring(DensityFunction weight, DensityFunction surface) {
            this.weight = weight;
            this.surface = surface;
        }
    }

    /* Called around RandomState's router wiring. */
    public static void beginWiring(RandomState randomState, NoiseRouter unwiredRouter) {
        DensityFunction weight = DensityFunctions.flatCache(new Weight(
                randomState.getOrCreateNoise(ShatteredPlains.MASK_NOISE),
                unwiredRouter.continents(),
                unwiredRouter.temperature(),
                unwiredRouter.vegetation()
        ));
        DensityFunction surface = DensityFunctions.flatCache(new Surface(
                randomState.getOrCreateNoise(HEIGHT_NOISE),
                randomState.getOrCreateNoise(WARP_X_NOISE),
                randomState.getOrCreateNoise(WARP_Z_NOISE),
                randomState.getOrCreateNoise(CHASM_WIDTH_NOISE),
                randomState.getOrCreateRandomFactory(Stormlight.id("shattered_plains_plateaus"))
                        .at(0, 0, 0).nextLong()
        ));
        WIRING.set(new Wiring(weight, surface));
    }

    /*
     * Ends wiring, returning the wired terrain weight if this router got the
     * Shattered Plains terrain, or null if it didn't (not an overworld) or
     * no visitor is given (wiring failed).
     */
    public static @Nullable DensityFunction endWiring(DensityFunction.@Nullable Visitor wiringVisitor) {
        Wiring wiring = WIRING.get();
        WIRING.remove();
        if (wiring == null || !wiring.used || wiringVisitor == null) {
            return null;
        }
        // The wiring visitor caches what it maps, so this is the router's own instance.
        return wiring.weight.mapAll(wiringVisitor);
    }

    private static final Aquifer.FluidStatus NO_FLUID =
            new Aquifer.FluidStatus(DimensionType.MIN_Y * 2, Blocks.AIR.defaultBlockState());

    /*
     * Terrain below sea level normally floods with sea water, which is how
     * oceans form. Wherever the plains terrain applies, sea-level water is
     * dropped so the chasms stay dry. Lava and underground aquifers are
     * unaffected. One picker serves one NoiseChunk on one thread, so the
     * per-column cache needs no locking.
     */
    public static Aquifer.FluidPicker dryChasms(Aquifer.FluidPicker picker, DensityFunction weight) {
        Long2DoubleOpenHashMap weights = new Long2DoubleOpenHashMap();
        return (x, y, z) -> {
            Aquifer.FluidStatus status = picker.computeFluid(x, y, z);
            if (!status.fluidType().is(Blocks.WATER)) {
                return status;
            }
            int quartX = QuartPos.fromBlock(x);
            int quartZ = QuartPos.fromBlock(z);
            long column = ((long) quartX << 32) | (quartZ & 0xFFFFFFFFL);
            double w = weights.computeIfAbsent(column, key -> weight.compute(
                    new DensityFunction.SinglePointContext(QuartPos.toBlock(quartX), 0, QuartPos.toBlock(quartZ))));
            return w > 0.0 ? NO_FLUID : status;
        };
    }

    /*
     * The replacement for a named density function holder, or null to
     * leave it alone. The returned function still has its children unmapped.
     */
    public static @Nullable DensityFunction wrap(Holder<DensityFunction> holder) {
        Wiring wiring = WIRING.get();
        if (wiring == null) {
            return null;
        }

        Identifier id = holder.unwrapKey().map(ResourceKey::identifier).orElse(null);
        if (id == null || !id.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) {
            return null;
        }

        String path = id.getPath();
        int slash = path.indexOf('/');
        if (slash < 0 || !OVERWORLD_VARIANTS.contains(path.substring(0, slash))) {
            return null;
        }

        DensityFunction vanilla = holder.value();
        DensityFunction plains = switch (path.substring(slash + 1)) {
            case "offset" -> wiring.surface;
            case "factor" -> DensityFunctions.constant(PLAINS_FACTOR);
            case "jaggedness" -> DensityFunctions.zero();
            default -> null;
        };
        if (plains == null) {
            return null;
        }
        wiring.used = true;
        return new Blend(wiring.weight, vanilla, plains);
    }

    static double smoothstep(double edge0, double edge1, double x) {
        double t = Mth.clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    /* 0 outside the Shattered Plains, rising to 1 a little way inside its border. */
    public static double weight(double mask, double continentalness, double temperature, double humidity) {
        double score = ShatteredPlains.score(mask, temperature, humidity);
        return smoothstep(ShatteredPlains.THRESHOLD, ShatteredPlains.THRESHOLD + MASK_BLEND, score)
                * smoothstep(ShatteredPlains.MIN_CONTINENTALNESS, ShatteredPlains.MIN_CONTINENTALNESS
                + CONTINENTALNESS_BLEND, continentalness);
    }

    private static final MapCodec<DensityFunction> UNSERIALIZABLE = MapCodec.unit(() -> {
        throw new UnsupportedOperationException("Shattered Plains terrain functions are not data-driven");
    });

    private interface Unserializable extends DensityFunction {
        @Override
        default KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return KeyDispatchDataCodec.of(UNSERIALIZABLE);
        }

        @Override
        default void fillArray(double[] array, ContextProvider provider) {
            provider.fillAllDirectly(array, this);
        }
    }

    /* Mixes vanilla towards plains by the weight, skipping whichever side isn't needed. */
    record Blend(DensityFunction weight, DensityFunction vanilla, DensityFunction plains) implements Unserializable {
        @Override
        public double compute(FunctionContext context) {
            double w = weight.compute(context);
            if (w <= 0.0) {
                return vanilla.compute(context);
            }
            if (w >= 1.0) {
                return plains.compute(context);
            }
            return Mth.lerp(w, vanilla.compute(context), plains.compute(context));
        }

        @Override
        public DensityFunction mapChildren(Visitor visitor) {
            return new Blend(visitor.apply(weight), visitor.apply(vanilla), visitor.apply(plains));
        }

        @Override
        public double minValue() {
            return Math.min(vanilla.minValue(), plains.minValue());
        }

        @Override
        public double maxValue() {
            return Math.max(vanilla.maxValue(), plains.maxValue());
        }
    }

    /* The terrain blend weight. Reads the same climate functions the biome source does. */
    record Weight(
            NormalNoise mask,
            DensityFunction continents,
            DensityFunction temperature,
            DensityFunction humidity
    ) implements Unserializable {
        /* The most the climate nudge can add, so most columns skip the climate lookups. */
        private static final double MAX_CLIMATE_BIAS = 0.2;

        @Override
        public double compute(FunctionContext context) {
            double maskValue = ShatteredPlains.maskAt(mask, context.blockX(), context.blockZ());
            if (maskValue + MAX_CLIMATE_BIAS <= ShatteredPlains.THRESHOLD) {
                return 0.0;
            }
            return weight(maskValue, continents.compute(context),
                    temperature.compute(context), humidity.compute(context));
        }

        @Override
        public DensityFunction mapChildren(Visitor visitor) {
            return new Weight(mask, visitor.apply(continents), visitor.apply(temperature), visitor.apply(humidity));
        }

        @Override
        public double minValue() {
            return 0.0;
        }

        @Override
        public double maxValue() {
            return 1.0;
        }
    }

    /* Plains surface height as a vanilla offset value. */
    public record Surface(
            NormalNoise height,
            NormalNoise warpX,
            NormalNoise warpZ,
            NormalNoise chasmWidth,
            long seed
    ) implements Unserializable {
        private static final double MIN_Y = CHASM_FLOOR_Y - CHASM_FLOOR_ROLL;
        private static final double MAX_Y = PLATEAU_Y + PLATEAU_ROLL + PLATEAU_STEP / 2;

        @Override
        public double compute(FunctionContext context) {
            return (surfaceY(context.blockX(), context.blockZ()) - OFFSET_ZERO_Y) / OFFSET_BLOCKS_PER_UNIT;
        }

        public double surfaceY(int blockX, int blockZ) {
            double roll = height.getValue(blockX, 0, blockZ);
            double x = blockX + WARP_BLOCKS * warpX.getValue(blockX, 0, blockZ);
            double z = blockZ + WARP_BLOCKS * warpZ.getValue(blockX, 0, blockZ);

            // Worley noise: nearest and second-nearest plateau centres.
            int cellX = Mth.floor(x / PLATEAU_CELL);
            int cellZ = Mth.floor(z / PLATEAU_CELL);
            double nearest = Double.MAX_VALUE;
            double second = Double.MAX_VALUE;
            long nearestCell = 0;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int cx = cellX + dx;
                    int cz = cellZ + dz;
                    long hash = hash(cx, cz);
                    double px = (cx + 0.5 + CELL_JITTER * (unit(hash) - 0.5)) * PLATEAU_CELL;
                    double pz = (cz + 0.5 + CELL_JITTER * (unit(hash >>> 21) - 0.5)) * PLATEAU_CELL;
                    double distance = Math.sqrt((px - x) * (px - x) + (pz - z) * (pz - z));
                    if (distance < nearest) {
                        second = nearest;
                        nearest = distance;
                        nearestCell = hash;
                    } else if (distance < second) {
                        second = distance;
                    }
                }
            }

            // Half the F2-F1 gap is roughly the distance to the plateau's edge.
            double toEdge = (second - nearest) / 2.0;
            double halfWidth = Mth.lerp((chasmWidth.getValue(blockX, 0, blockZ) + 1.0) / 2.0,
                    CHASM_MIN_HALF_WIDTH, CHASM_MAX_HALF_WIDTH);
            double depth = 1.0 - smoothstep(halfWidth - WALL_RUN, halfWidth, toEdge);

            double plateau = PLATEAU_Y + PLATEAU_ROLL * roll + PLATEAU_STEP * (unit(nearestCell >>> 42) - 0.5);
            double floor = CHASM_FLOOR_Y + CHASM_FLOOR_ROLL * roll;
            return Mth.lerp(depth, plateau, floor);
        }

        private long hash(int cellX, int cellZ) {
            long h = seed ^ (cellX * 0x9E3779B97F4A7C15L) ^ (cellZ * 0xC2B2AE3D27D4EB4FL);
            h = (h ^ (h >>> 33)) * 0xFF51AFD7ED558CCDL;
            h = (h ^ (h >>> 33)) * 0xC4CEB9FE1A85EC53L;
            return h ^ (h >>> 33);
        }

        /* Low 21 bits as a value in [0, 1). */
        private static double unit(long bits) {
            return (bits & 0x1FFFFF) / (double) 0x200000;
        }

        @Override
        public DensityFunction mapChildren(Visitor visitor) {
            return this;
        }

        @Override
        public double minValue() {
            return (MIN_Y - OFFSET_ZERO_Y) / OFFSET_BLOCKS_PER_UNIT;
        }

        @Override
        public double maxValue() {
            return (MAX_Y - OFFSET_ZERO_Y) / OFFSET_BLOCKS_PER_UNIT;
        }
    }
}
