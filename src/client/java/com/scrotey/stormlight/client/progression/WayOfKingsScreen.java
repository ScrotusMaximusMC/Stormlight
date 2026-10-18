package com.scrotey.stormlight.client.progression;

import com.scrotey.stormlight.network.OpenWayOfKingsPayload;
import com.scrotey.stormlight.network.TakeLecternBookPayload;
import com.scrotey.stormlight.progression.RadiantOrderRegistry;
import com.scrotey.stormlight.progression.RadiantProgression;
import com.scrotey.stormlight.progression.StonecuttingProgression;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** A read-only field guide. The server supplies the reader's progress on each opening. */
public final class WayOfKingsScreen extends Screen {
    private static final int INK = 0xFF35251A;
    private static final int MUTED = 0xFF765F46;
    private static final int BLUE = 0xFF24677A;
    private static final int GOLD = 0xFF9C6B21;
    private static final int LOCKED = 0xFF9A8B72;
    private static final String[] TOPICS = {"stormlight", "crafting", "storms", "storage", "radiants", "spren"};
    private final List<PlainTextButton> contentsLinks = new ArrayList<>();
    private PlainTextButton contentsBack;
    private int articleRow;
    private final OpenWayOfKingsPayload progress;
    private int page, left, top, panelWidth, panelHeight, cuttingRow;
    private Button cuttingUp, cuttingDown;

    public WayOfKingsScreen(OpenWayOfKingsPayload progress) {
        this(progress, 0);
    }

    public WayOfKingsScreen(OpenWayOfKingsPayload progress, int initialPage) {
        super(Component.translatable("screen.stormlight.way_of_kings.title"));
        this.progress = progress;
        this.page = Math.clamp(initialPage, 0, 2 + TOPICS.length);
        this.cuttingRow = (StonecuttingProgression.levelForCuts(progress.completedCuts()) - 1) / 2;
    }

    @Override protected void init() {
        panelWidth = Math.min(610, width - 24);
        panelHeight = Math.min(350, height - 20);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        int tabWidth = Math.min(100, (panelWidth - 44) / 3);
        int gap = 5;
        int start = width / 2 - (tabWidth * 3 + gap * 2) / 2;
        for (int i = 0; i < 3; i++) {
            final int target = i;
            addRenderableWidget(Button.builder(Component.translatable(
                    "screen.stormlight.way_of_kings.tab." + i), b -> openPage(target))
                    .bounds(start + i * (tabWidth + gap), top + 38, tabWidth, 20).build());
        }
        contentsLinks.clear();
        for (int i = 0; i < TOPICS.length; i++) {
            final int target = 3 + i;
            Component label = topicText(TOPICS[i], "title");
            int x = i % 2 == 0 ? left + 18 : width / 2 + 18;
            contentsLinks.add(addRenderableWidget(new GuideLink(x, top + 91 + (i / 2) * 30,
                    font.width(label), 14, label, b -> openPage(target), font)));
        }
        Component back = Component.translatable("screen.stormlight.way_of_kings.contents.back");
        contentsBack = addRenderableWidget(new GuideLink(left + 12, top + 65,
                font.width(back), 14, back, b -> openPage(0), font));
        addRenderableWidget(Button.builder(Component.translatable("screen.stormlight.way_of_kings.take"),
                b -> { ClientPlayNetworking.send(new TakeLecternBookPayload(progress.lecternPos())); onClose(); })
                .bounds(width / 2 - 96, top + panelHeight - 27, 90, 20).build());
        cuttingUp = addRenderableWidget(Button.builder(Component.literal("▲"),
                b -> scrollPage(-1))
                .bounds(left + 12, top + panelHeight - 27, 20, 20).build());
        cuttingDown = addRenderableWidget(Button.builder(Component.literal("▼"),
                b -> scrollPage(1))
                .bounds(left + panelWidth - 32, top + panelHeight - 27, 20, 20).build());
        cuttingRow = Math.min(cuttingRow, maxCuttingRow());
        updateNavigation();
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
            case 2 -> cuttingTree(graphics);
            default -> article(graphics);
        }
        updateNavigation();
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    /** Book links use flat ink: the vanilla text button adds a heavy shadow. */
    private final class GuideLink extends PlainTextButton {
        private GuideLink(int x, int y, int width, int height, Component label, Button.OnPress onPress,
                          net.minecraft.client.gui.Font font) {
            super(x, y, width, height, label, onPress, font);
        }

