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

    private static final int POUCH_PANEL_COLOUR =
            0xFF172630;

    private static final int POUCH_PANEL_BORDER =
            0xFF5BD8F2;

    private static final int POUCH_SLOT_BORDER =
            0xFF08141C;

    private static final int POUCH_SLOT_BACKGROUND =
            0xFF365365;

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

        inventoryLabelY =
                menu.isShowingPouch()
                        ? -1000
                        : 127;
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

        if (menu.isShowingPouch()) {
            drawPouchPanel(graphics);
        }
    }

    private void drawPouchPanel(
            GuiGraphicsExtractor graphics
    ) {
        graphics.fill(
                leftPos + 7,
                topPos + 122,
                leftPos + 169,
                topPos + 215,
                POUCH_PANEL_BORDER
        );

        graphics.fill(
                leftPos + 9,
                topPos + 124,
                leftPos + 167,
                topPos + 213,
                POUCH_PANEL_COLOUR
        );

        graphics.text(
                font,
                Component.translatable(
                        "container.stormlight.sphere_pouch"
                ),
                leftPos + 13,
                topPos + 127,
                0xFFFFFFFF,
                false
        );

        drawPouchSlotGrid(
                graphics,
                53,
                139
        );
    }

    private void drawPouchSlotGrid(
            GuiGraphicsExtractor graphics,
            int startX,
            int startY
    ) {
        for (int row = 0; row < 4; row++) {
            for (int column = 0;
                 column < 4;
                 column++) {

                int x =
                        leftPos
                                + startX
                                + column * 18;

                int y =
                        topPos
                                + startY
                                + row * 18;

                graphics.fill(
                        x - 1,
                        y - 1,
                        x + 17,
                        y + 17,
                        POUCH_SLOT_BORDER
                );

                graphics.fill(
                        x,
                        y,
                        x + 16,
                        y + 16,
                        POUCH_SLOT_BACKGROUND
                );
            }
        }
    }
}
