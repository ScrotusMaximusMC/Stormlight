package com.scrotey.stormlight.item;

import com.scrotey.stormlight.component.ModComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.List;
import java.util.function.Consumer;

public class SphereItem extends Item {
    private static final long FULL_DECAY_TICKS =
            10L * 24_000L;
    private final int capacity;

    public SphereItem(Properties properties, int capacity, String sphereName) {
        super(properties);
        this.capacity = capacity;
        this.sphereName = sphereName;
    }

    @Override
    public Component getName(ItemStack stack) {
        String state = getCharge(stack) > 0 ? "infused" : "dun";

        return Component.translatable(
                "item.stormlight." + sphereName + "." + state
        );
    }

    public int getCapacity() {
        return capacity;
    }

    private final String sphereName;

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> textConsumer,
            TooltipFlag flag
    ) {
        int charge = getCharge(stack);

        textConsumer.accept(
                Component.translatable(
                        "tooltip.stormlight.charge",
                        charge,
                        capacity
                ).withStyle(ChatFormatting.AQUA)
        );
    }

    public int getCharge(ItemStack stack) {
        int storedCharge = stack.getOrDefault(
                ModComponents.STORMLIGHT_CHARGE,
                0
        );

        return Math.max(0, Math.min(storedCharge, capacity));
    }

    public void setCharge(
            ItemStack stack,
            int newCharge
    ) {
        int previousCharge =
                getCharge(stack);

        int clampedCharge =
                Math.max(
                        0,
                        Math.min(
                                newCharge,
                                capacity
                        )
                );

        stack.set(
                ModComponents.STORMLIGHT_CHARGE,
                clampedCharge
        );

        /*
         * Receiving new Stormlight restarts the decay timer.
         *
         * Highstorm charging happens gradually, so its final charging
         * pulse becomes the start of the new ten-day decay period.
         */
        if (clampedCharge > previousCharge
                || clampedCharge <= 0) {

            stack.set(
                    ModComponents.STORMLIGHT_LAST_DECAY_TICK,
                    -1L
            );
        }

        int visualStage =
                getVisualStage(clampedCharge);

        stack.set(
                DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(
                        List.of(
                                (float) visualStage
                        ),
                        List.of(),
                        List.of(),
                        List.of()
                )
        );
    }

    /**
     * Applies time-based Stormlight leakage to this sphere.
     *
     * Every denomination loses its entire capacity over exactly ten
     * Minecraft days. Larger spheres therefore lose individual charge
     * points more frequently than smaller spheres.
     *
     * Returns true when the stack's data changed.
     */
    public boolean applyPassiveDecay(
            ItemStack stack,
            long currentGameTick
    ) {
        int currentCharge =
                getCharge(stack);

        if (currentCharge <= 0) {
            long lastDecayTick =
                    stack.getOrDefault(
                            ModComponents.STORMLIGHT_LAST_DECAY_TICK,
                            -1L
                    );

            if (lastDecayTick != -1L) {
                stack.set(
                        ModComponents.STORMLIGHT_LAST_DECAY_TICK,
                        -1L
                );

                return true;
            }

            return false;
        }

        long lastDecayTick =
                stack.getOrDefault(
                        ModComponents.STORMLIGHT_LAST_DECAY_TICK,
                        -1L
                );

        /*
         * Begin a fresh ten-day timer for newly infused spheres and
         * safely recover if the stored timestamp is invalid.
         */
        if (lastDecayTick < 0L
                || lastDecayTick > currentGameTick) {

            stack.set(
                    ModComponents.STORMLIGHT_LAST_DECAY_TICK,
                    currentGameTick
            );

            return true;
        }

        long ticksPerChargePoint =
                FULL_DECAY_TICKS
                        / capacity;

        long elapsedTicks =
                currentGameTick
                        - lastDecayTick;

        long decaySteps =
                elapsedTicks
                        / ticksPerChargePoint;

        if (decaySteps <= 0L) {
            return false;
        }

        int chargeLost =
                (int) Math.min(
                        currentCharge,
                        decaySteps
                );

        int remainingCharge =
                currentCharge
                        - chargeLost;

        /*
         * Preserve any partial progress toward the next lost point.
         */
        long updatedDecayTick =
                lastDecayTick
                        + chargeLost
                        * ticksPerChargePoint;

        setCharge(
                stack,
                remainingCharge
        );

        if (remainingCharge > 0) {
            stack.set(
                    ModComponents.STORMLIGHT_LAST_DECAY_TICK,
                    updatedDecayTick
            );
        }

        return true;
    }

    private int getVisualStage(int charge) {
        if (charge <= 0) {
            return 0;
        }

        double percentage = (double) charge / capacity;

        if (percentage <= 0.25) {
            return 1; // Faint
        }

        if (percentage <= 0.50) {
            return 2; // Soft
        }

        if (percentage <= 0.75) {
            return 3; // Bright
        }

        return 4; // Brilliant
    }
}