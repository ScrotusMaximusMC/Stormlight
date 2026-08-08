package com.scrotey.stormlight.breathing;

import com.scrotey.stormlight.item.SphereItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class StormlightPool {
    private StormlightPool() {
    }

    public record Totals(int charge, int capacity) {
    }

    public static Totals getTotals(ServerPlayer player) {
        int charge = 0;
        int capacity = 0;

        for (ItemStack stack
                : player.getInventory().getNonEquipmentItems()) {
            if (stack.getItem() instanceof SphereItem sphere) {
                charge += sphere.getCharge(stack);
                capacity += sphere.getCapacity();
            }
        }

        ItemStack offhand = player.getOffhandItem();

        if (offhand.getItem() instanceof SphereItem sphere) {
            charge += sphere.getCharge(offhand);
            capacity += sphere.getCapacity();
        }

        return new Totals(charge, capacity);
    }

    /**
     * Drains up to {@code amount} stormlight from the player's carried
     * spheres. Returns the amount actually drained, which may be less
     * than requested if the player's pool runs dry mid-drain.
     */
    public static int drain(ServerPlayer player, int amount) {
        int remaining = amount;

        for (ItemStack stack
                : player.getInventory().getNonEquipmentItems()) {
            remaining = drainFromSphere(stack, remaining);

            if (remaining == 0) {
                break;
            }
        }

        if (remaining > 0) {
            remaining = drainFromSphere(player.getOffhandItem(), remaining);
        }

        player.getInventory().setChanged();

        return amount - remaining;
    }

    private static int drainFromSphere(
            ItemStack stack,
            int requested
    ) {
        if (requested <= 0
                || !(stack.getItem() instanceof SphereItem sphere)) {
            return requested;
        }

        int available = sphere.getCharge(stack);
        int drained = Math.min(available, requested);

        if (drained > 0) {
            sphere.setCharge(stack, available - drained);
        }

        return requested - drained;
    }
}
