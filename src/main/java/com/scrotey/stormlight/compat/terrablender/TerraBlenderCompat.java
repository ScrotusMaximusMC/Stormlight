package com.scrotey.stormlight.compat.terrablender;

import com.scrotey.stormlight.Stormlight;
import com.scrotey.stormlight.worldgen.biome.ShatteredPlainsRegion;
import terrablender.api.Regions;
import terrablender.api.TerraBlenderApi;

/**
 * Optional TerraBlender entrypoint.
 *
 * TerraBlender itself invokes this through the custom "terrablender"
 * Fabric entrypoint after TerraBlender has initialized its config and
 * registries. If TerraBlender is not installed, this class is never loaded.
 */
public final class TerraBlenderCompat implements TerraBlenderApi {

    @Override
    public void onTerraBlenderInitialized() {
        // Rare like vanilla Badlands: low regional prevalence, large areas once selected.
        Regions.register(
                new ShatteredPlainsRegion(
                        Stormlight.id("shattered_plains_region"),
                        1
                )
        );

        Stormlight.LOGGER.info(
                "TerraBlender initialized; Shattered Plains integration enabled."
        );
    }
}
