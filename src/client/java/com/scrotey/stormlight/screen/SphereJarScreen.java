package com.scrotey.stormlight.screen;

import com.scrotey.stormlight.Stormlight;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class SphereJarScreen
        extends AbstractContainerScreen<SphereJarMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(
                    Stormlight.MOD_ID,
                    "textures/gui/sphere_jar_gui.png"
            );

    private Button chipButton;
    private Button markButton;
    private Button broamButton;

    public SphereJarScreen(
            SphereJarMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(
                menu,
                inventory,
                title,
                176,
                222
        );

        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = 8;
        inventoryLabelY = 127;
    }

    @Override
    protected void init() {
        super.init();

        chipButton = addRenderableWidget(
                Button.builder(
                        Component.literal("Chips"),
                        button -> selectTab(
                                SphereJarMenu.CHIP_TAB
                        )
                ).bounds(
                        leftPos + 10,
                        topPos + 18,
                        50,
                        20
                ).build()
        );

        markButton = addRenderableWidget(
                Button.builder(
                        Component.literal("Marks"),
                        button -> selectTab(
                                SphereJarMenu.MARK_TAB
                        )
                ).bounds(
                        leftPos + 63,
                        topPos + 18,
                        50,
                        20
                ).build()
        );

        broamButton = addRenderableWidget(
                Button.builder(
                        Component.literal("Broams"),
                        button -> selectTab(
                                SphereJarMenu.BROAM_TAB
                        )
                ).bounds(
                        leftPos + 116,
                        topPos + 18,
                        50,
                        20
                ).build()
        );

        updateTabButtons();
    }

    private void selectTab(int tab) {
        if (minecraft == null
                || minecraft.player == null
                || minecraft.gameMode == null) {
            return;
        }

        if (menu.clickMenuButton(
                minecraft.player,
                tab
        )) {
            minecraft.gameMode
                    .handleInventoryButtonClick(
                            menu.containerId,
                            tab
                    );

            updateTabButtons();
        }
    }

    private void updateTabButtons() {
        int selected = menu.getSelectedTab();

        chipButton.active =
                selected != SphereJarMenu.CHIP_TAB;

        markButton.active =
                selected != SphereJarMenu.MARK_TAB;

        broamButton.active =
                selected != SphereJarMenu.BROAM_TAB;
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        super.extractBackground(
                graphics,
                mouseX,
                mouseY,
                delta
        );

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                leftPos,
                topPos,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                176,
                222
        );
    }
}