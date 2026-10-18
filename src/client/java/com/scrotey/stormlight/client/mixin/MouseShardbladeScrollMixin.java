package com.scrotey.stormlight.client.mixin;

import com.scrotey.stormlight.client.spren.ClientShardblade;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Extend wheel selection to a temporary tenth position without changing inventory size. */
@Mixin(MouseHandler.class)
public abstract class MouseShardbladeScrollMixin {
    @Redirect(method = "onScroll", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;getSelectedSlot()I"))
    private int stormlight$currentSlot(Inventory inventory) {
        return ClientShardblade.isSelected() ? 9
                : inventory.getSelectedSlot();
    }

    @Redirect(method = "onScroll", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;getSelectionSize()I"))
    private int stormlight$selectionSize() {
        return ClientShardblade.isSummoned() ? 10 : 9;
    }

    @Redirect(method = "onScroll", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;setSelectedSlot(I)V"))
    private void stormlight$selectSlot(Inventory inventory, int slot) {
        if (ClientShardblade.isSelected()) {
            ClientShardblade.dismiss();
        }
        inventory.setSelectedSlot(slot == 9
                ? inventory.getSelectedSlot() : slot);
    }
}
