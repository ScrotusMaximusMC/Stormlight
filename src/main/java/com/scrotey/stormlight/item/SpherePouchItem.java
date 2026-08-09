package com.scrotey.stormlight.item;

import com.scrotey.stormlight.attachment.ModAttachments;
import com.scrotey.stormlight.component.ModComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class SpherePouchItem extends Item {
    public static final int SLOT_COUNT = 16;

    public SpherePouchItem(Properties properties) {
        super(properties);
    }

    /*
     * These two methods exist only to migrate pouches created before
     * the player-owned storage update.
     */
    public ItemContainerContents getLegacyContents(
            ItemStack pouch
    ) {
        return pouch.getOrDefault(
                ModComponents.SPHERE_POUCH_CONTENTS,
                ItemContainerContents.EMPTY
        );
    }

    public void clearLegacyContents(
            ItemStack pouch
    ) {
        pouch.set(
                ModComponents.SPHERE_POUCH_CONTENTS,
                ItemContainerContents.EMPTY
        );
    }

    @Override
    public InteractionResult use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        if (level.isClientSide()) {
            return InteractionResult.PASS;
        }

        ItemStack heldPouch =
                player.getItemInHand(hand);

        ItemStack previouslyEquipped =
                ModAttachments.getEquippedPouch(player);

        ModAttachments.setEquippedPouch(
                player,
                heldPouch
        );

        player.setItemInHand(
                hand,
                previouslyEquipped
        );

        if (previouslyEquipped.isEmpty()) {
            player.sendOverlayMessage(
                    Component.translatable(
                            "message.stormlight."
                                    + "sphere_pouch.equipped"
                    )
            );
        } else {
            player.sendOverlayMessage(
                    Component.translatable(
                            "message.stormlight."
                                    + "sphere_pouch.swapped"
                    )
            );
        }

        return InteractionResult.SUCCESS_SERVER;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> textConsumer,
            TooltipFlag flag
    ) {
        textConsumer.accept(
                Component.translatable(
                        "tooltip.stormlight.sphere_pouch"
                ).withStyle(ChatFormatting.AQUA)
        );
    }
}