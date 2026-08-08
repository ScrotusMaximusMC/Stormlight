package com.scrotey.stormlight.breathing;

import com.scrotey.stormlight.attachment.ModAttachments;
import com.scrotey.stormlight.item.SphereItem;
import com.scrotey.stormlight.item.SpherePouchItem;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class StormlightPool {

    /**
     * Remembers which pouch slot should supply the next single point
     * of Stormlight for each player.
     *
     * This lets repeated one-point costs rotate evenly between all
     * charged spheres instead of repeatedly draining the first slot.
     */
    private static final Map<UUID, Integer>
            NEXT_DRAIN_SLOT =
            new HashMap<>();

    private StormlightPool() {
    }

    public record Totals(
            int charge,
            int capacity
    ) {
    }

    /**
     * Returns the total Stormlight stored in spheres inside the
     * player's currently equipped Sphere Pouch.
     *
     * Loose spheres in the inventory, hotbar, hands or offhand
     * deliberately do not count.
     */
    public static Totals getTotals(
            ServerPlayer player
    ) {
        ItemStack pouch =
                ModAttachments.getEquippedPouch(player);

        if (!(pouch.getItem()
                instanceof SpherePouchItem pouchItem)) {

            return new Totals(0, 0);
        }

        NonNullList<ItemStack> pouchItems =
                loadPouchItems(
                        pouch,
                        pouchItem
                );

        int charge = 0;
        int capacity = 0;

        for (ItemStack stack : pouchItems) {
            if (stack.getItem()
                    instanceof SphereItem sphere) {

                charge += sphere.getCharge(stack);
                capacity += sphere.getCapacity();
            }
        }

        return new Totals(
                charge,
                capacity
        );
    }

    /**
     * Drains Stormlight evenly from every charged sphere inside the
     * equipped Sphere Pouch.
     *
     * Each point is taken from the next charged pouch slot in a
     * round-robin sequence. This means every sphere drains at the
     * same absolute rate.
     *
     * Smaller denominations therefore become dun before larger
     * denominations, while all charged spheres still contribute.
     */
    public static int drain(
            ServerPlayer player,
            int amount
    ) {
        if (amount <= 0) {
            return 0;
        }

        ItemStack pouch =
                ModAttachments.getEquippedPouch(player);

        if (!(pouch.getItem()
                instanceof SpherePouchItem pouchItem)) {

            NEXT_DRAIN_SLOT.remove(
                    player.getUUID()
            );

            return 0;
        }

        NonNullList<ItemStack> pouchItems =
                loadPouchItems(
                        pouch,
                        pouchItem
                );

        int nextSlot =
                Math.floorMod(
                        NEXT_DRAIN_SLOT.getOrDefault(
                                player.getUUID(),
                                0
                        ),
                        pouchItems.size()
                );

        int remaining = amount;

        while (remaining > 0) {
            int sphereSlot =
                    findNextChargedSphere(
                            pouchItems,
                            nextSlot
                    );

            if (sphereSlot < 0) {
                break;
            }

            ItemStack sphereStack =
                    pouchItems.get(sphereSlot);

            SphereItem sphere =
                    (SphereItem)
                            sphereStack.getItem();

            int currentCharge =
                    sphere.getCharge(
                            sphereStack
                    );

            sphere.setCharge(
                    sphereStack,
                    currentCharge - 1
            );

            remaining--;

            nextSlot =
                    (sphereSlot + 1)
                            % pouchItems.size();
        }

        int drained =
                amount - remaining;

        if (drained > 0) {
            pouchItem.setContents(
                    pouch,
                    ItemContainerContents.fromItems(
                            pouchItems
                    )
            );

            ModAttachments.setEquippedPouch(
                    player,
                    pouch
            );

            NEXT_DRAIN_SLOT.put(
                    player.getUUID(),
                    nextSlot
            );
        }

        return drained;
    }

    /**
     * Finds the next charged sphere, starting at the supplied pouch
     * slot and wrapping around to the beginning.
     *
     * Returns -1 if every sphere is dun or the pouch is empty.
     */
    private static int findNextChargedSphere(
            NonNullList<ItemStack> pouchItems,
            int startingSlot
    ) {
        for (int offset = 0;
             offset < pouchItems.size();
             offset++) {

            int slot =
                    (startingSlot + offset)
                            % pouchItems.size();

            ItemStack stack =
                    pouchItems.get(slot);

            if (stack.getItem()
                    instanceof SphereItem sphere
                    && sphere.getCharge(stack) > 0) {

                return slot;
            }
        }

        return -1;
    }

    /**
     * Copies the pouch component into a mutable sixteen-slot list.
     */
    private static NonNullList<ItemStack> loadPouchItems(
            ItemStack pouch,
            SpherePouchItem pouchItem
    ) {
        NonNullList<ItemStack> pouchItems =
                NonNullList.withSize(
                        SpherePouchItem.SLOT_COUNT,
                        ItemStack.EMPTY
                );

        pouchItem.getContents(pouch)
                .copyInto(pouchItems);

        return pouchItems;
    }
}