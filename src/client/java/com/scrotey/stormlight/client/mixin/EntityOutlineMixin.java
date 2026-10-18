package com.scrotey.stormlight.client.mixin;

import com.scrotey.stormlight.client.lashing.ClientExternalLashingState;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityOutlineMixin {
    @Inject(
            method = "isCurrentlyGlowing",
            at = @At("HEAD"),
            cancellable = true
    )
    private void stormlight$showExternalLashingOutline(
            CallbackInfoReturnable<Boolean> callback
    ) {
        Entity entity = (Entity) (Object) this;

        if (ClientExternalLashingState.shouldOutline(entity)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(
            method = "getTeamColor",
            at = @At("HEAD"),
            cancellable = true
    )
    private void stormlight$colourExternalLashingOutline(
            CallbackInfoReturnable<Integer> callback
    ) {
        Entity entity = (Entity) (Object) this;

        if (ClientExternalLashingState.shouldOutline(entity)) {
            callback.setReturnValue(
                    ClientExternalLashingState.outlineColour(entity)
            );
        }
    }
}
