package com.scrotey.stormlight.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;

/** The bench supplies the chisel and cutting time for these single-gem recipes. */
public class GemcuttingRecipe extends SingleItemRecipe {
    public static final MapCodec<GemcuttingRecipe> CODEC = simpleMapCodec(GemcuttingRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, GemcuttingRecipe> STREAM_CODEC =
            simpleStreamCodec(GemcuttingRecipe::new);

    public GemcuttingRecipe(Recipe.CommonInfo info, Ingredient input, ItemStackTemplate result) {
        super(info, input, result);
    }

    @Override
    public RecipeSerializer<GemcuttingRecipe> getSerializer() { return ModRecipes.GEMCUTTING; }

    @Override
    public RecipeType<GemcuttingRecipe> getType() { return ModRecipes.GEMCUTTING_TYPE; }

    @Override
    public String group() { return ""; }

    @Override
    public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }
}
