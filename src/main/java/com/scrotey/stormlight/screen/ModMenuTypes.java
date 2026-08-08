package com.scrotey.stormlight.screen;

import com.scrotey.stormlight.Stormlight;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public final class ModMenuTypes {
    public static final MenuType<SphereJarMenu> SPHERE_JAR =
            register(
                    "sphere_jar",
                    SphereJarMenu::new
            );

    public static final MenuType<SpherePouchMenu>
            SPHERE_POUCH =
            register(
                    "sphere_pouch",
                    SpherePouchMenu::new
            );

    private ModMenuTypes() {
    }

    private static <T extends AbstractContainerMenu>
    MenuType<T> register(
            String name,
            MenuType.MenuSupplier<T> supplier
    ) {
        return Registry.register(
                BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath(
                        Stormlight.MOD_ID,
                        name
                ),
                new MenuType<>(
                        supplier,
                        FeatureFlagSet.of()
                )
        );
    }

    public static void initialize() {
    }
}