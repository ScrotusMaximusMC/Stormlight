package com.scrotey.stormlight.network;

import com.scrotey.stormlight.breathing.StormlightManager;
import com.scrotey.stormlight.attachment.ModAttachments;
import com.scrotey.stormlight.item.SpherePouchItem;
import com.scrotey.stormlight.lashing.LashingManager;
import com.scrotey.stormlight.progression.RadiantLecternInteraction;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ModNetworking {
    private ModNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(
                BreatheStormlightPayload.TYPE,
                BreatheStormlightPayload.CODEC
        );

        PayloadTypeRegistry.serverboundPlay().register(
                ExhaleStormlightPayload.TYPE,
                ExhaleStormlightPayload.CODEC
        );

        PayloadTypeRegistry.serverboundPlay().register(
                ToggleLashingPayload.TYPE,
                ToggleLashingPayload.CODEC
        );

        PayloadTypeRegistry.clientboundPlay().register(
                StormlightStatusPayload.TYPE,
                StormlightStatusPayload.CODEC
        );

        PayloadTypeRegistry.clientboundPlay().register(
                LashingStatePayload.TYPE,
                LashingStatePayload.CODEC
        );

        PayloadTypeRegistry.clientboundPlay().register(
                OpenRadiantProgressionPayload.TYPE,
                OpenRadiantProgressionPayload.CODEC
        );

        PayloadTypeRegistry.serverboundPlay().register(
                ChooseRadiantOrderPayload.TYPE,
                ChooseRadiantOrderPayload.CODEC
        );

        PayloadTypeRegistry.serverboundPlay().register(
                UnlockRadiantLevelPayload.TYPE,
                UnlockRadiantLevelPayload.CODEC
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
                BreatheStormlightPayload.TYPE,
                (payload, context) ->
                        StormlightManager.breathe(
                                context.player()
                        )
        );

        ServerPlayNetworking.registerGlobalReceiver(
                ExhaleStormlightPayload.TYPE,
                (payload, context) ->
                        StormlightManager.exhale(
                                context.player()
                        )
        );

        ServerPlayNetworking.registerGlobalReceiver(
                ToggleLashingPayload.TYPE,
                (payload, context) ->
                        LashingManager.handleInput(
                                context.player(),
                                payload.pressed()
                        )
        );

        ServerPlayNetworking.registerGlobalReceiver(
                ChooseRadiantOrderPayload.TYPE,
                (payload, context) ->
                        RadiantLecternInteraction.chooseOrder(
                                context.player(),
                                payload
                        )
        );

        ServerPlayNetworking.registerGlobalReceiver(
                UnlockRadiantLevelPayload.TYPE,
                (payload, context) ->
                        RadiantLecternInteraction.unlockLevel(
                                context.player(),
                                payload
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
