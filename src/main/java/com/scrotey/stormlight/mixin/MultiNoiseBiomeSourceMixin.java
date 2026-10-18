package com.scrotey.stormlight.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Either;
import com.scrotey.stormlight.worldgen.biome.ShatteredPlains;
import com.scrotey.stormlight.worldgen.biome.ShatteredPlainsBiomeSource;
import com.scrotey.stormlight.worldgen.biome.ShatteredPlainsParameters;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MultiNoiseBiomeSource.class)
public abstract class MultiNoiseBiomeSourceMixin implements ShatteredPlainsBiomeSource {
    @Shadow
    @Final
    private Either<Climate.ParameterList<Holder<Biome>>, Holder<MultiNoiseBiomeSourceParameterList>> parameters;

    /* Both null until attached, and always null outside the overworld. */
    @Unique
    private volatile @Nullable Holder<Biome> stormlight$shatteredPlains;

    @Unique
    private volatile @Nullable NormalNoise stormlight$mask;

    @Override
    public void stormlight$attachShatteredPlains(RandomState randomState) {
        Holder<Biome> shatteredPlains = parameters.right()
                .map(preset -> ((ShatteredPlainsParameters) preset.value()).stormlight$shatteredPlains())
                .orElse(null);
        if (shatteredPlains == null) {
            return;
        }

        stormlight$mask = randomState.getOrCreateNoise(ShatteredPlains.MASK_NOISE);
        stormlight$shatteredPlains = shatteredPlains;
    }

    @WrapOperation(
            method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/biome/MultiNoiseBiomeSource;getNoiseBiome(Lnet/minecraft/world/level/biome/Climate$TargetPoint;)Lnet/minecraft/core/Holder;"
            )
    )
    private Holder<Biome> stormlight$placeShatteredPlains(
            MultiNoiseBiomeSource source,
            Climate.TargetPoint target,
            Operation<Holder<Biome>> original,
            @Local(argsOnly = true, ordinal = 0) int quartX,
            @Local(argsOnly = true, ordinal = 2) int quartZ
    ) {
        Holder<Biome> vanilla = original.call(source, target);
        Holder<Biome> shatteredPlains = stormlight$shatteredPlains;
        NormalNoise mask = stormlight$mask;
        if (shatteredPlains == null || mask == null) {
            return vanilla;
        }

        return ShatteredPlains.choose(vanilla, shatteredPlains, mask, target, quartX, quartZ);
    }
}
