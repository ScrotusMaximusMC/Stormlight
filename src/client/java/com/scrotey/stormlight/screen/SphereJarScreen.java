package com.scrotey.stormlight.screen;

import com.scrotey.stormlight.Stormlight;
import com.scrotey.stormlight.client.StormlightGuiStyle;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class SphereJarScreen
        extends AbstractContainerScreen<SphereJarMenu> {

    private static final int JAR_TEXTURE_WIDTH = 176;
    private static final int SCREEN_WIDTH_WITH_POUCH = 288;

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(
                    Stormlight.MOD_ID,
                    "textures/gui/sphere_jar_gui.png"
            );

    public SphereJarScreen(
            SphereJarMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(
                menu,
                inventory,
                title,
                menu.isShowingPouch()
                        ? SCREEN_WIDTH_WITH_POUCH
                        : JAR_TEXTURE_WIDTH,
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

        addRenderableWidget(
                new DenominationTab(
                        leftPos + 10,
                        topPos + 18,
                        Component.literal("Chips"),
                        SphereJarMenu.CHIP_TAB,
                        1
                )
        );

        addRenderableWidget(
                new DenominationTab(
                        leftPos + 63,
                        topPos + 18,
                        Component.literal("Marks"),
                        SphereJarMenu.MARK_TAB,
                        2
                )
        );

        addRenderableWidget(
                new DenominationTab(
                        leftPos + 116,
                        topPos + 18,
                        Component.literal("Broams"),
                        SphereJarMenu.BROAM_TAB,
                        3
                )
        );
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
        }
    }

    @Override
    protected void extractLabels(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY
    ) {
        int stormlightWhite = 0xFFE9FBFF;

        graphics.text(
                font,
                title,
                titleLabelX,
                titleLabelY,
                stormlightWhite,
                true
        );

        graphics.text(
                font,
                playerInventoryTitle,
                inventoryLabelX,
                inventoryLabelY,
                stormlightWhite,
                true
        );
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
                JAR_TEXTURE_WIDTH,
                imageHeight,
                JAR_TEXTURE_WIDTH,
                222
        );

        if (menu.isShowingPouch()) {
            drawPouchPanel(graphics);
        }
    }

    private void drawPouchPanel(
            GuiGraphicsExtractor graphics
    ) {
        StormlightGuiStyle.drawSpherePanel(
                graphics,
                font,
                Component.translatable(
                        "container.stormlight.sphere_pouch"
                ),
                leftPos + 190,
                topPos + 27,
                90,
                89,
                leftPos + 204,
                topPos + 44,
                4,
                4
        );
    }

    /**
     * A small blue-glass Stormlight tab. The row of crystal sparks indicates
     * the denomination at a glance, while the selected tab looks infused.
     */
    private final class DenominationTab extends AbstractWidget {
        private static final int FRAME_DARK = 0xFF03101A;
        private static final int FRAME_BLUE = 0xFF287FA5;
        private static final int FACE = 0xFF0B2638;
        private static final int FACE_SELECTED = 0xFF17465D;
        private static final int EDGE = 0xFF68E8FF;
        private static final int TEXT = 0xFFC5EAF1;
        private static final int TEXT_SELECTED = 0xFFFFFFFF;

        private final int tab;
        private final int rank;

        private DenominationTab(
                int x,
                int y,
                Component label,
                int tab,
                int rank
        ) {
            super(x, y, 50, 20, label);
            this.tab = tab;
            this.rank = rank;
        }

        @Override
        protected void extractWidgetRenderState(
                GuiGraphicsExtractor graphics,
                int mouseX,
                int mouseY,
                float delta
        ) {
            int left = getX();
            int top = getY();
            int right = left + getWidth();
            int bottom = top + getHeight();
            boolean selected = menu.getSelectedTab() == tab;
            boolean highlighted = selected || isHovered();

            // A restrained aura appears on hover; the selected tab breathes.
            if (highlighted) {
                int aura = selected
                        && (System.currentTimeMillis() / 350L) % 2L == 0L
                        ? 0x7068E8FF
                        : 0x4053CFEF;
                graphics.fill(left - 1, top + 3, right + 1, bottom - 2, aura);
            }

            graphics.fill(left, top, right, bottom, FRAME_DARK);
            graphics.fill(left + 1, top + 1, right - 1, bottom - 1,
                    highlighted ? EDGE : FRAME_BLUE);
            graphics.fill(left + 2, top + 2, right - 2, bottom - 2,
                    selected ? FACE_SELECTED : FACE);

            // Tiny corner glyph marks break up the ordinary button silhouette.
            graphics.fill(left + 3, top + 3, left + 6, top + 4, FRAME_BLUE);
            graphics.fill(right - 6, top + 3, right - 3, top + 4, FRAME_BLUE);

            // One, two or three crystal sparks identify chip, mark and broam.
            int sparksWidth = rank * 4 - 1;
            int sparkX = left + (getWidth() - sparksWidth) / 2;
            for (int index = 0; index < rank; index++) {
                int x = sparkX + index * 4;
                graphics.fill(x + 1, top + 3, x + 2, top + 6, EDGE);
                graphics.fill(x, top + 4, x + 3, top + 5, EDGE);
            }

            if (selected) {
                graphics.fill(left + 4, bottom - 3, right - 4, bottom - 2, EDGE);
                graphics.fill(left + 10, bottom - 2, right - 10, bottom - 1,
                        0xFFDAFAFF);
            } else if (isHovered()) {
                graphics.fill(left + 8, bottom - 3, right - 8, bottom - 2,
                        0xFF49BDD8);
            }

            drawCentredLabel(
                    graphics,
                    font,
                    left,
                    top + 9,
                    selected ? TEXT_SELECTED : TEXT
            );
        }

        private void drawCentredLabel(
                GuiGraphicsExtractor graphics,
                Font font,
                int left,
                int top,
                int colour
        ) {
            int textX = left + (getWidth() - font.width(getMessage())) / 2;
            graphics.text(font, getMessage(), textX, top, colour, true);
        }

        @Override
        public void onClick(
                MouseButtonEvent event,
                boolean doubleClick
        ) {
            selectTab(tab);
        }

        @Override
        protected void updateWidgetNarration(
                NarrationElementOutput output
        ) {
            defaultButtonNarrationText(output);
        }
    }
}
