package com.scrotey.stormlight.item;

import com.scrotey.stormlight.component.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomModelData;


import java.util.List;
import java.util.function.Consumer;

public class SphereItem extends Item {
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

    public void setCharge(ItemStack stack, int newCharge) {
        int clampedCharge = Math.max(0, Math.min(newCharge, capacity));

        stack.set(
                ModComponents.STORMLIGHT_CHARGE,
                clampedCharge
        );

        int visualStage = getVisualStage(clampedCharge);

        stack.set(
                DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(
                        List.of((float) visualStage),
                        List.of(),
                        List.of(),
                        List.of()
                )
        );
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