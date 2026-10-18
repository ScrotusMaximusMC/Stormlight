package com.scrotey.stormlight.mixin;

import com.mojang.datafixers.util.Pair;
import com.scrotey.stormlight.worldgen.biome.ShatteredPlains;
import com.scrotey.stormlight.worldgen.biome.ShatteredPlainsParameters;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/*
 * Adds the Shattered Plains to the overworld parameter list at an
 * unreachable climate point, so it's one of the biome source's possible
 * biomes. Where it actually generates is decided by MultiNoiseBiomeSourceMixin.
 */
@Mixin(MultiNoiseBiomeSourceParameterList.class)
public abstract class MultiNoiseBiomeSourceParameterListMixin implements ShatteredPlainsParameters {
    @Shadow
    @Final
    @Mutable
    private Climate.ParameterList<Holder<Biome>> parameters;

    @Unique
    private @Nullable Holder<Biome> stormlight$shatteredPlains;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void stormlight$addShatteredPlains(
            MultiNoiseBiomeSourceParameterList.Preset preset,
            HolderGetter<Biome> biomes,
            CallbackInfo callback
    ) {
        if (preset != MultiNoiseBiomeSourceParameterList.Preset.OVERWORLD) {
            return;
        }

        Holder<Biome> shatteredPlains = biomes.getOrThrow(ShatteredPlains.BIOME);
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> values = new ArrayList<>(parameters.values());
        values.add(Pair.of(ShatteredPlains.UNREACHABLE, shatteredPlains));

        parameters = new Climate.ParameterList<>(values);
        stormlight$shatteredPlains = shatteredPlains;
    }

    @Override
    public @Nullable Holder<Biome> stormlight$shatteredPlains() {
        return stormlight$shatteredPlains;
    }
}
