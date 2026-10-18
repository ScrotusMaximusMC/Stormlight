package com.scrotey.stormlight.client.progression;

import com.scrotey.stormlight.network.OpenWayOfKingsPayload;
import com.scrotey.stormlight.network.TakeLecternBookPayload;
import com.scrotey.stormlight.progression.RadiantOrderRegistry;
import com.scrotey.stormlight.progression.RadiantProgression;
import com.scrotey.stormlight.progression.StonecuttingProgression;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** A read-only field guide. The server supplies the reader's progress on each opening. */
public final class WayOfKingsScreen extends Screen {
    private static final int INK = 0xFF35251A;
    private static final int MUTED = 0xFF765F46;
    private static final int BLUE = 0xFF24677A;
    private static final int GOLD = 0xFF9C6B21;
    private static final int LOCKED = 0xFF9A8B72;
    private final OpenWayOfKingsPayload progress;
    private int page, left, top, panelWidth, panelHeight;

    public WayOfKingsScreen(OpenWayOfKingsPayload progress) {
        this(progress, 0);
    }

    public WayOfKingsScreen(OpenWayOfKingsPayload progress, int initialPage) {
        super(Component.translatable("screen.stormlight.way_of_kings.title"));
        this.progress = progress;
        this.page = Math.clamp(initialPage, 0, 4);
    }

