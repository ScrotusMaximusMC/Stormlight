package com.scrotey.stormlight;

import com.scrotey.stormlight.client.progression.WayOfKingsScreen;
import com.scrotey.stormlight.network.OpenWayOfKingsPayload;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;

/** Guide navigation and screenshots, independent of sphere creation. */
public class WayOfKingsClientTest implements FabricClientGameTest {
    private static final BlockPos POS = new BlockPos(0, 100, 0);

    @Override public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            context.runOnClient(client -> client.gui.setScreen(new WayOfKingsScreen(
                    new OpenWayOfKingsPayload(POS, 1, 3, 8, 35))));
            context.waitTicks(3);
            context.takeScreenshot("way-of-kings-guide");
            for (String topic : new String[]{"stormlight", "crafting", "storms", "storage", "radiants", "spren"}) {
                pressGuideLink(context, "screen.stormlight.way_of_kings.article." + topic + ".title");
                context.waitTicks(2);
                context.runOnClient(client -> {
                    var screen = client.gui.screen();
                    long visibleLinks = screen.children().stream()
                            .filter(child -> child instanceof net.minecraft.client.gui.components.PlainTextButton button && button.visible)
                            .count();
                    if (visibleLinks != 1) throw new AssertionError("Articles show the Contents link, not the contents entries");
                });
                context.takeScreenshot("way-of-kings-article-" + topic);
                if (topic.equals("crafting")) {
                    context.runOnClient(client -> {
                        var down = client.gui.screen().children().stream()
                                .filter(child -> child instanceof net.minecraft.client.gui.components.Button button
                                        && button.visible && button.getMessage().getString().equals("▼"))
                                .map(child -> (net.minecraft.client.gui.components.Button) child).findFirst().orElseThrow();
                        for (int i = 0; i < 100 && down.active; i++)
                            down.onPress(new net.minecraft.client.input.KeyEvent(257, 0, 0));
                        if (down.active) throw new AssertionError("Crafting instructions have a reachable end");
                    });
                    context.waitTicks(2);
                    context.takeScreenshot("way-of-kings-crafting-end");
                }
                pressGuideLink(context, "screen.stormlight.way_of_kings.contents.back");
                context.runOnClient(client -> {
                    long links = client.gui.screen().children().stream()
                            .filter(child -> child instanceof net.minecraft.client.gui.components.PlainTextButton button && button.visible)
                            .count();
                    if (links != 6) throw new AssertionError("Contents restores all six chapter links");
                });
            }
            context.runOnClient(client -> client.gui.setScreen(new WayOfKingsScreen(
                    new OpenWayOfKingsPayload(POS, 1, 3, 8, 35), 1)));
            context.waitTicks(2);
            context.takeScreenshot("way-of-kings-order");
            context.runOnClient(client -> client.gui.setScreen(new WayOfKingsScreen(
                    new OpenWayOfKingsPayload(POS, 1, 3, 8, 35), 2)));
            context.waitTicks(2);
            context.takeScreenshot("way-of-kings-gem-cutting-tree");
            context.runOnClient(client -> client.gui.setScreen(new WayOfKingsScreen(
                    new OpenWayOfKingsPayload(POS, 1, 3, 8, 40), 2)));
            context.waitTicks(5);
            context.takeScreenshot("way-of-kings-combined-cutting-level-five");
            context.runOnClient(client -> client.gui.setScreen(new WayOfKingsScreen(
                    new OpenWayOfKingsPayload(POS, 1, 3, 8, 35), 3)));
            context.waitTicks(2);
            context.takeScreenshot("way-of-kings-stormlight-guide");
            context.runOnClient(client -> client.gui.setScreen(new WayOfKingsScreen(
                    new OpenWayOfKingsPayload(POS, 0, 0, 0, 0), 1)));
            context.waitTicks(2);
            context.takeScreenshot("way-of-kings-no-order");
            context.runOnClient(client -> client.gui.setScreen(new WayOfKingsScreen(
                    new OpenWayOfKingsPayload(POS, 1, 10, 99, 100), 2)));
            context.waitTicks(2);
            context.takeScreenshot("way-of-kings-mastered-tree");
        }
    }

    private static void pressGuideLink(ClientGameTestContext context, String key) {
        context.runOnClient(client -> {
            String label = net.minecraft.network.chat.Component.translatable(key).getString();
            var link = client.gui.screen().children().stream()
                    .filter(child -> child instanceof net.minecraft.client.gui.components.PlainTextButton button
                            && button.visible && button.getMessage().getString().equals(label))
                    .map(child -> (net.minecraft.client.gui.components.PlainTextButton) child).findFirst().orElseThrow();
            link.onPress(new net.minecraft.client.input.KeyEvent(257, 0, 0));
        });
    }

}
