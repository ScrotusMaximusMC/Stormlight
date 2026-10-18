package com.scrotey.stormlight;

import com.scrotey.stormlight.item.ModItems;
import com.scrotey.stormlight.item.SphereItem;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Shared cracks must remain legible on every gem, size and charge stage. */
public class SphereWearClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            for (int chargeStage = 0; chargeStage <= 4; chargeStage++) {
                int stage = chargeStage;
                context.runOnClient(client -> client.gui.setScreen(new WearPreview(stage)));
                context.waitTicks(3);
                context.takeScreenshot("sphere-wear-charge-" + stage);
            }
        }
    }

    private static class WearPreview extends Screen {
        private final int chargeStage;
        private final Item[] spheres = {
                ModItems.DIAMOND_CHIP, ModItems.DIAMOND_MARK, ModItems.DIAMOND_BROAM,
                ModItems.GARNET_CHIP, ModItems.GARNET_MARK, ModItems.GARNET_BROAM,
                ModItems.RUBY_CHIP, ModItems.RUBY_MARK, ModItems.RUBY_BROAM,
                ModItems.SAPPHIRE_CHIP, ModItems.SAPPHIRE_MARK, ModItems.SAPPHIRE_BROAM,
                ModItems.EMERALD_CHIP, ModItems.EMERALD_MARK, ModItems.EMERALD_BROAM};

        WearPreview(int chargeStage) {
            super(Component.literal("Sphere wear"));
            this.chargeStage = chargeStage;
        }

        @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            g.fill(0, 0, width, height, 0xFFEAD8A8);
            int left = (width - 390) / 2, top = (height - 200) / 2;
            g.text(font, "Charge stage " + chargeStage + " | columns: chip / mark / broam", left, top, 0xFF35251A, false);
            String[] labels = {"Pristine", "Light", "Moderate", "Nearly spent"};
            double[] damage = {0, 0.1, 0.5, 0.95};
            for (int row = 0; row < labels.length; row++) {
                int y = top + 30 + row * 42;
                g.text(font, labels[row], left, y + 4, 0xFF35251A, false);
                for (int col = 0; col < spheres.length; col++) {
                    var stack = new ItemStack(spheres[col]);
                    var sphere = (SphereItem) spheres[col];
                    sphere.setCharge(stack, sphere.getCapacity() * chargeStage / 4);
                    stack.setDamageValue((int) (stack.getMaxDamage() * damage[row]));
                    g.item(stack, left + 90 + col * 20, y);
                }
            }
        }
    }
}
