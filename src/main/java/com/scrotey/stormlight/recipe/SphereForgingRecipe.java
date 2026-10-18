package com.scrotey.stormlight.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;

/** A gem in the forge's mould, sealed by pouring {@code glass} units of molten glass over it. */
public class SphereForgingRecipe extends SingleItemRecipe {
    public static final MapCodec<SphereForgingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(r -> r.commonInfo),
            Ingredient.CODEC.fieldOf("ingredient").forGetter(SingleItemRecipe::input),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(SphereForgingRecipe::result),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("glass", 1).forGetter(SphereForgingRecipe::glass)
    ).apply(i, SphereForgingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, SphereForgingRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, r -> r.commonInfo,
            Ingredient.CONTENTS_STREAM_CODEC, SingleItemRecipe::input,
            ItemStackTemplate.STREAM_CODEC, SphereForgingRecipe::result,
            ByteBufCodecs.VAR_INT, SphereForgingRecipe::glass,
            SphereForgingRecipe::new);

    private final int glass;

    public SphereForgingRecipe(Recipe.CommonInfo info, Ingredient input, ItemStackTemplate result, int glass) {
        super(info, input, result);
        this.glass = glass;
    }

    public int glass() { return glass; }

    @Override
    protected ItemStackTemplate result() { return super.result(); }

    @Override
    public RecipeSerializer<SphereForgingRecipe> getSerializer() { return ModRecipes.SPHERE_FORGING; }

    @Override
    public RecipeType<SphereForgingRecipe> getType() { return ModRecipes.SPHERE_FORGING_TYPE; }

    @Override
    public String group() { return ""; }

    @Override
    public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }
}
