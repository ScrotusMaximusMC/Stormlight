package com.scrotey.stormlight.recipe;

import com.mojang.serialization.MapCodec;
import com.scrotey.stormlight.item.ModItems;
import com.scrotey.stormlight.item.SphereItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class SphereUpgradeRecipe extends CustomRecipe {
    public static final SphereUpgradeRecipe DIAMOND_CHIP_TO_MARK =
            new SphereUpgradeRecipe(ModItems.DIAMOND_CHIP, ModItems.DIAMOND_MARK);

    public static final SphereUpgradeRecipe DIAMOND_MARK_TO_BROAM =
            new SphereUpgradeRecipe(ModItems.DIAMOND_MARK, ModItems.DIAMOND_BROAM);

    public static final SphereUpgradeRecipe EMERALD_CHIP_TO_MARK =
            new SphereUpgradeRecipe(ModItems.EMERALD_CHIP, ModItems.EMERALD_MARK);

    public static final SphereUpgradeRecipe EMERALD_MARK_TO_BROAM =
            new SphereUpgradeRecipe(ModItems.EMERALD_MARK, ModItems.EMERALD_BROAM);

    public static final MapCodec<SphereUpgradeRecipe> DIAMOND_CHIP_TO_MARK_CODEC =
            MapCodec.unit(DIAMOND_CHIP_TO_MARK);

    public static final StreamCodec<RegistryFriendlyByteBuf, SphereUpgradeRecipe>
            DIAMOND_CHIP_TO_MARK_STREAM_CODEC = StreamCodec.unit(DIAMOND_CHIP_TO_MARK);

    public static final MapCodec<SphereUpgradeRecipe> DIAMOND_MARK_TO_BROAM_CODEC =
            MapCodec.unit(DIAMOND_MARK_TO_BROAM);

    public static final StreamCodec<RegistryFriendlyByteBuf, SphereUpgradeRecipe>
            DIAMOND_MARK_TO_BROAM_STREAM_CODEC = StreamCodec.unit(DIAMOND_MARK_TO_BROAM);

    public static final MapCodec<SphereUpgradeRecipe> EMERALD_CHIP_TO_MARK_CODEC =
            MapCodec.unit(EMERALD_CHIP_TO_MARK);

    public static final StreamCodec<RegistryFriendlyByteBuf, SphereUpgradeRecipe>
            EMERALD_CHIP_TO_MARK_STREAM_CODEC = StreamCodec.unit(EMERALD_CHIP_TO_MARK);

    public static final MapCodec<SphereUpgradeRecipe> EMERALD_MARK_TO_BROAM_CODEC =
            MapCodec.unit(EMERALD_MARK_TO_BROAM);

    public static final StreamCodec<RegistryFriendlyByteBuf, SphereUpgradeRecipe>
            EMERALD_MARK_TO_BROAM_STREAM_CODEC = StreamCodec.unit(EMERALD_MARK_TO_BROAM);

    private final Item inputItem;
    private final Item outputItem;

    private SphereUpgradeRecipe(Item inputItem, Item outputItem) {
        this.inputItem = inputItem;
        this.outputItem = outputItem;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return hasValidInputs(input);
    }

    private boolean hasValidInputs(CraftingInput input) {
        if (input.ingredientCount() != 9) {
            return false;
        }

        for (ItemStack stack : input.items()) {
            if (!stack.is(inputItem)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        if (!hasValidInputs(input)) {
            return ItemStack.EMPTY;
        }

        SphereItem inputSphere = (SphereItem) inputItem;
        SphereItem outputSphere = (SphereItem) outputItem;
        int totalCharge = 0;

        for (ItemStack stack : input.items()) {
            totalCharge += inputSphere.getCharge(stack);
        }

        ItemStack result = new ItemStack(outputItem);
        outputSphere.setCharge(result, totalCharge);
        return result;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        if (this == DIAMOND_CHIP_TO_MARK) {
            return ModRecipes.DIAMOND_CHIP_TO_MARK;
        }
        if (this == DIAMOND_MARK_TO_BROAM) {
            return ModRecipes.DIAMOND_MARK_TO_BROAM;
        }
        if (this == EMERALD_CHIP_TO_MARK) {
            return ModRecipes.EMERALD_CHIP_TO_MARK;
        }
        if (this == EMERALD_MARK_TO_BROAM) {
            return ModRecipes.EMERALD_MARK_TO_BROAM;
        }

        throw new IllegalStateException("Unregistered sphere upgrade recipe");
    }
}
