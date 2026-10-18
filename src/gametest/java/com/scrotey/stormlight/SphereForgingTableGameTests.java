package com.scrotey.stormlight;

import com.scrotey.stormlight.block.ModBlocks;
import com.scrotey.stormlight.block.custom.SphereForgingTableBlock;
import com.scrotey.stormlight.block.entity.SphereForgingTableBlockEntity;
import com.scrotey.stormlight.item.ModItems;
import com.scrotey.stormlight.item.SphereItem;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;

public class SphereForgingTableGameTests {
    private static final BlockPos POS = new BlockPos(2, 1, 2);
    private static final int MELT = SphereForgingTableBlockEntity.MELT_TICKS;
    private static final int POUR = SphereForgingTableBlockEntity.POUR_TICKS + 5;
    private static final int COOL = SphereForgingTableBlockEntity.COOL_TICKS + 5;

    private SphereForgingTableBlockEntity forge(GameTestHelper helper) {
        var state = ModBlocks.SPHERE_FORGING_TABLE.defaultBlockState();
        helper.setBlock(POS, state);
        ModBlocks.SPHERE_FORGING_TABLE.setPlacedBy(helper.getLevel(), helper.absolutePos(POS), state,
                null, new ItemStack(ModBlocks.SPHERE_FORGING_TABLE));
        return helper.getBlockEntity(POS, SphereForgingTableBlockEntity.class);
    }

    private static boolean use(SphereForgingTableBlockEntity forge, Player player, ItemStack held) {
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        return forge.interact(player, InteractionHand.MAIN_HAND, true);
    }

    private static int count(Player player, Item item) {
        return player.getInventory().countItem(item);
    }

    @GameTest(maxTicks = 500)
    public void lavaMeltsGlassAndPoursAChip(GameTestHelper h) {
        var forge = forge(h);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var glass = new ItemStack(Items.GLASS, 3);
        for (int i = 0; i < 3; i++) h.assertTrue(use(forge, player, glass), "Glass goes into the crucible");
        h.assertTrue(glass.isEmpty(), "Each glass block is consumed");
        h.assertTrue(!use(forge, player, new ItemStack(ModItems.DIAMOND_NUGGET)), "A gem needs a mould");
        h.assertTrue(use(forge, player, new ItemStack(ModItems.CHIP_MOULD)), "Chip mould goes on the bench");
        h.assertTrue(use(forge, player, new ItemStack(ModItems.DIAMOND_NUGGET)), "Nugget sets in the mould");
        h.assertTrue(!use(forge, player, ItemStack.EMPTY), "A cold forge cannot pour");
        h.assertTrue(!forge.isPouring(), "Nothing poured");
        h.assertTrue(use(forge, player, new ItemStack(Items.LAVA_BUCKET)), "Lava fuels the forge");
        h.assertTrue(player.getMainHandItem().is(Items.BUCKET), "Lava bucket is emptied");
        h.runAtTickTime(5, () -> h.assertTrue(h.getBlockState(POS).getValue(SphereForgingTableBlock.LIT), "Forge lights"));
        int melted = 3 * (MELT + 1) + 5;
        h.runAtTickTime(melted, () -> {
            h.assertValueEqual(forge.getUnmelted(), 0, "All glass melted");
            h.assertValueEqual(forge.getMolten(), 3, "Three units of molten glass");
            h.assertTrue(use(forge, player, ItemStack.EMPTY), "Turning the spigot pours");
            h.assertTrue(forge.isPouring(), "Pour started");
            h.assertValueEqual(forge.getMolten(), 2, "A chip takes one glass");
            h.assertTrue(!use(forge, player, ItemStack.EMPTY), "Cannot pour twice at once");
        });
        h.runAtTickTime(melted + POUR, () -> {
            h.assertTrue(forge.getGem().isEmpty(), "Nugget is sealed in glass");
            h.assertTrue(forge.getResult().is(ModItems.DIAMOND_CHIP), "A diamond chip is cast");
            h.assertTrue(!use(forge, player, ItemStack.EMPTY), "Too hot to take");
        });
        h.runAtTickTime(melted + POUR + COOL, () -> {
            h.assertTrue(use(forge, player, ItemStack.EMPTY), "Cooled sphere can be taken");
            h.assertValueEqual(count(player, ModItems.DIAMOND_CHIP), 1, "Player receives the chip");
            var chip = player.getInventory().getItem(player.getInventory().findSlotMatchingItem(new ItemStack(ModItems.DIAMOND_CHIP)));
            h.assertValueEqual(((SphereItem) chip.getItem()).getCharge(chip), 0, "New spheres are dun");
            h.assertTrue(forge.getResult().isEmpty(), "Mould is clear");
            h.assertTrue(forge.getMould().is(ModItems.CHIP_MOULD), "The mould stays on the bench for the next pour");
            h.succeed();
        });
    }

