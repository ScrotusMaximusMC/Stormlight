package com.scrotey.stormlight.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;

/** A gem set in {@code mould} on the forge's bench, sealed by pouring {@code glass} units of molten glass over it. */
public class SphereForgingRecipe extends SingleItemRecipe {
    public static final MapCodec<SphereForgingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(r -> r.commonInfo),
            Ingredient.CODEC.fieldOf("ingredient").forGetter(SingleItemRecipe::input),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(SphereForgingRecipe::result),
            Item.CODEC.fieldOf("mould").forGetter(SphereForgingRecipe::mould),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("glass", 1).forGetter(SphereForgingRecipe::glass)
    ).apply(i, SphereForgingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, SphereForgingRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, r -> r.commonInfo,
            Ingredient.CONTENTS_STREAM_CODEC, SingleItemRecipe::input,
            ItemStackTemplate.STREAM_CODEC, SphereForgingRecipe::result,
            Item.STREAM_CODEC, SphereForgingRecipe::mould,
            ByteBufCodecs.VAR_INT, SphereForgingRecipe::glass,
            SphereForgingRecipe::new);

    private final Holder<Item> mould;
    private final int glass;

    public SphereForgingRecipe(Recipe.CommonInfo info, Ingredient input, ItemStackTemplate result,
                               Holder<Item> mould, int glass) {
        super(info, input, result);
        this.mould = mould;
        this.glass = glass;
    }

    public Holder<Item> mould() { return mould; }
    public int glass() { return glass; }
    public boolean fitsMould(ItemStack stack) { return stack.is(mould); }

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
