package com.scrotey.stormlight.attachment;

import com.scrotey.stormlight.Stormlight;
import com.scrotey.stormlight.item.SpherePouchItem;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;

public final class ModAttachments {
    public static final AttachmentType<ItemStack>
            EQUIPPED_SPHERE_POUCH =
            AttachmentRegistry.<ItemStack>create(
                    Stormlight.id("equipped_sphere_pouch"),
                    builder -> builder
                            .initializer(() -> ItemStack.EMPTY)
                            .persistent(ItemStack.OPTIONAL_CODEC)
                            .copyOnDeath()
                            .syncWith(
                                    ItemStack.OPTIONAL_STREAM_CODEC,
                                    AttachmentSyncPredicate.targetOnly()
                            )
            );

    private ModAttachments() {
    }

    public static ItemStack getEquippedPouch(
            Player player
    ) {
        ItemStack pouch = player.getAttachedOrElse(
                EQUIPPED_SPHERE_POUCH,
                ItemStack.EMPTY
        );

        return pouch.isEmpty()
                ? ItemStack.EMPTY
                : pouch.copy();
    }

    public static boolean hasEquippedPouch(
            Player player
    ) {
        return !getEquippedPouch(player).isEmpty();
    }

    public static void setEquippedPouch(
            Player player,
            ItemStack pouch
    ) {
        if (!pouch.isEmpty()
                && !(pouch.getItem()
                instanceof SpherePouchItem)) {

            throw new IllegalArgumentException(
                    "Only a Sphere Pouch can occupy "
                            + "the equipped pouch slot"
            );
        }

        ItemStack storedPouch;

        if (pouch.isEmpty()) {
            storedPouch = ItemStack.EMPTY;
        } else {
            storedPouch = pouch.copy();
            storedPouch.setCount(1);
        }

        player.setAttached(
                EQUIPPED_SPHERE_POUCH,
                storedPouch
        );
    }

    public static ItemStack removeEquippedPouch(
            Player player
    ) {
        ItemStack pouch = getEquippedPouch(player);

        setEquippedPouch(
                player,
                ItemStack.EMPTY
        );

        return pouch;
    }

    public static void initialize() {
        ServerLivingEntityEvents.AFTER_DEATH.register(
                (entity, damageSource) -> {
                    if (!(entity instanceof ServerPlayer player)) {
                        return;
                    }

                    boolean keepInventory =
                            player.level()
                                    .getGameRules()
                                    .get(
                                            GameRules.KEEP_INVENTORY
                                    );

                    if (keepInventory) {
                        return;
                    }

                    ItemStack pouch =
                            removeEquippedPouch(player);

                    if (pouch.isEmpty()) {
                        return;
                    }

                    ItemEntity droppedPouch =
                            new ItemEntity(
                                    player.level(),
                                    player.getX(),
                                    player.getY() + 0.5,
                                    player.getZ(),
                                    pouch
                            );

                    droppedPouch.setDefaultPickUpDelay();

                    player.level().addFreshEntity(
                            droppedPouch
                    );
                }
        );
    }
}