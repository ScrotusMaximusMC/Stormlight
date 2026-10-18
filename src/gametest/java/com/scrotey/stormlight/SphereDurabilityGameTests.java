package com.scrotey.stormlight;

import com.scrotey.stormlight.item.ModItems;
import com.scrotey.stormlight.item.SphereItem;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

public class SphereDurabilityGameTests {
    @GameTest
    public void tenFullDischargesBreakSphere(GameTestHelper h) {
        for (var item : new net.minecraft.world.item.Item[]{ModItems.DIAMOND_CHIP, ModItems.EMERALD_BROAM}) {
            var stack = new ItemStack(item);
            var sphere = (SphereItem) item;
            int capacity = sphere.getCapacity();
            h.assertValueEqual(stack.getMaxDamage(), capacity * 10, "Lifetime is ten capacities");
            h.assertTrue(sphere.isBarVisible(stack), "Health bar is visible at full health");
            for (int cycle = 0; cycle < 10; cycle++) {
                sphere.setCharge(stack, capacity);
                h.assertValueEqual(sphere.getHealth(stack), (10 - cycle) * capacity, "Charging preserves wear");
                sphere.setCharge(stack, 1);
                h.assertTrue(!stack.isEmpty(), "Sphere survives until the last lifetime point");
                sphere.setCharge(stack, 0);
            }
            h.assertTrue(stack.isEmpty(), "Sphere disappears after ten capacities are used");
        }
        h.succeed();
    }

    @GameTest
    public void leakageDependsOnWearWithoutConsumingHealth(GameTestHelper h) {
        var sphere = (SphereItem) ModItems.DIAMOND_CHIP;
        for (int damage : new int[]{0, 20}) {
            var stack = new ItemStack(sphere);
            stack.setDamageValue(damage);
            sphere.setCharge(stack, 10);
            sphere.applyPassiveDecay(stack, 0);
            sphere.applyPassiveDecay(stack, 2_400_000L);
            h.assertValueEqual(sphere.getCharge(stack), 10, "No leakage at or above 80% health");
        }

        var worn = new ItemStack(sphere);
        worn.setDamageValue(60);
        sphere.setCharge(worn, 10);
        sphere.applyPassiveDecay(worn, 0);
        sphere.applyPassiveDecay(worn, 48_000L);
        h.assertValueEqual(sphere.getCharge(worn), 9, "40% health leaks at half the maximum rate");
        h.assertValueEqual(sphere.getHealth(worn), 40, "Leakage does not consume health");

        var fragile = new ItemStack(sphere);
        fragile.setDamageValue(99);
        sphere.setCharge(fragile, 10);
        sphere.applyPassiveDecay(fragile, 0);
        sphere.applyPassiveDecay(worn, 96_000L);
        sphere.applyPassiveDecay(fragile, 96_000L);
        h.assertTrue(sphere.getCharge(fragile) < sphere.getCharge(worn), "More wear increases leakage");
        sphere.applyPassiveDecay(fragile, 1_000_000L);
        h.assertTrue(!fragile.isEmpty(), "Passive leakage cannot break a sphere");
        h.assertValueEqual(sphere.getHealth(fragile), 1, "Last health point survives leakage");
        h.assertValueEqual(sphere.getCharge(fragile), 0, "Leakage eventually empties the sphere");
        h.succeed();
    }
}