    @GameTest(maxTicks = 900)
    public void marksAndBroamsCostMoreGlass(GameTestHelper h) {
        var forge = forge(h);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        use(forge, player, new ItemStack(Items.LAVA_BUCKET));
        var glass = new ItemStack(Items.GLASS, 8);
        int lumps = SphereForgingTableBlockEntity.MAX_UNMELTED;
        for (int i = 0; i < lumps; i++) h.assertTrue(use(forge, player, glass), "Glass accepted");
        h.assertTrue(!use(forge, player, glass), "Unmelted glass is limited");
        int t = lumps * (MELT + 1) + 5;
        h.runAtTickTime(t, () -> {
            h.assertValueEqual(forge.getMolten(), lumps, "All the glass melted");
            h.assertTrue(use(forge, player, new ItemStack(ModItems.MARK_MOULD)), "Mark mould goes on the bench");
            h.assertTrue(use(forge, player, new ItemStack(Items.DIAMOND)), "Gem sets in the mould");
            h.assertTrue(!use(forge, player, new ItemStack(Items.EMERALD)), "Mould holds one gem");
            h.assertTrue(use(forge, player, ItemStack.EMPTY), "Pour a mark");
            h.assertValueEqual(forge.getMolten(), lumps - 2, "A mark takes two glass");
        });
        h.runAtTickTime(t + POUR + COOL, () -> {
            h.assertTrue(use(forge, player, ItemStack.EMPTY), "Take the mark");
            h.assertValueEqual(count(player, ModItems.DIAMOND_MARK), 1, "Diamond mark cast");
            h.assertTrue(!use(forge, player, new ItemStack(ModItems.LARGE_RUBY)), "A large gem won't fit a mark mould");
            player.setShiftKeyDown(true);
            h.assertTrue(use(forge, player, ItemStack.EMPTY), "Sneak lifts the empty mould off");
            player.setShiftKeyDown(false);
            h.assertValueEqual(count(player, ModItems.MARK_MOULD), 1, "Mark mould returned");
            h.assertTrue(use(forge, player, new ItemStack(ModItems.BROAM_MOULD)), "Broam mould goes on the bench");
            h.assertTrue(use(forge, player, new ItemStack(ModItems.LARGE_RUBY)), "Large gem sets");
            h.assertTrue(use(forge, player, ItemStack.EMPTY), "Pour a broam");
            h.assertValueEqual(forge.getMolten(), lumps - 5, "A broam takes three glass");
        });
        h.runAtTickTime(t + 2 * (POUR + COOL), () -> {
            h.assertTrue(use(forge, player, ItemStack.EMPTY), "Take the broam");
            h.assertValueEqual(count(player, ModItems.RUBY_BROAM), 1, "Ruby broam cast");
            h.assertTrue(use(forge, player, new ItemStack(ModItems.LARGE_GARNET)), "Another large gem sets");
            h.assertTrue(!use(forge, player, ItemStack.EMPTY), "Not enough glass left for a broam");
            h.assertTrue(!forge.isPouring() && forge.getMolten() == lumps - 5, "Short pour leaves glass untouched");
            h.succeed();
        });
    }

    @GameTest(maxTicks = 100)
    public void glassThrownIntoTheCrucibleIsCaught(GameTestHelper h) {
        var forge = forge(h);
        var vat = SphereForgingTableBlock.vatInterior(h.absolutePos(POS), Direction.EAST).getCenter();
        var thrown = new ItemEntity(h.getLevel(), vat.x, vat.y + 0.6, vat.z, new ItemStack(Items.GLASS, 3));
        // A fresh item entity pops in a random direction; drop it straight in so the test is stable.
        thrown.setDeltaMovement(0, 0, 0);
        h.getLevel().addFreshEntity(thrown);
        h.runAtTickTime(40, () -> {
            h.assertValueEqual(forge.getUnmelted(), 3, "Thrown glass lands in the crucible");
            h.assertTrue(thrown.isRemoved(), "The dropped stack is used up");
            h.succeed();
        });
    }

    @GameTest
    public void onlyTheTapPours(GameTestHelper h) {
        var forge = forge(h);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        use(forge, player, new ItemStack(Items.LAVA_BUCKET));
        use(forge, player, new ItemStack(ModItems.CHIP_MOULD));
        use(forge, player, new ItemStack(ModItems.RUBY_NUGGET));
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        h.assertTrue(!forge.interact(player, InteractionHand.MAIN_HAND, false), "Clicking the hearth does not pour");
        h.assertTrue(!forge.isPouring(), "Nothing poured");
        h.succeed();
    }

