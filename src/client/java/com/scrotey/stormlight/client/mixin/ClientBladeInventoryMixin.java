package com.scrotey.stormlight.client.mixin;

import com.scrotey.stormlight.client.spren.ClientShardblade;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
public abstract class ClientBladeInventoryMixin {
    @Inject(method = "setSelectedSlot", at = @At("HEAD"))
    private void stormlight$numberKeyLeavesBlade(int slot,
            CallbackInfo ci) {
        Inventory inventory = (Inventory) (Object) this;
        if (Minecraft.getInstance().player == inventory.player) {
            ClientShardblade.dismiss();
        }
    }
}
