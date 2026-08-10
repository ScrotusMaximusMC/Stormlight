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

    public static final RecipeSerializer<SphereUpgradeRecipe> GARNET_CHIP_TO_MARK =
            register(
                    "garnet_chip_to_mark",
                    SphereUpgradeRecipe.GARNET_CHIP_TO_MARK_CODEC,
                    SphereUpgradeRecipe.GARNET_CHIP_TO_MARK_STREAM_CODEC
            );

    public static final RecipeSerializer<SphereUpgradeRecipe> GARNET_MARK_TO_BROAM =
            register(
                    "garnet_mark_to_broam",
                    SphereUpgradeRecipe.GARNET_MARK_TO_BROAM_CODEC,
                    SphereUpgradeRecipe.GARNET_MARK_TO_BROAM_STREAM_CODEC
            );
    public static final RecipeSerializer<SphereUpgradeRecipe>
            RUBY_CHIP_TO_MARK =
            register(
                    "ruby_chip_to_mark",
                    SphereUpgradeRecipe.RUBY_CHIP_TO_MARK_CODEC,
                    SphereUpgradeRecipe.RUBY_CHIP_TO_MARK_STREAM_CODEC
            );

    public static final RecipeSerializer<SphereUpgradeRecipe>
            RUBY_MARK_TO_BROAM =
            register(
                    "ruby_mark_to_broam",
                    SphereUpgradeRecipe.RUBY_MARK_TO_BROAM_CODEC,
                    SphereUpgradeRecipe.RUBY_MARK_TO_BROAM_STREAM_CODEC
            );

    public static final RecipeSerializer<SphereUpgradeRecipe>
            SAPPHIRE_CHIP_TO_MARK =
            register(
                    "sapphire_chip_to_mark",
                    SphereUpgradeRecipe.SAPPHIRE_CHIP_TO_MARK_CODEC,
                    SphereUpgradeRecipe.SAPPHIRE_CHIP_TO_MARK_STREAM_CODEC
            );

    public static final RecipeSerializer<SphereUpgradeRecipe>
            SAPPHIRE_MARK_TO_BROAM =
            register(
                    "sapphire_mark_to_broam",
                    SphereUpgradeRecipe.SAPPHIRE_MARK_TO_BROAM_CODEC,
                    SphereUpgradeRecipe.SAPPHIRE_MARK_TO_BROAM_STREAM_CODEC
            );

    public static final RecipeSerializer<SphereUpgradeRecipe>
            EMERALD_CHIP_CRAFTING =
            register(
                    "emerald_chip_crafting",
                    SphereUpgradeRecipe.EMERALD_CHIP_CRAFTING_CODEC,
                    SphereUpgradeRecipe.EMERALD_CHIP_CRAFTING_STREAM_CODEC
            );

    public static final RecipeSerializer<SphereUpgradeRecipe>
            EMERALD_MARK_CRAFTING =
            register(
                    "emerald_mark_crafting",
                    SphereUpgradeRecipe.EMERALD_MARK_CRAFTING_CODEC,
                    SphereUpgradeRecipe.EMERALD_MARK_CRAFTING_STREAM_CODEC
            );

    public static final RecipeSerializer<SphereUpgradeRecipe>
            EMERALD_BROAM_CRAFTING =
            register(
                    "emerald_broam_crafting",
                    SphereUpgradeRecipe.EMERALD_BROAM_CRAFTING_CODEC,
                    SphereUpgradeRecipe.EMERALD_BROAM_CRAFTING_STREAM_CODEC
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
