package com.scrotey.stormlight.client.mixin;

import com.scrotey.stormlight.client.highstorm.ClientHighstormState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class DaylightDarkeningMixin {
    /*
     * Feeds the custom Highstorm darkness curve into Minecraft's
     * own thunder rendering. This affects the complete weather
     * lightmap rather than only the cached skylight value.
     */
    @Inject(
            method = "getThunderLevel",
            at = @At("RETURN"),
            cancellable = true
    )
    private void stormlight$applyHighstormThunderDarkness(
            float partialTick,
            CallbackInfoReturnable<Float> callback
    ) {
        if (!((Object) this
                instanceof ClientLevel clientLevel)) {
            return;
        }

        if (!clientLevel.dimension()
                .equals(Level.OVERWORLD)) {
            return;
        }

        /*
         * Outside our custom storm phases, preserve ordinary
         * Minecraft weather exactly as it was.
         */
        if (!ClientHighstormState.isStormfrontVisible()) {
            return;
        }

        callback.setReturnValue(
                ClientHighstormState
                        .getStormDarkness()
        );
    }
}