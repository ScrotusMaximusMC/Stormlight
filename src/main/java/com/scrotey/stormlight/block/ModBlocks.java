package com.scrotey.stormlight.block;

import com.scrotey.stormlight.Stormlight;
import com.scrotey.stormlight.block.custom.SphereJarBlock;
import com.scrotey.stormlight.block.custom.SphereLanternBlock;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModBlocks {

    /*
     * Gemstone ores.
     *
     * Stone variants copy diamond ore's hardness and resistance.
     * Deepslate variants copy deepslate diamond ore.
     *
     * All six drop 3–7 experience when mined without Silk Touch.
     */
    public static final Block GARNET_ORE =
            registerOre(
                    "garnet_ore",
                    Blocks.DIAMOND_ORE
            );

    public static final Block DEEPSLATE_GARNET_ORE =
            registerOre(
                    "deepslate_garnet_ore",
                    Blocks.DEEPSLATE_DIAMOND_ORE
            );

    public static final Block RUBY_ORE =
            registerOre(
                    "ruby_ore",
                    Blocks.DIAMOND_ORE
            );

    public static final Block DEEPSLATE_RUBY_ORE =
            registerOre(
                    "deepslate_ruby_ore",
                    Blocks.DEEPSLATE_DIAMOND_ORE
            );

    public static final Block SAPPHIRE_ORE =
            registerOre(
                    "sapphire_ore",
                    Blocks.DIAMOND_ORE
            );

    public static final Block DEEPSLATE_SAPPHIRE_ORE =
            registerOre(
                    "deepslate_sapphire_ore",
                    Blocks.DEEPSLATE_DIAMOND_ORE
            );

    /*
     * Chrysalis Shell.
     *
     * The brown terracotta texture is temporary while we establish
     * the worldgen shape. It can later receive its own proper texture
     * without changing any generation code.
     */
    private static final Identifier CHRYSALIS_SHELL_ID =
            Identifier.fromNamespaceAndPath(
                    Stormlight.MOD_ID,
                    "chrysalis_shell"
            );

    private static final ResourceKey<Block>
            CHRYSALIS_SHELL_BLOCK_KEY =
            ResourceKey.create(
                    Registries.BLOCK,
                    CHRYSALIS_SHELL_ID
            );

    private static final ResourceKey<Item>
            CHRYSALIS_SHELL_ITEM_KEY =
            ResourceKey.create(
                    Registries.ITEM,
                    CHRYSALIS_SHELL_ID
            );

    public static final Block CHRYSALIS_SHELL =
            Registry.register(
                    BuiltInRegistries.BLOCK,
                    CHRYSALIS_SHELL_BLOCK_KEY,
                    new Block(
                            BlockBehaviour.Properties
                                    .ofFullCopy(
                                            Blocks.TERRACOTTA
                                    )
                                    .setId(
                                            CHRYSALIS_SHELL_BLOCK_KEY
                                    )
                                    .strength(
                                            4.0F,
                                            8.0F
                                    )
                                    .sound(
                                            SoundType.DRIPSTONE_BLOCK
                                    )
                    )
            );

    public static final Item CHRYSALIS_SHELL_ITEM =
            Registry.register(
                    BuiltInRegistries.ITEM,
                    CHRYSALIS_SHELL_ITEM_KEY,
                    new BlockItem(
                            CHRYSALIS_SHELL,
                            new Item.Properties()
                                    .setId(
                                            CHRYSALIS_SHELL_ITEM_KEY
                                    )
                                    .useBlockDescriptionPrefix()
                    )
            );

    private static final Identifier WEATHERED_CHRYSALIS_SHELL_ID =
            Identifier.fromNamespaceAndPath(
                    Stormlight.MOD_ID,
                    "weathered_chrysalis_shell"
            );

    private static final ResourceKey<Block>
            WEATHERED_CHRYSALIS_SHELL_BLOCK_KEY =
            ResourceKey.create(
                    Registries.BLOCK,
                    WEATHERED_CHRYSALIS_SHELL_ID
            );

    private static final ResourceKey<Item>
            WEATHERED_CHRYSALIS_SHELL_ITEM_KEY =
            ResourceKey.create(
                    Registries.ITEM,
                    WEATHERED_CHRYSALIS_SHELL_ID
            );

    public static final Block WEATHERED_CHRYSALIS_SHELL =
            Registry.register(
                    BuiltInRegistries.BLOCK,
                    WEATHERED_CHRYSALIS_SHELL_BLOCK_KEY,
                    new Block(
                            BlockBehaviour.Properties
                                    .ofFullCopy(
                                            Blocks.TERRACOTTA
                                    )
                                    .setId(
                                            WEATHERED_CHRYSALIS_SHELL_BLOCK_KEY
                                    )
                                    .strength(
                                            4.0F,
                                            8.0F
                                    )
                                    .sound(
                                            SoundType.DRIPSTONE_BLOCK
                                    )
                    )
            );

    public static final Item WEATHERED_CHRYSALIS_SHELL_ITEM =
            Registry.register(
                    BuiltInRegistries.ITEM,
                    WEATHERED_CHRYSALIS_SHELL_ITEM_KEY,
                    new BlockItem(
                            WEATHERED_CHRYSALIS_SHELL,
                            new Item.Properties()
                                    .setId(
                                            WEATHERED_CHRYSALIS_SHELL_ITEM_KEY
                                    )
                                    .useBlockDescriptionPrefix()
                    )
            );

    private static final Identifier PALE_CHRYSALIS_SHELL_ID =
            Identifier.fromNamespaceAndPath(
                    Stormlight.MOD_ID,
                    "pale_chrysalis_shell"
            );

    private static final ResourceKey<Block>
            PALE_CHRYSALIS_SHELL_BLOCK_KEY =
            ResourceKey.create(
                    Registries.BLOCK,
                    PALE_CHRYSALIS_SHELL_ID
            );

    private static final ResourceKey<Item>
            PALE_CHRYSALIS_SHELL_ITEM_KEY =
            ResourceKey.create(
                    Registries.ITEM,
                    PALE_CHRYSALIS_SHELL_ID
            );

    public static final Block PALE_CHRYSALIS_SHELL =
            Registry.register(
                    BuiltInRegistries.BLOCK,
                    PALE_CHRYSALIS_SHELL_BLOCK_KEY,
                    new Block(
                            BlockBehaviour.Properties
                                    .ofFullCopy(
                                            Blocks.TERRACOTTA
                                    )
                                    .setId(
                                            PALE_CHRYSALIS_SHELL_BLOCK_KEY
                                    )
                                    .strength(
                                            4.0F,
                                            8.0F
                                    )
                                    .sound(
                                            SoundType.DRIPSTONE_BLOCK
                                    )
                    )
            );

    public static final Item PALE_CHRYSALIS_SHELL_ITEM =
            Registry.register(
                    BuiltInRegistries.ITEM,
                    PALE_CHRYSALIS_SHELL_ITEM_KEY,
                    new BlockItem(
                            PALE_CHRYSALIS_SHELL,
                            new Item.Properties()
                                    .setId(
                                            PALE_CHRYSALIS_SHELL_ITEM_KEY
                                    )
                                    .useBlockDescriptionPrefix()
                    )
            );

    /*
     * Sphere Jar.
     */
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
                                            state.getValue(
                                                    SphereJarBlock.LIGHT_LEVEL
                                            )
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

    /*
     * Sphere Lantern.
     */
    private static final Identifier SPHERE_LANTERN_ID =
            Identifier.fromNamespaceAndPath(
                    Stormlight.MOD_ID,
                    "sphere_lantern"
            );

    private static final ResourceKey<Block>
            SPHERE_LANTERN_BLOCK_KEY =
            ResourceKey.create(
                    Registries.BLOCK,
                    SPHERE_LANTERN_ID
            );

    private static final ResourceKey<Item>
            SPHERE_LANTERN_ITEM_KEY =
            ResourceKey.create(
                    Registries.ITEM,
                    SPHERE_LANTERN_ID
            );

    public static final Block SPHERE_LANTERN =
            Registry.register(
                    BuiltInRegistries.BLOCK,
                    SPHERE_LANTERN_BLOCK_KEY,
                    new SphereLanternBlock(
                            BlockBehaviour.Properties.of()
                                    .setId(
                                            SPHERE_LANTERN_BLOCK_KEY
                                    )
                                    .strength(1.5F)
                                    .sound(SoundType.LANTERN)
                                    .noOcclusion()
                                    .lightLevel(state ->
                                            state.getValue(
                                                    SphereLanternBlock.LIGHT_LEVEL
                                            )
                                    )
                    )
            );

    public static final Item SPHERE_LANTERN_ITEM =
            Registry.register(
                    BuiltInRegistries.ITEM,
                    SPHERE_LANTERN_ITEM_KEY,
                    new BlockItem(
                            SPHERE_LANTERN,
                            new Item.Properties()
                                    .setId(
                                            SPHERE_LANTERN_ITEM_KEY
                                    )
                                    .useBlockDescriptionPrefix()
                    )
            );


    /*
     * Registers an ore block and its matching inventory item.
     */
    private static Block registerOre(
            String name,
            Block vanillaOre
    ) {
        Identifier id =
                Identifier.fromNamespaceAndPath(
                        Stormlight.MOD_ID,
                        name
                );

        ResourceKey<Block> blockKey =
                ResourceKey.create(
                        Registries.BLOCK,
                        id
                );

        ResourceKey<Item> itemKey =
                ResourceKey.create(
                        Registries.ITEM,
                        id
                );

        Block block =
                new DropExperienceBlock(
                        UniformInt.of(3, 7),
                        BlockBehaviour.Properties
                                .ofFullCopy(vanillaOre)
                                .setId(blockKey)
                );

        Registry.register(
                BuiltInRegistries.BLOCK,
                blockKey,
                block
        );

        Registry.register(
                BuiltInRegistries.ITEM,
                itemKey,
                new BlockItem(
                        block,
                        new Item.Properties()
                                .setId(itemKey)
                                .useBlockDescriptionPrefix()
                )
        );

        return block;
    }

    private ModBlocks() {
    }

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(
                CreativeModeTabs.FUNCTIONAL_BLOCKS
        ).register(entries -> {
            entries.accept(SPHERE_JAR_ITEM);
            entries.accept(SPHERE_LANTERN_ITEM);
        });

        CreativeModeTabEvents.modifyOutputEvent(
                CreativeModeTabs.NATURAL_BLOCKS
        ).register(entries -> {
            entries.accept(GARNET_ORE.asItem());
            entries.accept(DEEPSLATE_GARNET_ORE.asItem());

            entries.accept(RUBY_ORE.asItem());
            entries.accept(DEEPSLATE_RUBY_ORE.asItem());

            entries.accept(SAPPHIRE_ORE.asItem());
            entries.accept(DEEPSLATE_SAPPHIRE_ORE.asItem());

            entries.accept(CHRYSALIS_SHELL_ITEM);
            entries.accept(WEATHERED_CHRYSALIS_SHELL_ITEM);
            entries.accept(PALE_CHRYSALIS_SHELL_ITEM);
        });
    }
}