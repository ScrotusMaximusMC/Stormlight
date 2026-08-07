package com.scrotey.stormlight.recipe;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class ModRecipes {
    private static final String MOD_ID = "stormlight";

    public static final RecipeSerializer<SphereUpgradeRecipe> DIAMOND_CHIP_TO_MARK =
            register(
                    "diamond_chip_to_mark",
                    SphereUpgradeRecipe.DIAMOND_CHIP_TO_MARK_CODEC,
                    SphereUpgradeRecipe.DIAMOND_CHIP_TO_MARK_STREAM_CODEC
            );

    public static final RecipeSerializer<SphereUpgradeRecipe> DIAMOND_MARK_TO_BROAM =
            register(
                    "diamond_mark_to_broam",
                    SphereUpgradeRecipe.DIAMOND_MARK_TO_BROAM_CODEC,
                    SphereUpgradeRecipe.DIAMOND_MARK_TO_BROAM_STREAM_CODEC
            );

    public static final RecipeSerializer<SphereUpgradeRecipe> EMERALD_CHIP_TO_MARK =
            register(
                    "emerald_chip_to_mark",
                    SphereUpgradeRecipe.EMERALD_CHIP_TO_MARK_CODEC,
                    SphereUpgradeRecipe.EMERALD_CHIP_TO_MARK_STREAM_CODEC
            );

    public static final RecipeSerializer<SphereUpgradeRecipe> EMERALD_MARK_TO_BROAM =
            register(
                    "emerald_mark_to_broam",
                    SphereUpgradeRecipe.EMERALD_MARK_TO_BROAM_CODEC,
                    SphereUpgradeRecipe.EMERALD_MARK_TO_BROAM_STREAM_CODEC
            );

    private static RecipeSerializer<SphereUpgradeRecipe> register(
            String name,
            com.mojang.serialization.MapCodec<SphereUpgradeRecipe> codec,
            net.minecraft.network.codec.StreamCodec<
                    net.minecraft.network.RegistryFriendlyByteBuf,
                    SphereUpgradeRecipe
                    > streamCodec
    ) {
        return Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                Identifier.fromNamespaceAndPath(MOD_ID, name),
                new RecipeSerializer<>(codec, streamCodec)
        );
    }

    public static void initialize() {
    }
}
