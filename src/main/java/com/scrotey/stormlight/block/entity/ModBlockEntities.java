package com.scrotey.stormlight.block.entity;

import com.scrotey.stormlight.Stormlight;
import com.scrotey.stormlight.block.ModBlocks;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    public static final BlockEntityType<SphereJarBlockEntity>
            SPHERE_JAR_BLOCK_ENTITY =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(
                            Stormlight.MOD_ID,
                            "sphere_jar"
                    ),
                    FabricBlockEntityTypeBuilder.create(
                            SphereJarBlockEntity::new,
                            ModBlocks.SPHERE_JAR
                    ).build()
            );

    public static final BlockEntityType<SphereLanternBlockEntity>
            SPHERE_LANTERN_BLOCK_ENTITY =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(
                            Stormlight.MOD_ID,
                            "sphere_lantern"
                    ),
                    FabricBlockEntityTypeBuilder.create(
                            SphereLanternBlockEntity::new,
                            ModBlocks.SPHERE_LANTERN
                    ).build()
            );

    private ModBlockEntities() {
    }

    public static void initialize() {
    }
}