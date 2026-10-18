package com.scrotey.stormlight.progression;

import com.mojang.serialization.Codec;
import com.scrotey.stormlight.Stormlight;
import com.scrotey.stormlight.item.ModItems;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** A linear skill tree; only completed, non-creative cuts award practice. */
public final class StonecuttingProgression {
    private static final int[] THRESHOLDS = {0, 10, 20, 30, 40, 50, 60, 70, 85, 100};
    public static final int MAX_LEVEL = THRESHOLDS.length;
    public static final int MAX_CUTS = THRESHOLDS[MAX_LEVEL - 1];
    private static final int[] DOUBLE_MARK_CHANCES = {0, 0, 2, 4, 6, 8, 10, 12, 14, 15};

    public static final AttachmentType<Integer> COMPLETED_CUTS = AttachmentRegistry.<Integer>create(
            Stormlight.id("stonecutting_completed_cuts"), b -> b.initializer(() -> 0)
                    .persistent(Codec.intRange(0, MAX_CUTS)).copyOnDeath());

    private StonecuttingProgression() {}
    public static void initialize() {}

    public static int levelForCuts(int cuts) {
        int level = 1;
        while (level < THRESHOLDS.length && cuts >= THRESHOLDS[level]) level++;
        return level;
    }
    public static int cutsForLevel(int level) {
        return THRESHOLDS[Math.clamp(level, 1, MAX_LEVEL) - 1];
    }
    public static int chipChance(int level) { return 100 - Math.clamp(level, 1, MAX_LEVEL) * 5; }
    public static int doubleMarkChance(int level) { return DOUBLE_MARK_CHANCES[Math.clamp(level, 1, MAX_LEVEL) - 1]; }
    public static int singleMarkChance(int level) { return 100 - chipChance(level) - doubleMarkChance(level); }
    public static int getCompletedCuts(Player player) {
        return player.getAttachedOrElse(COMPLETED_CUTS, 0);
    }
    public static int getLevel(Player player) {
        return levelForCuts(player.getAttachedOrElse(COMPLETED_CUTS, 0));
    }

    /** Each rank shifts five percentage points from chips to marks. */
    public static boolean isMarkRoll(int level, int roll) {
        return roll >= 100 - Math.clamp(level, 1, MAX_LEVEL) * 5;
    }
    public static ItemStack resultForRoll(ItemStack gem, int level, int roll, int chipCount) {
        if (roll < 0 || roll >= 100) throw new IllegalArgumentException("Roll must be 0..99");
        if (chipCount < 1 || chipCount > 3) throw new IllegalArgumentException("Chip count must be 1..3");
        Item chip, mark;
        if (gem.is(Items.DIAMOND)) { chip = ModItems.RAW_DIAMOND_CHIP; mark = ModItems.RAW_DIAMOND_MARK; }
        else if (gem.is(ModItems.GARNET)) { chip = ModItems.RAW_GARNET_CHIP; mark = ModItems.RAW_GARNET_MARK; }
        else if (gem.is(ModItems.RUBY)) { chip = ModItems.RAW_RUBY_CHIP; mark = ModItems.RAW_RUBY_MARK; }
        else if (gem.is(ModItems.SAPPHIRE)) { chip = ModItems.RAW_SAPPHIRE_CHIP; mark = ModItems.RAW_SAPPHIRE_MARK; }
        else if (gem.is(Items.EMERALD)) { chip = ModItems.RAW_EMERALD_CHIP; mark = ModItems.RAW_EMERALD_MARK; }
        else return ItemStack.EMPTY;
        int doubleMarkChance = DOUBLE_MARK_CHANCES[Math.clamp(level, 1, MAX_LEVEL) - 1];
        return isMarkRoll(level, roll) ? new ItemStack(mark, roll >= 100 - doubleMarkChance ? 2 : 1)
                : new ItemStack(chip, chipCount);
    }
    public static ItemStack rollResult(ItemStack gem, Player player, RandomSource random) {
        return resultForRoll(gem, getLevel(player), random.nextInt(100), 1 + random.nextInt(3));
    }
    public static void completeCut(Player player) {
        if (player.isCreative()) return;
        int oldLevel = getLevel(player);
        player.setAttached(COMPLETED_CUTS, Math.min(MAX_CUTS, player.getAttachedOrElse(COMPLETED_CUTS, 0) + 1));
        if (oldLevel != getLevel(player)) player.sendOverlayMessage(
                Component.translatable("message.stormlight.stonecutting.level_up", getLevel(player)));
    }
    public static void showStatus(Player player) {
        int level = getLevel(player), doubles = DOUBLE_MARK_CHANCES[level - 1];
        int chips = 100 - level * 5, singles = level * 5 - doubles;
        if (level == MAX_LEVEL) player.sendOverlayMessage(Component.translatable(
                "message.stormlight.stonecutting.master", level, chips, singles, doubles));
        else player.sendOverlayMessage(Component.translatable("message.stormlight.stonecutting.status", level,
                player.getAttachedOrElse(COMPLETED_CUTS, 0) - THRESHOLDS[level - 1],
                THRESHOLDS[level] - THRESHOLDS[level - 1], chips, singles, doubles));
    }
}
