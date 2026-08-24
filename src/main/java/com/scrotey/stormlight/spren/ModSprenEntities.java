package com.scrotey.stormlight.spren;

import com.scrotey.stormlight.Stormlight;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModSprenEntities {
    public static final EntityType<SprenEntity> SPREN = register(
            "spren",
            EntityType.Builder.<SprenEntity>of(
                            SprenEntity::new,
                            MobCategory.MISC
                    )
                    .sized(0.6F, 0.35F)
    );

    private ModSprenEntities() {
    }

    private static <T extends Entity> EntityType<T> register(
            String name,
            EntityType.Builder<T> builder
    ) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(
                Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(
                        Stormlight.MOD_ID,
                        name
                )
        );

        return Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                key,
                builder.build(key)
        );
    }

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(
                SPREN,
                SprenEntity.createAttributes()
        );
    }
}
