package com.scrotey.stormlight.client.mixin;

import com.scrotey.stormlight.attachment.ModAttachments;
import com.scrotey.stormlight.client.StormlightGuiStyle;
import com.scrotey.stormlight.screen.SpherePouchInventoryLayout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {

    @Inject(
            method = "extractBackground",
            at = @At("RETURN")
    )
    private void stormlight$drawBrightPouchPanel(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta,
            CallbackInfo callbackInfo
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null
                || !ModAttachments.hasEquippedPouch(minecraft.player)) {
            return;
        }

        AbstractContainerScreenAccessor accessor =
                (AbstractContainerScreenAccessor) this;

        int inventoryLeft = accessor.stormlight$getLeftPos();
        int inventoryTop = accessor.stormlight$getTopPos();

        int panelLeft =
                inventoryLeft + SpherePouchInventoryLayout.PANEL_X;

        int panelTop =
                inventoryTop + SpherePouchInventoryLayout.PANEL_Y;

        StormlightGuiStyle.drawSpherePanel(
                graphics,
                minecraft.font,
                Component.translatable(
                        "container.stormlight.sphere_pouch"
                ),
                panelLeft,
                panelTop,
                SpherePouchInventoryLayout.PANEL_WIDTH,
                SpherePouchInventoryLayout.PANEL_HEIGHT,
                inventoryLeft
                        + SpherePouchInventoryLayout.SLOT_START_X,
                inventoryTop
                        + SpherePouchInventoryLayout.SLOT_START_Y,
                4,
                4
        );
    }
}