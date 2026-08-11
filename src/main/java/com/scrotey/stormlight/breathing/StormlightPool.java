package com.scrotey.stormlight.breathing;

import com.scrotey.stormlight.attachment.ModAttachments;
import com.scrotey.stormlight.item.SphereItem;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class StormlightPool {
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

    public static Totals getTotals(
            ServerPlayer player
    ) {
        /*
         * The spheres continue to exist while the pouch is removed,
         * but cannot be accessed or breathed from.
         */
        if (!ModAttachments
                .hasEquippedPouch(player)) {

            return new Totals(0, 0);
        }

        NonNullList<ItemStack> sphereItems =
                ModAttachments.getSphereItems(
                        player
                );

        int charge = 0;
        int capacity = 0;

        for (ItemStack stack : sphereItems) {
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

    public static int drain(
            ServerPlayer player,
            int amount
    ) {
        return drainSpheres(player, amount);
    }

    public static int drainSpheres(
            ServerPlayer player,
            int amount
    ) {
        if (amount <= 0) {
            return 0;
        }

        if (!ModAttachments
                .hasEquippedPouch(player)) {

            NEXT_DRAIN_SLOT.remove(
                    player.getUUID()
            );

            return 0;
        }

        NonNullList<ItemStack> sphereItems =
                ModAttachments.getSphereItems(
                        player
                );

        int nextSlot =
                Math.floorMod(
                        NEXT_DRAIN_SLOT.getOrDefault(
                                player.getUUID(),
                                0
                        ),
                        sphereItems.size()
                );

        int remaining = amount;

        while (remaining > 0) {
            int sphereSlot =
                    findNextChargedSphere(
                            sphereItems,
                            nextSlot
                    );

            if (sphereSlot < 0) {
                break;
            }

            ItemStack sphereStack =
                    sphereItems.get(
                            sphereSlot
                    );

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
                            % sphereItems.size();
        }

        int drained =
                amount - remaining;

        if (drained > 0) {
            ModAttachments.setSphereItems(
                    player,
                    sphereItems
            );

            NEXT_DRAIN_SLOT.put(
                    player.getUUID(),
                    nextSlot
            );
        }

        return drained;
    }

    public static int drainReserve(
            ServerPlayer player,
            int amount
    ) {
        if (amount <= 0) {
            return 0;
        }

        int reserve =
                ModAttachments.getPersonalStormlight(player);

        int drained = Math.min(reserve, amount);

        if (drained > 0) {
            ModAttachments.setPersonalStormlight(
                    player,
                    reserve - drained
            );
        }

        return drained;
    }

    private static int findNextChargedSphere(
            NonNullList<ItemStack> sphereItems,
            int startingSlot
    ) {
        for (int offset = 0;
             offset < sphereItems.size();
             offset++) {

            int slot =
                    (startingSlot + offset)
                            % sphereItems.size();

            ItemStack stack =
                    sphereItems.get(slot);

            if (stack.getItem()
                    instanceof SphereItem sphere
                    && sphere.getCharge(stack) > 0) {

                return slot;
            }
        }

        return -1;
    }
}
