package com.scrotey.stormlight.worldgen.biome;

import com.scrotey.stormlight.Stormlight;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/*
 * The Shattered Plains are placed by a seeded, very low-frequency mask
 * noise instead of vanilla's climate parameters. Vanilla biomes all share
 * the same climate noise scale, so a biome placed through climate
 * parameters alone can't be much bigger than its neighbours.
 *
 * The mask noise is data-driven (worldgen/noise/shattered_plains_mask.json)
 * so the terrain shaping can read exactly the same values and line its
 * edges up with the biome's.
 */
public final class ShatteredPlains {

    public static final ResourceKey<Biome> BIOME =
            ResourceKey.create(Registries.BIOME, Stormlight.id("shattered_plains"));

    public static final ResourceKey<NormalNoise.NoiseParameters> MASK_NOISE =
            ResourceKey.create(Registries.NOISE, Stormlight.id("shattered_plains_mask"));

    /*
     * Mask value above which a column belongs to the Shattered Plains.
     * With the mask's -13 first octave this gives regions roughly
     * 3-5k blocks across covering about 5% of land.
     */
    public static final double THRESHOLD = 0.4;

    /* The border reaches further into warm, dry land and pulls back from cold, wet land. */
    private static final double TEMPERATURE_BIAS = 0.1;
    private static final double HUMIDITY_BIAS = 0.1;

    /*
     * Vanilla's "near inland" band starts at -0.11 and "mid inland" at 0.03.
     * Staying past it keeps coasts, beaches and oceans vanilla.
     */
    public static final double MIN_CONTINENTALNESS = 0.03;

    /*
     * Added to the overworld parameter list so the biome counts as one the
     * overworld can generate (features, structures, biome modifications), at
     * a climate point no real sample is ever nearest to.
     */
    public static final Climate.ParameterPoint UNREACHABLE =
            Climate.parameters(10F, 10F, 10F, 10F, 10F, 10F, 0F);

    private ShatteredPlains() {
    }

    public static double maskAt(NormalNoise mask, int blockX, int blockZ) {
        return mask.getValue(blockX, 0, blockZ);
    }

    /*
     * Mask value plus the climate nudge; compared against THRESHOLD. The
     * terrain uses this too, so its edges line up with the biome's.
     */
    public static double score(double mask, double temperature, double humidity) {
        return mask + TEMPERATURE_BIAS * temperature - HUMIDITY_BIAS * humidity;
    }

    /*
     * Picks the biome at a quart position, given the biome vanilla chose.
     * Cave biomes are kept so lush caves and the deep dark still generate
     * underneath.
     */
    public static Holder<Biome> choose(
            Holder<Biome> vanilla,
            Holder<Biome> shatteredPlains,
            NormalNoise mask,
            Climate.TargetPoint target,
            int quartX,
            int quartZ
    ) {
        if (Climate.unquantizeCoord(target.continentalness()) < MIN_CONTINENTALNESS
                || vanilla.is(Biomes.LUSH_CAVES)
                || vanilla.is(Biomes.DRIPSTONE_CAVES)
                || vanilla.is(Biomes.DEEP_DARK)) {
            return vanilla;
        }

        double score = score(maskAt(mask, QuartPos.toBlock(quartX), QuartPos.toBlock(quartZ)),
                Climate.unquantizeCoord(target.temperature()), Climate.unquantizeCoord(target.humidity()));
        return score > THRESHOLD ? shatteredPlains : vanilla;
    }
}
