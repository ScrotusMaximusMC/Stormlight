package com.scrotey.stormlight.recipe;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class ModRecipes {
    private static final String MOD_ID =
            "stormlight";

    public static final RecipeSerializer<
            SphereUpgradeRecipe
            > DIAMOND_CHIP_TO_MARK =
            Registry.register(
                    BuiltInRegistries
                            .RECIPE_SERIALIZER,
                    Identifier.fromNamespaceAndPath(
                            MOD_ID,
                            "diamond_chip_to_mark"
                    ),
                    new RecipeSerializer<>(
                            SphereUpgradeRecipe
                                    .DIAMOND_CHIP_TO_MARK_CODEC,
                            SphereUpgradeRecipe
                                    .DIAMOND_CHIP_TO_MARK_STREAM_CODEC
                    )
            );

    public static final RecipeSerializer<
            SphereUpgradeRecipe
            > DIAMOND_MARK_TO_BROAM =
            Registry.register(
                    BuiltInRegistries
                            .RECIPE_SERIALIZER,
                    Identifier.fromNamespaceAndPath(
                            MOD_ID,
                            "diamond_mark_to_broam"
                    ),
                    new RecipeSerializer<>(
                            SphereUpgradeRecipe
                                    .DIAMOND_MARK_TO_BROAM_CODEC,
                            SphereUpgradeRecipe
                                    .DIAMOND_MARK_TO_BROAM_STREAM_CODEC
                    )
            );

    public static void initialize() {
    }
}