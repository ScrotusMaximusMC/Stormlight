package com.scrotey.stormlight.worldgen.biome;

import java.util.Optional;
import java.util.Set;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;

import org.jspecify.annotations.Nullable;

/*
 * Badlands-style ground for the Shattered Plains: plateau tops of packed
 * mud, coarse dirt and terracotta with patches of mud, chasm walls in
 * badlands terracotta stripes, and mud along the chasm floors. Nothing
 * here is grass, so nothing grows.
 */
public final class ShatteredPlainsSurface {

    /* Floors below this are chasm bottoms rather than plateau tops. */
    private static final int CHASM_FLOOR_BELOW_Y = 45;

    private ShatteredPlainsSurface() {
    }

    /*
     * Vanilla's surface rule with the Shattered Plains rule in front of it,
     * or the vanilla rule unchanged if the plains can't generate here.
     */
    public static SurfaceRules.RuleSource withPlains(
            SurfaceRules.RuleSource vanilla,
            Set<Holder<Biome>> possibleBiomes
    ) {
        Holder<Biome> plains = possibleBiomes.stream()
                .filter(biome -> biome.is(ShatteredPlains.BIOME))
                .findFirst()
                .orElse(null);
        if (!(plains instanceof Holder.Reference<Biome> reference)) {
            return vanilla;
        }
        return SurfaceRules.sequence(rule(reference), vanilla);
    }

    private static SurfaceRules.RuleSource rule(Holder.Reference<Biome> plains) {
        SurfaceRules.ConditionSource inChasm = SurfaceRules.not(
                SurfaceRules.yBlockCheck(VerticalAnchor.absolute(CHASM_FLOOR_BELOW_Y), 0));

        SurfaceRules.RuleSource plateauTop = SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.noiseCondition2d(Noises.SURFACE_SECONDARY, 0.45), state(Blocks.MUD)),
                SurfaceRules.ifTrue(SurfaceRules.noiseCondition2d(Noises.SURFACE, 0.3), state(Blocks.TERRACOTTA)),
                SurfaceRules.ifTrue(SurfaceRules.noiseCondition2d(Noises.SURFACE, -0.2), state(Blocks.PACKED_MUD)),
                SurfaceRules.ifTrue(SurfaceRules.noiseCondition2d(Noises.SURFACE, -0.5), state(Blocks.COARSE_DIRT)),
                state(Blocks.DYED_TERRACOTTA.brown())
        );

        return SurfaceRules.ifTrue(
                SurfaceRules.isBiome(getterFor(plains), ShatteredPlains.BIOME),
                SurfaceRules.ifTrue(SurfaceRules.abovePreliminarySurface(), SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                                SurfaceRules.ifTrue(inChasm, state(Blocks.MUD)),
                                plateauTop
                        )),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SurfaceRules.sequence(
                                SurfaceRules.ifTrue(inChasm, state(Blocks.PACKED_MUD)),
                                state(Blocks.TERRACOTTA)
                        )),
                        // Everything else near the surface, chasm walls included.
                        SurfaceRules.bandlands()
                ))
        );
    }

    private static SurfaceRules.RuleSource state(Block block) {
        return SurfaceRules.state(block.defaultBlockState());
    }

    /* isBiome needs a getter; this one only knows the plains biome. */
    private static HolderGetter<Biome> getterFor(Holder.Reference<Biome> plains) {
        return new HolderGetter<>() {
            @Override
            public Optional<Holder.Reference<Biome>> get(ResourceKey<Biome> key) {
                return plains.is(key) ? Optional.of(plains) : Optional.empty();
            }

            @Override
            public Optional<HolderSet.Named<Biome>> get(TagKey<Biome> tag) {
                return Optional.empty();
            }
        };
    }
}
