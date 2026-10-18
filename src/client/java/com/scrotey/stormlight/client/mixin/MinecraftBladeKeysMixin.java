package com.scrotey.stormlight.client.mixin;

import com.scrotey.stormlight.client.spren.ClientShardblade;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftBladeKeysMixin {
    @Shadow public net.minecraft.client.Options options;

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void stormlight$boundBladeKeys(CallbackInfo ci) {
        if (!ClientShardblade.isSelected()) return;
        boolean drop = false;
        while (options.keyDrop.consumeClick()) drop = true;
        while (options.keySwapOffhand.consumeClick()) {
            // The bound blade cannot be moved to another inventory slot.
        }
        if (drop) ClientShardblade.dismiss();
    }
}
