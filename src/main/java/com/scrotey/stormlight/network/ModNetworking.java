package com.scrotey.stormlight.network;

import com.scrotey.stormlight.breathing.StormlightManager;
import com.scrotey.stormlight.attachment.ModAttachments;
import com.scrotey.stormlight.item.SpherePouchItem;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ModNetworking {
    private ModNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(
                AbilityInputPayload.TYPE,
                AbilityInputPayload.CODEC
        );

        PayloadTypeRegistry.clientboundPlay().register(
                StormlightStatusPayload.TYPE,
                StormlightStatusPayload.CODEC
        );

        PayloadTypeRegistry.clientboundPlay().register(
                HighstormVisualPayload.TYPE,
                HighstormVisualPayload.CODEC
        );

        PayloadTypeRegistry.serverboundPlay().register(
                SpherePouchSlotPayload.TYPE,
                SpherePouchSlotPayload.CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(
                AbilityInputPayload.TYPE,
                (payload, context) ->
                        StormlightManager.handleInput(
                                context.player(),
                                payload.ability(),
                                payload.action()
                        )
        );

        ServerPlayNetworking.registerGlobalReceiver(
                SpherePouchSlotPayload.TYPE,
                (payload, context) -> {
                    var player = context.player();

                    ItemStack carried =
                            player.containerMenu
                                    .getCarried();

                    ItemStack equipped =
                            ModAttachments
                                    .getEquippedPouch(player);

                    if (carried.isEmpty()) {
                        if (equipped.isEmpty()) {
                            return;
                        }

                        player.containerMenu.setCarried(
                                ModAttachments
                                        .removeEquippedPouch(player)
                        );
                    } else if (carried.getItem()
                            instanceof SpherePouchItem) {

                        ModAttachments.setEquippedPouch(
                                player,
                                carried
                        );

                        player.containerMenu.setCarried(
                                equipped
                        );
                    } else {
                        player.sendOverlayMessage(
                                Component.translatable(
                                        "message.stormlight."
                                                + "sphere_pouch.invalid_item"
                                )
                        );

                        return;
                    }

                    player.containerMenu.broadcastChanges();
                }
        );
    }
}
