package com.scrotey.stormlight.client.spren;

import com.scrotey.stormlight.Stormlight;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;

public final class ModSprenModelLayers {
    public static final ModelLayerLocation HONORSPREN =
            new ModelLayerLocation(Stormlight.id("honorspren"), "main");

    private ModSprenModelLayers() {
    }

    public static void register() {
        ModelLayerRegistry.registerModelLayer(
                HONORSPREN,
                HonorSprenModel::getLayerDefinition
        );
    }
}
