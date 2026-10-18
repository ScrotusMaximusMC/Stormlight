package com.scrotey.stormlight;

import com.scrotey.stormlight.block.ModBlocks;
import com.scrotey.stormlight.block.custom.GemcuttingTableBlock;
import com.scrotey.stormlight.block.entity.GemcuttingTableBlockEntity;
import com.scrotey.stormlight.item.ModItems;
import com.scrotey.stormlight.progression.StonecuttingProgression;
import com.scrotey.stormlight.progression.RadiantLecternInteraction;
import com.scrotey.stormlight.network.TakeLecternBookPayload;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;

public class GemcuttingTableGameTests {
    private static final BlockPos POS = new BlockPos(2, 1, 2);
    private GemcuttingTableBlockEntity bench(GameTestHelper helper) {
        var state = ModBlocks.GEMCUTTING_TABLE.defaultBlockState();
        helper.setBlock(POS, state);
        ModBlocks.GEMCUTTING_TABLE.setPlacedBy(helper.getLevel(), helper.absolutePos(POS), state,
                null, new ItemStack(ModBlocks.GEMCUTTING_TABLE));
        return helper.getBlockEntity(POS, GemcuttingTableBlockEntity.class);
    }
    @GameTest(maxTicks = 400)
    public void allFiveGemsRequireTenStrikes(GameTestHelper h) {
        var table = bench(h);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        Item[] gems = {Items.DIAMOND, ModItems.GARNET, ModItems.RUBY, ModItems.SAPPHIRE, Items.EMERALD};
        Item[] chips = {ModItems.RAW_DIAMOND_CHIP, ModItems.RAW_GARNET_CHIP, ModItems.RAW_RUBY_CHIP,
                ModItems.RAW_SAPPHIRE_CHIP, ModItems.RAW_EMERALD_CHIP};
        Item[] marks = {ModItems.RAW_DIAMOND_MARK, ModItems.RAW_GARNET_MARK, ModItems.RAW_RUBY_MARK,
                ModItems.RAW_SAPPHIRE_MARK, ModItems.RAW_EMERALD_MARK};
        var chisel = new ItemStack(ModItems.CHISEL);
        for (int g = 0; g < 5; g++) {
            final int index = g;
            int start = g * 75 + 1;
            h.runAtTickTime(start, () -> {
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(gems[index], 2));
                h.assertTrue(table.interact(player, InteractionHand.MAIN_HAND), "Gem loads");
                h.assertValueEqual(player.getMainHandItem().getCount(), 1, "Loads exactly one gem");
                h.assertTrue(!table.interact(player, InteractionHand.MAIN_HAND), "Occupied vise rejects another gem");
                player.setItemInHand(InteractionHand.MAIN_HAND, chisel);
                h.assertTrue(!table.interact(player, InteractionHand.MAIN_HAND), "Wait for the vise to close");
            });
            for (int hit = 0; hit < 10; hit++) {
                final int strike = hit;
                h.runAtTickTime(start + 25 + hit * 4, () -> {
                    h.assertTrue(table.interact(player, InteractionHand.MAIN_HAND), "Chisel strike accepted");
                    h.assertValueEqual(chisel.getDamageValue(), index * 10 + strike + 1, "Each strike wears the tool");
                    if (strike < 9) {
                        h.assertTrue(!table.getGem().isEmpty(), "No early completion");
                        h.assertTrue(!table.interact(player, InteractionHand.MAIN_HAND), "Duplicate same-tick strike ignored");
                    } else {
                        h.assertTrue(table.getGem().isEmpty(), "Tenth strike consumes the gem");
                        var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class,
                                new AABB(h.absolutePos(POS)).inflate(4),
                                e -> e.getItem().is(chips[index]) || e.getItem().is(marks[index]));
                        h.assertValueEqual(drops.size(), 1, "One outcome per gem");
                        var output = drops.getFirst().getItem();
                        h.assertTrue(output.is(chips[index]) ? output.getCount() >= 1 && output.getCount() <= 3
                                : output.getCount() >= 1 && output.getCount() <= 2, "Valid raw yield");
                        h.assertValueEqual(player.getAttachedOrElse(StonecuttingProgression.COMPLETED_CUTS, 0),
                                index + 1, "Only completion awards practice");
                        if (index == 4) h.succeed();
                    }
                });
            }
        }
    }
    @GameTest(maxTicks = 100)
    public void progressSurvivesReloadAndBrokenChisel(GameTestHelper h) {
        var table = bench(h);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RUBY));
        table.interact(player, InteractionHand.MAIN_HAND);
        var chisel = new ItemStack(ModItems.CHISEL);
        chisel.setDamageValue(chisel.getMaxDamage() - 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, chisel);
        h.runAtTickTime(25, () -> {
            h.assertTrue(table.interact(player, InteractionHand.MAIN_HAND), "Last tool use counts");
            h.assertTrue(player.getMainHandItem().isEmpty(), "Chisel breaks");
            var restored = new GemcuttingTableBlockEntity(table.getBlockPos(), table.getBlockState());
            restored.setLevel(h.getLevel());
            restored.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,
                    h.getLevel().registryAccess(), table.saveWithoutMetadata(h.getLevel().registryAccess())));
            h.assertTrue(restored.getGem().is(ModItems.RUBY), "Gem survives saving");
            h.assertValueEqual(restored.getStrikes(), 1, "Damage survives saving");
            h.assertTrue(!restored.interact(player, InteractionHand.MAIN_HAND), "Empty hand cannot chisel");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CHISEL));
            h.assertTrue(restored.interact(player, InteractionHand.MAIN_HAND), "Replacement chisel resumes cutting");
            h.assertValueEqual(restored.getStrikes(), 2, "Progress continues");
            h.succeed();
        });
    }
    @GameTest(maxTicks = 120)
    public void loadedGemDoesNotProcessAutomatically(GameTestHelper h) {
        var table = bench(h);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COAL));
        h.assertTrue(!table.interact(player, InteractionHand.MAIN_HAND), "Invalid gem rejected");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.EMERALD));
        table.interact(player, InteractionHand.MAIN_HAND);
        h.runAtTickTime(100, () -> {
            h.assertTrue(table.getGem().is(Items.EMERALD), "Gem remains without clicks");
            h.assertValueEqual(table.getStrikes(), 0, "No automatic progress");
            h.succeed();
        });
    }
    @GameTest
    public void breakingEitherHalfDropsOneBenchAndItsGem(GameTestHelper h) {
        for (boolean right : new boolean[]{false, true}) {
            h.killAllEntities();
            var table = bench(h);
            var player = h.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND));
            table.interact(player, InteractionHand.MAIN_HAND);
            h.assertValueEqual(GemcuttingTableBlock.inventoryPos(h.absolutePos(POS.east()),
                    h.getBlockState(POS.east())), h.absolutePos(POS), "Both halves share one gem");
            h.getLevel().destroyBlock(h.absolutePos(right ? POS.east() : POS), true);
            h.assertBlockNotPresent(ModBlocks.GEMCUTTING_TABLE, POS);
            h.assertBlockNotPresent(ModBlocks.GEMCUTTING_TABLE, POS.east());
            h.assertItemEntityCountIs(ModBlocks.GEMCUTTING_TABLE.asItem(), POS, 4, 1);
            h.assertItemEntityCountIs(Items.DIAMOND, POS, 4, 1);
        }
        h.succeed();
    }
    @GameTest
    public void exactOddsAndRankThresholds(GameTestHelper h) {
        int[] thresholds = {0, 10, 20, 30, 40, 50, 60, 70, 85, 100};
        for (int rank = 1; rank <= 10; rank++) {
            h.assertValueEqual(StonecuttingProgression.levelForCuts(thresholds[rank - 1]), rank, "Rank threshold");
            if (rank > 1) h.assertValueEqual(StonecuttingProgression.levelForCuts(thresholds[rank - 1] - 1), rank - 1, "Before threshold");
            int chips = 0, single = 0, doubleMarks = 0;
            for (int roll = 0; roll < 100; roll++) {
                var output = StonecuttingProgression.resultForRoll(new ItemStack(Items.DIAMOND), rank, roll, 1);
                if (output.is(ModItems.RAW_DIAMOND_CHIP)) {
                    chips++;
                    h.assertValueEqual(output.getCount(), 1, "Minimum chip yield");
                    h.assertValueEqual(StonecuttingProgression.resultForRoll(new ItemStack(Items.DIAMOND), rank, roll, 2).getCount(), 2, "Middle chip yield");
                    h.assertValueEqual(StonecuttingProgression.resultForRoll(new ItemStack(Items.DIAMOND), rank, roll, 3).getCount(), 3, "Maximum chip yield");
                } else if (output.getCount() == 1) single++;
                else doubleMarks++;
            }
            h.assertValueEqual(chips, 100 - rank * 5, "Exact chip probability");
            h.assertValueEqual(single, new int[]{5, 10, 13, 16, 19, 22, 25, 28, 31, 35}[rank - 1], "Exact single mark probability");
            h.assertValueEqual(doubleMarks, new int[]{0, 0, 2, 4, 6, 8, 10, 12, 14, 15}[rank - 1], "Exact double mark probability");
        }
        h.succeed();
    }

    @GameTest
    public void exactBroamOddsForEveryGemAndRank(GameTestHelper h) {
        Item[] gems = {ModItems.LARGE_DIAMOND, ModItems.LARGE_GARNET, ModItems.LARGE_RUBY,
                ModItems.LARGE_SAPPHIRE, ModItems.LARGE_EMERALD};
        Item[] broams = {ModItems.RAW_DIAMOND_BROAM, ModItems.RAW_GARNET_BROAM, ModItems.RAW_RUBY_BROAM,
                ModItems.RAW_SAPPHIRE_BROAM, ModItems.RAW_EMERALD_BROAM};
        int[] doubles = {5, 12, 19, 26, 33, 40}, triples = {0, 2, 4, 6, 8, 10};
        for (int g = 0; g < gems.length; g++) {
            for (int rank = 1; rank <= 10; rank++) {
                int[] counts = new int[4];
                for (int roll = 0; roll < 100; roll++) {
                    var result = StonecuttingProgression.broamResultForRoll(new ItemStack(gems[g]), rank, roll);
                    if (rank < 5) h.assertTrue(result.isEmpty(), "Broams locked before level 5");
                    else {
                        h.assertTrue(result.is(broams[g]), "Cut keeps gemstone type");
                        h.assertTrue(result.getCount() >= 1 && result.getCount() <= 3, "Guaranteed broam yield");
                        counts[result.getCount()]++;
                    }
                }
                if (rank >= 5) {
                    h.assertValueEqual(counts[2], doubles[rank - 5], "Exact double broam odds");
                    h.assertValueEqual(counts[3], triples[rank - 5], "Exact triple broam odds");
                    h.assertValueEqual(counts[1], 100 - doubles[rank - 5] - triples[rank - 5], "Remaining rolls give one broam");
                }
            }
        }
        h.succeed();
    }

    @GameTest(maxTicks = 100)
    public void broamCuttingRequiresRankAndDiamondChiselOnEveryStrike(GameTestHelper h) {
        var table = bench(h);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var lowRank = h.makeMockPlayer(GameType.SURVIVAL);
        player.setAttached(StonecuttingProgression.COMPLETED_CUTS, 39);
        var largeGem = new ItemStack(ModItems.LARGE_DIAMOND, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, largeGem);
        h.assertTrue(!table.interact(player, InteractionHand.MAIN_HAND), "Level 4 cannot load large gems");
        h.assertValueEqual(largeGem.getCount(), 2, "Rejected gem is preserved");
        player.setAttached(StonecuttingProgression.COMPLETED_CUTS, 40);
        h.assertTrue(table.interact(player, InteractionHand.MAIN_HAND), "Level 5 loads a large gem");
        h.assertValueEqual(largeGem.getCount(), 1, "One large gem is consumed");
        var diamondChisel = new ItemStack(ModItems.DIAMOND_CHISEL);
        h.runAtTickTime(25, () -> {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CHISEL));
            h.assertTrue(!table.interact(player, InteractionHand.MAIN_HAND), "Iron chisel cannot cut broams");
            h.assertValueEqual(player.getMainHandItem().getDamageValue(), 0, "Rejected strike does not damage tool");
            lowRank.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DIAMOND_CHISEL));
            h.assertTrue(!table.interact(lowRank, InteractionHand.MAIN_HAND), "Low rank cannot work another player's gem");
            h.assertValueEqual(table.getStrikes(), 0, "Rejected strikes give no progress");
            player.setItemInHand(InteractionHand.MAIN_HAND, diamondChisel);
        });
        for (int hit = 0; hit < 10; hit++) {
            int strike = hit;
            h.runAtTickTime(26 + hit * 4, () -> {
                h.assertTrue(table.interact(player, InteractionHand.MAIN_HAND), "Diamond chisel strike accepted");
                h.assertValueEqual(diamondChisel.getDamageValue(), strike + 1, "Diamond chisel wears per strike");
                if (strike == 9) {
                    h.assertTrue(table.getGem().isEmpty(), "Ten strikes finish the large gem");
                    h.assertValueEqual(StonecuttingProgression.getCompletedCuts(player), 41, "Broam cut earns practice");
                    var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(POS)).inflate(4),
                            e -> e.getItem().is(ModItems.RAW_DIAMOND_BROAM));
                    h.assertValueEqual(drops.size(), 1, "Raw broams dropped");
                    h.assertTrue(drops.getFirst().getItem().getCount() >= 1, "At least one broam guaranteed");
                    h.succeed();
                }
            });
        }
    }

    @GameTest
    public void wayOfKingsCanBeTakenFromLectern(GameTestHelper h) {
        h.setBlock(POS, Blocks.LECTERN);
        var lectern = h.getBlockEntity(POS, LecternBlockEntity.class);
        lectern.setBook(new ItemStack(ModItems.WAY_OF_KINGS));
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(h.absolutePos(POS).getX() + 0.5, h.absolutePos(POS).getY() + 1,
                h.absolutePos(POS).getZ() + 0.5);
        RadiantLecternInteraction.takeLecternBook(player, new TakeLecternBookPayload(h.absolutePos(POS)));
        h.assertTrue(lectern.getBook().isEmpty(), "Book removed from lectern");
        h.assertValueEqual(player.getInventory().countItem(ModItems.WAY_OF_KINGS), 1,
                "Reader receives the same book");
        h.succeed();
    }

}
