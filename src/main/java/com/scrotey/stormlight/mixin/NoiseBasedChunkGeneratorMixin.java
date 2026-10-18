package com.scrotey.stormlight.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.scrotey.stormlight.worldgen.biome.ShatteredPlainsSurface;

import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.SurfaceRules;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/*
 * Puts the Shattered Plains surface rule in front of vanilla's, for both
 * the surface pass and the carvers (which re-surface carved ground).
 */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin {
    /* The settings never change for a generator, so the combined rule is built once. */
    @Unique
    private volatile SurfaceRules.@Nullable RuleSource stormlight$surfaceRule;

    @WrapOperation(
            method = {"buildSurface(Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/levelgen/WorldGenerationContext;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/biome/BiomeManager;Lnet/minecraft/world/level/levelgen/blending/Blender;Ljava/util/Set;)V", "applyCarvers"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;surfaceRule()Lnet/minecraft/world/level/levelgen/SurfaceRules$RuleSource;"
            )
    )
    private SurfaceRules.RuleSource stormlight$addShatteredPlains(
            NoiseGeneratorSettings settings,
            Operation<SurfaceRules.RuleSource> original
    ) {
        SurfaceRules.RuleSource rule = stormlight$surfaceRule;
        if (rule == null) {
            rule = ShatteredPlainsSurface.withPlains(original.call(settings),
                    ((ChunkGenerator) (Object) this).getBiomeSource().possibleBiomes());
            stormlight$surfaceRule = rule;
        }
        return rule;
    }
}