        @Override public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            boolean highlighted = isHoveredOrFocused();
            graphics.text(WayOfKingsScreen.this.font,
                    getMessage().copy().withStyle(style -> style.withUnderlined(true)),
                    getX(), getY(), highlighted ? GOLD : INK, false);
        }
    }

    private void openPage(int target) {
        page = target;
        articleRow = 0;
        updateNavigation();
    }

    private void updateNavigation() {
        for (PlainTextButton link : contentsLinks) link.visible = page == 0;
        contentsBack.visible = page >= 3;
        if (cuttingUp != null && cuttingDown != null) {
            int max = page == 2 ? maxCuttingRow() : page >= 3 ? maxArticleRow() : 0;
            int row = page == 2 ? cuttingRow : articleRow;
            cuttingUp.visible = cuttingDown.visible = max > 0;
            cuttingUp.active = row > 0;
            cuttingDown.active = row < max;
        }
    }

    private void overview(GuiGraphicsExtractor g) {
        centered(g, Component.translatable("screen.stormlight.way_of_kings.contents.title"),
                width / 2, top + 70, BLUE);
        for (int i = 0; i < TOPICS.length; i++) {
            int x = i % 2 == 0 ? left + 26 : width / 2 + 26;
            g.text(font, topicText(TOPICS[i], "summary"), x, top + 107 + (i / 2) * 30, MUTED, false);
        }
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

    private net.minecraft.network.chat.MutableComponent topicText(String topic, String part) {
        return Component.translatable("screen.stormlight.way_of_kings.article." + topic + "." + part);
    }

    private List<FormattedCharSequence> articleLines(String side) {
        return font.split(topicText(TOPICS[page - 3], side + "_body"), panelWidth / 2 - 36);
    }

    private int visibleArticleRows() {
        return Math.max(1, (panelHeight - 137) / 11);
    }

    private int maxArticleRow() {
        return Math.max(0, Math.max(articleLines("left").size(), articleLines("right").size()) - visibleArticleRows());
    }

    private void article(GuiGraphicsExtractor g) {
        String topic = TOPICS[page - 3];
        centered(g, topicText(topic, "title"), width / 2, top + 72, BLUE);
        for (String side : List.of("left", "right")) {
            int x = side.equals("left") ? left + 18 : width / 2 + 18;
            g.text(font, topicText(topic, side + "_title"), x, top + 92, BLUE, false);
            List<FormattedCharSequence> lines = articleLines(side);
            for (int row = articleRow; row < Math.min(lines.size(), articleRow + visibleArticleRows()); row++) {
                g.text(font, lines.get(row), x, top + 106 + (row - articleRow) * 11, INK, false);
            }
        }
    }

    private void scrollPage(int direction) {
        if (page == 2) cuttingRow = Math.clamp(cuttingRow + direction, 0, maxCuttingRow());
        else if (page >= 3) articleRow = Math.clamp(articleRow + direction, 0, maxArticleRow());
        updateNavigation();
    }

    private void cuttingTree(GuiGraphicsExtractor g) {
        int level = StonecuttingProgression.levelForCuts(progress.completedCuts());
        int remaining = level == StonecuttingProgression.MAX_LEVEL ? 0
                : StonecuttingProgression.cutsForLevel(level + 1) - progress.completedCuts();
        Component progressText = level == StonecuttingProgression.MAX_LEVEL
                ? Component.translatable("screen.stormlight.way_of_kings.cutting.master_progress",
                        progress.completedCuts(), StonecuttingProgression.MAX_CUTS)
                : Component.translatable("screen.stormlight.way_of_kings.cutting.progress",
                        level, progress.completedCuts(), StonecuttingProgression.MAX_CUTS, remaining);
        centered(g, progressText, width / 2, top + 65, BLUE);
        centered(g, Component.translatable("screen.stormlight.way_of_kings.cutting.tools"),
                width / 2, top + 77, MUTED);
        centered(g, Component.translatable("screen.stormlight.way_of_kings.cutting.legend"),
                width / 2, top + 89, MUTED);
        int firstX = left + panelWidth / 4;
        int secondX = left + panelWidth * 3 / 4;
        int halfWidth = panelWidth / 4 - 10;
        for (int rank = 1; rank <= StonecuttingProgression.MAX_LEVEL; rank++) {
            int row = (rank - 1) / 2;
            if (row < cuttingRow || row >= cuttingRow + visibleCuttingRows()) continue;
            boolean right = (rank & 1) == 0;
            int x = right ? secondX : firstX;
            int y = top + 103 + (row - cuttingRow) * 38;
            int colour = rank <= level ? BLUE : rank == level + 1 ? GOLD : LOCKED;
            if (right) g.fill(firstX + halfWidth, y + 16, secondX - halfWidth, y + 18, colour);
            if (row > cuttingRow) g.fill(x - 1, y - 4, x + 1, y, colour);
            g.fill(x - halfWidth, y, x + halfWidth, y + 34, colour);
            g.fill(x - halfWidth + 2, y + 2, x + halfWidth - 2, y + 32, 0xFFE7D1A0);
            centered(g, Component.translatable("screen.stormlight.way_of_kings.cutting.rank",
                    rank, StonecuttingProgression.cutsForLevel(rank)), x, y + 3, colour);
            centered(g, Component.translatable("screen.stormlight.way_of_kings.cutting.gem_odds",
                    StonecuttingProgression.chipChance(rank), StonecuttingProgression.singleMarkChance(rank),
                    StonecuttingProgression.doubleMarkChance(rank)), x, y + 13, INK);
            Component broams = rank < StonecuttingProgression.BROAM_UNLOCK_LEVEL
                    ? Component.translatable("screen.stormlight.way_of_kings.cutting.broam_locked")
                    : Component.translatable("screen.stormlight.way_of_kings.cutting.broam_odds",
                            StonecuttingProgression.singleBroamChance(rank), StonecuttingProgression.doubleBroamChance(rank),
                            StonecuttingProgression.tripleBroamChance(rank));
            centered(g, broams, x, y + 23, rank < StonecuttingProgression.BROAM_UNLOCK_LEVEL ? MUTED : colour);
        }
    }

    private int visibleCuttingRows() {
        return Math.max(1, Math.min(5, (panelHeight - 137) / 38));
    }

    private int maxCuttingRow() {
        return 5 - visibleCuttingRows();
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if ((page == 2 || page >= 3) && mouseX >= left && mouseX < left + panelWidth
                && mouseY >= top + 65 && mouseY < top + panelHeight - 30 && scrollY != 0) {
            scrollPage(scrollY > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
