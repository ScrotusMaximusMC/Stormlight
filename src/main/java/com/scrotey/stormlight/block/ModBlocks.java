package com.scrotey.stormlight.block;

import com.scrotey.stormlight.Stormlight;
import com.scrotey.stormlight.block.custom.SphereJarBlock;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModBlocks {
    private static final Identifier SPHERE_JAR_ID =
            Identifier.fromNamespaceAndPath(
                    Stormlight.MOD_ID,
                    "sphere_jar"
            );

    private static final ResourceKey<Block> SPHERE_JAR_BLOCK_KEY =
            ResourceKey.create(
                    Registries.BLOCK,
                    SPHERE_JAR_ID
            );

    private static final ResourceKey<Item> SPHERE_JAR_ITEM_KEY =
            ResourceKey.create(
                    Registries.ITEM,
                    SPHERE_JAR_ID
            );

    public static final Block SPHERE_JAR =
            Registry.register(
                    BuiltInRegistries.BLOCK,
                    SPHERE_JAR_BLOCK_KEY,
                    new SphereJarBlock(
                            BlockBehaviour.Properties.of()
                                    .setId(SPHERE_JAR_BLOCK_KEY)
                                    .strength(0.3F)
                                    .sound(SoundType.GLASS)
                                    .noOcclusion()
                                    .lightLevel(state ->
                                            state.getValue(SphereJarBlock.LIGHT_LEVEL)
                                    )
                    )
            );

    public static final Item SPHERE_JAR_ITEM =
            Registry.register(
                    BuiltInRegistries.ITEM,
                    SPHERE_JAR_ITEM_KEY,
                    new BlockItem(
                            SPHERE_JAR,
                            new Item.Properties()
                                    .setId(SPHERE_JAR_ITEM_KEY)
                                    .useBlockDescriptionPrefix()
                    )
            );

    private ModBlocks() {
    }

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(
                CreativeModeTabs.FUNCTIONAL_BLOCKS
        ).register(entries ->
                entries.accept(SPHERE_JAR_ITEM)
        );
    }
}