    @Override protected void init() {
        panelWidth = Math.min(610, width - 24);
        panelHeight = Math.min(350, height - 20);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        int tabWidth = Math.min(100, (panelWidth - 44) / 5);
        int gap = 5;
        int start = width / 2 - (tabWidth * 5 + gap * 4) / 2;
        for (int i = 0; i < 5; i++) {
            final int target = i;
            addRenderableWidget(Button.builder(Component.translatable(
                    "screen.stormlight.way_of_kings.tab." + i), b -> page = target)
                    .bounds(start + i * (tabWidth + gap), top + 38, tabWidth, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.stormlight.way_of_kings.take"),
                b -> { ClientPlayNetworking.send(new TakeLecternBookPayload(progress.lecternPos())); onClose(); })
                .bounds(width / 2 - 96, top + panelHeight - 27, 90, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(width / 2 + 6, top + panelHeight - 27, 90, 20).build());
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xD80A0806);
        graphics.fill(left - 4, top - 4, left + panelWidth + 4, top + panelHeight + 4, 0xFF3A2418);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xFF8A673D);
        graphics.fill(left + 5, top + 5, left + panelWidth - 5, top + panelHeight - 5, 0xFFEAD8A8);
        graphics.fill(width / 2 - 2, top + 66, width / 2 + 2, top + panelHeight - 34, 0x555E4027);
        centered(graphics, title, width / 2, top + 15, INK);
        switch (page) {
            case 0 -> overview(graphics);
            case 1 -> radiant(graphics);
            case 2 -> cuttingTree(graphics, false);
            case 4 -> cuttingTree(graphics, true);
            default -> stormlightGuide(graphics);
        }
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void overview(GuiGraphicsExtractor g) {
        int x = left + 18, right = width / 2 + 18, y = top + 73;
        heading(g, "radiants", x, y);
        line(g, "guide.radiants", x, y + 21, INK);
        line(g, "guide.radiants_2", x, y + 39, INK);
        heading(g, "spheres", right, y);
        line(g, "guide.spheres", right, y + 21, INK);
        line(g, "guide.spheres_2", right, y + 39, INK);
        heading(g, "storms", right, y + 75);
        line(g, "guide.storms", right, y + 96, INK);
    }

    private void radiant(GuiGraphicsExtractor g) {
        int x = left + 18, right = width / 2 + 18, y = top + 73;
        var order = RadiantOrderRegistry.byNetworkId(progress.orderNetworkId());
        heading(g, "your_order", x, y);
        if (order.isEmpty()) {
            line(g, "order.none", x, y + 24, INK);
            heading(g, "next", right, y);
            line(g, "order.first", right, y + 21, BLUE);
            line(g, "order.begin", right, y + 40, MUTED);
            g.text(font, Component.translatable("screen.stormlight.way_of_kings.order.cost",
                    RadiantProgression.experienceCost(1), progress.experienceLevel()),
                    right, y + 59, MUTED, false);
            return;
        }
        var selected = order.get();
        g.text(font, Component.translatable(selected.translationKey()), x, y + 21, BLUE, false);
        g.text(font, Component.translatable("screen.stormlight.way_of_kings.order.level", progress.radiantLevel()),
                x, y + 40, INK, false);
        selected.level(progress.radiantLevel()).ifPresent(current -> {
            g.text(font, Component.translatable(current.unlockTranslationKey()), x, y + 59, BLUE, false);
            g.text(font, Component.translatable("screen.stormlight.way_of_kings.order.capacity",
                    current.stormlightCapacity()), x, y + 78, INK, false);
        });
        heading(g, "next", right, y);
        if (progress.radiantLevel() >= RadiantProgression.MAX_LEVEL) {
            line(g, "order.master", right, y + 24, GOLD);
        } else {
            int next = progress.radiantLevel() + 1;
            selected.level(next).ifPresent(milestone -> {
                g.text(font, Component.translatable(milestone.unlockTranslationKey()), right, y + 21, BLUE, false);
                g.text(font, Component.translatable("screen.stormlight.way_of_kings.order.cost",
                        RadiantProgression.experienceCost(next), progress.experienceLevel()),
                        right, y + 40, MUTED, false);
                g.text(font, Component.translatable(milestone.descriptionTranslationKey()),
                        x, y + 97, INK, false);
            });
        }
    }

    private void stormlightGuide(GuiGraphicsExtractor g) {
        int x = left + 18, right = width / 2 + 18, y = top + 73;
        heading(g, "stormlight", x, y);
        line(g, "guide.charge", x, y + 21, INK);
        line(g, "guide.capacity", x, y + 39, INK);
        heading(g, "pouch", right, y);
        line(g, "guide.pouch", right, y + 21, INK);
        line(g, "guide.pouch_2", right, y + 39, INK);
        heading(g, "controls", x, y + 72);
        line(g, "guide.breathe", x, y + 92, INK);
        line(g, "guide.exhale", x, y + 109, INK);
        heading(g, "storage", right, y + 72);
        line(g, "guide.storage", right, y + 92, INK);
        line(g, "guide.lashing", right, y + 109, INK);
    }

    private void cuttingTree(GuiGraphicsExtractor g, boolean broams) {
        int level = StonecuttingProgression.levelForCuts(progress.completedCuts());
        int remaining = level == StonecuttingProgression.MAX_LEVEL ? 0
                : StonecuttingProgression.cutsForLevel(level + 1) - progress.completedCuts();
        Component progressText = level == StonecuttingProgression.MAX_LEVEL
                ? Component.translatable("screen.stormlight.way_of_kings.cutting.master_progress",
                        progress.completedCuts(), StonecuttingProgression.MAX_CUTS)
                : Component.translatable("screen.stormlight.way_of_kings.cutting.progress",
                        level, progress.completedCuts(), StonecuttingProgression.MAX_CUTS, remaining);
        centered(g, progressText, width / 2, top + 65, BLUE);
        centered(g, Component.translatable(broams ? "screen.stormlight.way_of_kings.cutting.broam_legend"
                : "screen.stormlight.way_of_kings.cutting.legend"),
                width / 2, top + 77, MUTED);
        int rowGap = Math.max(20, Math.min(38, (panelHeight - 136) / 4));
        int firstY = top + 95;
        int firstX = left + panelWidth / 4;
        int secondX = left + panelWidth * 3 / 4;
        int halfWidth = Math.min(63, panelWidth / 4 - 5);
        for (int rank = 1; rank <= StonecuttingProgression.MAX_LEVEL; rank++) {
            int row = (rank - 1) / 2;
            boolean right = (rank & 1) == 0;
            int x = right ? secondX : firstX;
            int y = firstY + row * rowGap;
            int colour = broams && rank < StonecuttingProgression.BROAM_UNLOCK_LEVEL ? LOCKED
                    : rank <= level ? BLUE : rank == level + 1 ? GOLD : LOCKED;
            g.fill(x - halfWidth, y - 9, x + halfWidth, y + 9, colour);
            g.fill(x - halfWidth + 2, y - 7, x + halfWidth - 2, y + 7, 0xFFE7D1A0);
            Component node = broams && rank < StonecuttingProgression.BROAM_UNLOCK_LEVEL
                    ? Component.translatable("screen.stormlight.way_of_kings.cutting.broam_locked", rank)
                    : Component.translatable("screen.stormlight.way_of_kings.cutting.node",
                            rank, StonecuttingProgression.cutsForLevel(rank),
                            broams ? StonecuttingProgression.singleBroamChance(rank) : StonecuttingProgression.chipChance(rank),
                            broams ? StonecuttingProgression.doubleBroamChance(rank) : StonecuttingProgression.singleMarkChance(rank),
                            broams ? StonecuttingProgression.tripleBroamChance(rank) : StonecuttingProgression.doubleMarkChance(rank));
            centered(g, node, x, y - 4, colour);
            if (right) g.fill(firstX + halfWidth, y, secondX - halfWidth, y + 2, colour);
            if (rank > 2) g.fill(x - 1, y - rowGap + 9, x + 1, y - 9, colour);
        }

    }

    private void heading(GuiGraphicsExtractor g, String key, int x, int y) {
        g.text(font, Component.translatable("screen.stormlight.way_of_kings." + key), x, y, BLUE, false);
    }
    private void line(GuiGraphicsExtractor g, String key, int x, int y, int colour) {
        g.text(font, Component.translatable("screen.stormlight.way_of_kings." + key), x, y, colour, false);
    }
    private void centered(GuiGraphicsExtractor g, Component component, int x, int y, int colour) {
        g.text(font, component, x - font.width(component) / 2, y, colour, false);
    }
    @Override public boolean isPauseScreen() { return false; }
}
