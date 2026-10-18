package com.scrotey.stormlight;

import com.scrotey.stormlight.client.progression.WayOfKingsScreen;
import com.scrotey.stormlight.item.ModItems;
import com.scrotey.stormlight.item.SphereItem;
import com.scrotey.stormlight.progression.StonecuttingProgression;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class DebugUiClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getServer().runOnServer(server -> {
                var player = world.getConnection().getServerPlayer();
                var commands = server.getCommands();
                var source = server.createCommandSourceStack().withEntity(player);
                commands.performPrefixedCommand(source, "stormlightdebug guide 5 3");
                if (StonecuttingProgression.getCompletedCuts(player) != 0)
                    throw new AssertionError("Guide preview must not change saved progress");
                commands.performPrefixedCommand(source, "stormlightdebug gemlevel 5");
                if (StonecuttingProgression.getLevel(player) != 5)
                    throw new AssertionError("Level setter must set Gem Cutting progress");
                var sphere = (SphereItem) ModItems.DIAMOND_CHIP;
                var stack = new ItemStack(sphere);
                sphere.setCharge(stack, 8);
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                commands.performPrefixedCommand(source, "stormlightdebug wear 95");
                if (stack.getDamageValue() != 95 || sphere.getCharge(stack) != 8)
                    throw new AssertionError("Wear command preserves charge and sets damage");
                commands.performPrefixedCommand(source, "stormlightdebug spheres broam 0");
                int samples = 0;
                for (var sample : player.getInventory().getNonEquipmentItems()) {
                    if (sample.getItem() instanceof SphereItem item && item.getCapacity() >= 200) {
                        samples++;
                        if (item.getCharge(sample) != 0) throw new AssertionError("Dun samples must be empty");
                    }
                }
                if (samples != 20) throw new AssertionError("Expected twenty sample spheres, found " + samples);
                commands.performPrefixedCommand(source, "stormlightdebug books");
                if (player.getInventory().countItem(ModItems.WAY_OF_KINGS) != 1
                        || player.getInventory().countItem(ModItems.MYSTERIOUS_BOOK) != 1
                        || player.getInventory().countItem(ModItems.WORDS_OF_RADIANCE) != 1)
                    throw new AssertionError("Book kit must contain all three books");
            });
            context.waitTicks(3);
            context.runOnClient(client -> {
                if (!(client.gui.screen() instanceof WayOfKingsScreen))
                    throw new AssertionError("Guide command must open the guide UI");
            });
            context.takeScreenshot("debug-guide-preview");
        }
    }
}
