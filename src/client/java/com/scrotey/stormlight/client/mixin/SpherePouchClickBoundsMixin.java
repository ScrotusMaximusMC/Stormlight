package com.scrotey.stormlight.client.mixin;

import com.scrotey.stormlight.attachment.ModAttachments;
import com.scrotey.stormlight.screen.SpherePouchInventoryLayout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractRecipeBookScreen.class)
public abstract class SpherePouchClickBoundsMixin {

    @Inject(
            method = "hasClickedOutside",
            at = @At("HEAD"),
            cancellable = true
    )
    private void stormlight$treatPouchPanelAsInside(
            double mouseX,
            double mouseY,
            int inventoryLeft,
            int inventoryTop,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (!((Object) this instanceof InventoryScreen)) {
            return;
        }

        var player = Minecraft.getInstance().player;

        if (player == null
                || !ModAttachments.hasEquippedPouch(player)) {
            return;
        }

        int pouchLeft = inventoryLeft
                + SpherePouchInventoryLayout.PANEL_X;

        int pouchTop = inventoryTop
                + SpherePouchInventoryLayout.PANEL_Y;

        boolean mouseIsOverPouch =
                mouseX >= pouchLeft
                        && mouseX < pouchLeft
                        + SpherePouchInventoryLayout.PANEL_WIDTH
                        && mouseY >= pouchTop
                        && mouseY < pouchTop
                        + SpherePouchInventoryLayout.PANEL_HEIGHT;

        if (mouseIsOverPouch) {
            callbackInfo.setReturnValue(false);
        }
    }
}