    @GameTest
    public void unsupportedItemsAndSneakRetrieval(GameTestHelper h) {
        var forge = forge(h);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        h.assertTrue(!use(forge, player, new ItemStack(Items.DIRT)), "Dirt is not a gem");
        h.assertTrue(!use(forge, player, new ItemStack(ModItems.CHISEL)), "Tools are ignored");
        h.assertTrue(use(forge, player, new ItemStack(ModItems.CHIP_MOULD)), "Mould sets");
        h.assertTrue(!use(forge, player, new ItemStack(ModItems.MARK_MOULD)), "Only one mould at a time");
        h.assertTrue(!use(forge, player, new ItemStack(ModItems.SAPPHIRE)), "A full gem won't fit a chip mould");
        h.assertTrue(use(forge, player, new ItemStack(ModItems.SAPPHIRE_NUGGET)), "Nugget sets");
        player.setShiftKeyDown(true);
        h.assertTrue(use(forge, player, ItemStack.EMPTY), "Sneak takes the gem back");
        h.assertTrue(forge.getGem().isEmpty() && !forge.getMould().isEmpty(), "Gem comes out before the mould");
        h.assertValueEqual(count(player, ModItems.SAPPHIRE_NUGGET), 1, "Nugget returned");
        h.assertTrue(use(forge, player, ItemStack.EMPTY), "Sneak again takes the mould");
        player.setShiftKeyDown(false);
        h.assertTrue(forge.getMould().isEmpty(), "Bench is clear");
        h.assertValueEqual(count(player, ModItems.CHIP_MOULD), 1, "Mould returned");
        h.succeed();
    }

    @GameTest
    public void stateSurvivesSaving(GameTestHelper h) {
        var forge = forge(h);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        use(forge, player, new ItemStack(Items.COAL));
        use(forge, player, new ItemStack(Items.GLASS, 2));
        use(forge, player, new ItemStack(Items.GLASS));
        use(forge, player, new ItemStack(ModItems.MARK_MOULD));
        use(forge, player, new ItemStack(ModItems.GARNET));
        var restored = new SphereForgingTableBlockEntity(forge.getBlockPos(), forge.getBlockState());
        restored.setLevel(h.getLevel());
        restored.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,
                h.getLevel().registryAccess(), forge.saveWithoutMetadata(h.getLevel().registryAccess())));
        h.assertTrue(restored.isLit(), "Fuel survives saving");
        h.assertValueEqual(restored.getUnmelted(), 2, "Unmelted glass survives saving");
        h.assertTrue(restored.getMould().is(ModItems.MARK_MOULD), "Mould survives saving");
        h.assertTrue(restored.getGem().is(ModItems.GARNET), "Gem survives saving");
        h.succeed();
    }

    @GameTest
    public void breakingAnyPartDropsForgeGlassAndGem(GameTestHelper h) {
        int[][] parts = {{0, 0, 0}, {2, 1, 1}, {1, 1, 0}, {0, 0, 1}};
        for (int[] part : parts) {
            h.killAllEntities();
            var forge = forge(h);
            var player = h.makeMockPlayer(GameType.SURVIVAL);
            use(forge, player, new ItemStack(Items.GLASS));
            use(forge, player, new ItemStack(Items.GLASS));
            use(forge, player, new ItemStack(ModItems.MARK_MOULD));
            use(forge, player, new ItemStack(ModItems.RUBY));
            BlockPos target = SphereForgingTableBlock.partPos(h.absolutePos(POS), Direction.EAST, part[0], part[1], part[2]);
            h.assertValueEqual(SphereForgingTableBlock.corePos(target, h.getLevel().getBlockState(target)),
                    h.absolutePos(POS), "Every part shares one forge");
            h.getLevel().destroyBlock(target, true);
            for (int x = 0; x < 3; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++)
                h.assertBlockNotPresent(ModBlocks.SPHERE_FORGING_TABLE, POS.east(x).above(y).south(z));
            h.assertItemEntityCountIs(ModBlocks.SPHERE_FORGING_TABLE.asItem(), POS, 4, 1);
            h.assertItemEntityCountIs(Items.GLASS, POS, 4, 2);
            h.assertItemEntityCountIs(ModItems.RUBY, POS, 4, 1);
            h.assertItemEntityCountIs(ModItems.MARK_MOULD, POS, 4, 1);
        }
        h.succeed();
    }
}
