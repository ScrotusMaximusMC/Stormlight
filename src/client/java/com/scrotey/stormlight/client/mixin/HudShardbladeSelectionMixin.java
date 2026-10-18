package com.scrotey.stormlight.client.mixin;

import com.scrotey.stormlight.client.spren.ClientShardblade;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Hud.class)
public abstract class HudShardbladeSelectionMixin {
    @Redirect(method = "extractItemHotbar", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;getSelectedSlot()I"))
    private int stormlight$hideOldSelection(Inventory inventory) {
        return ClientShardblade.isSelected() ? -100
                : inventory.getSelectedSlot();
    }
}
