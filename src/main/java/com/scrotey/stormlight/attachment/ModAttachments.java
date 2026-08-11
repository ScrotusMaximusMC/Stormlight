package com.scrotey.stormlight.attachment;

import com.scrotey.stormlight.Stormlight;
import com.scrotey.stormlight.item.SpherePouchItem;
import com.scrotey.stormlight.screen.SphereJarMenu;
import com.scrotey.stormlight.screen.SpherePouchInventoryAccess;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import com.mojang.serialization.Codec;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.gamerules.GameRules;

import java.util.List;

public final class ModAttachments {
    public static final int MAX_PERSONAL_STORMLIGHT = 1000;

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

    public static final AttachmentType<ItemContainerContents>
            PLAYER_SPHERE_STORAGE =
            AttachmentRegistry.<ItemContainerContents>create(
                    Stormlight.id("player_sphere_storage"),
                    builder -> builder
                            .initializer(
                                    () -> ItemContainerContents.EMPTY
                            )
                            .persistent(
                                    ItemContainerContents.CODEC
                            )
                            .copyOnDeath()
                            .syncWith(
                                    ItemContainerContents.STREAM_CODEC,
                                    AttachmentSyncPredicate.targetOnly()
                            )
            );

    public static final AttachmentType<Integer>
            PERSONAL_STORMLIGHT =
            AttachmentRegistry.<Integer>create(
                    Stormlight.id("personal_stormlight"),
                    builder -> builder
                            .initializer(() -> 0)
                            .persistent(
                                    Codec.intRange(
                                            0,
                                            MAX_PERSONAL_STORMLIGHT
                                    )
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

    public static NonNullList<ItemStack> getSphereItems(
            Player player
    ) {
        NonNullList<ItemStack> storedItems =
                NonNullList.withSize(
                        SpherePouchItem.SLOT_COUNT,
                        ItemStack.EMPTY
                );

        getSphereContents(player).copyInto(storedItems);

        return storedItems;
    }

    public static ItemContainerContents getSphereContents(
            Player player
    ) {
        return player.getAttachedOrElse(
                PLAYER_SPHERE_STORAGE,
                ItemContainerContents.EMPTY
        );
    }

    public static void setSphereItems(
            ServerPlayer player,
            List<ItemStack> items
    ) {
        NonNullList<ItemStack> storedItems =
                NonNullList.withSize(
                        SpherePouchItem.SLOT_COUNT,
                        ItemStack.EMPTY
                );

        int itemCount = Math.min(
                items.size(),
                SpherePouchItem.SLOT_COUNT
        );

        for (int slot = 0;
             slot < itemCount;
             slot++) {

            storedItems.set(
                    slot,
                    items.get(slot).copy()
            );
        }

        player.setAttached(
                PLAYER_SPHERE_STORAGE,
                ItemContainerContents.fromItems(
                        storedItems
                )
        );

        if (player.containerMenu
                instanceof SphereJarMenu jarMenu) {

            jarMenu.refreshSphereStorage();
        }

        if (player.inventoryMenu
                instanceof SpherePouchInventoryAccess inventoryMenu) {

            inventoryMenu.stormlight$refreshSphereStorage();
        }
    }

    public static int getPersonalStormlight(
            Player player
    ) {
        return Math.max(
                0,
                Math.min(
                        player.getAttachedOrElse(
                                PERSONAL_STORMLIGHT,
                                0
                        ),
                        MAX_PERSONAL_STORMLIGHT
                )
        );
    }

    public static void setPersonalStormlight(
            Player player,
            int amount
    ) {
        player.setAttached(
                PERSONAL_STORMLIGHT,
                Math.max(
                        0,
                        Math.min(
                                amount,
                                MAX_PERSONAL_STORMLIGHT
                        )
                )
        );
    }

    public static void initialize() {
        ServerLivingEntityEvents.AFTER_DEATH.register(
                (entity, damageSource) -> {
                    if (!(entity instanceof ServerPlayer player)) {
                        return;
                    }

                    setPersonalStormlight(player, 0);

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
