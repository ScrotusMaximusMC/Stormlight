package com.scrotey.stormlight.block.entity;

import com.scrotey.stormlight.item.ModItems;
import com.scrotey.stormlight.recipe.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class GemcuttingTableBlockEntity extends BlockEntity {
    public static final int REQUIRED_STRIKES = 10;
    public static final int CLAMP_TICKS = 20;
    private ItemStack gem = ItemStack.EMPTY;
    // Preserve inventories from the earlier screen-based table until collected or broken.
    private final NonNullList<ItemStack> legacyItems = NonNullList.withSize(3, ItemStack.EMPTY);
    private int strikes, clampTicks, previousClampTicks, gemKind;
    private long lastStrike = Long.MIN_VALUE;

    public GemcuttingTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GEMCUTTING_TABLE, pos, state);
    }

    public static boolean isGem(ItemStack stack) {
        return stack.is(Items.DIAMOND) || stack.is(Items.EMERALD) || stack.is(ModItems.GARNET)
                || stack.is(ModItems.RUBY) || stack.is(ModItems.SAPPHIRE);
    }

    public ItemStack getGem() { return gem.copy(); }
    public int getStrikes() { return strikes; }
    public int getGemKind() { return gemKind; }
    public float getClamp(float partialTick) {
        return (previousClampTicks + (clampTicks - previousClampTicks) * partialTick) / CLAMP_TICKS;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GemcuttingTableBlockEntity table) {
        table.previousClampTicks = table.clampTicks;
        int target = table.gem.isEmpty() ? 0 : CLAMP_TICKS;
        if (table.clampTicks != target) {
            table.clampTicks += Integer.signum(target - table.clampTicks);
            if (!level.isClientSide()) table.setChanged();
        }
    }

    public boolean interact(Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel server)) return false;
        for (int i = 0; i < legacyItems.size(); i++) {
            if (!legacyItems.get(i).isEmpty()) {
                player.getInventory().placeItemBackInInventory(legacyItems.get(i));
                legacyItems.set(i, ItemStack.EMPTY);
                setChanged();
            }
        }
        ItemStack held = player.getItemInHand(hand);
        if (gem.isEmpty() && isGem(held)) {
            var input = new SingleRecipeInput(held);
            if (server.recipeAccess().getRecipeFor(ModRecipes.GEMCUTTING_TYPE, input, server).isEmpty()) return false;
            gem = held.copyWithCount(1);
            if (!player.isCreative()) held.shrink(1);
            gemKind = gem.is(Items.DIAMOND) ? 0 : gem.is(ModItems.GARNET) ? 1
                    : gem.is(ModItems.RUBY) ? 2 : gem.is(ModItems.SAPPHIRE) ? 3 : 4;
            strikes = 0;
            lastStrike = Long.MIN_VALUE;
            sound(SoundEvents.GRINDSTONE_USE, 0.45F, 0.8F);
            sync();
            return true;
        }
        if (held.isEmpty() && player.isShiftKeyDown() && !gem.isEmpty()) {
            player.getInventory().placeItemBackInInventory(gem);
            gem = ItemStack.EMPTY;
            strikes = 0;
            sync();
            return true;
        }
        if (!held.is(ModItems.CHISEL) || held.isBroken() || gem.isEmpty() || clampTicks < CLAMP_TICKS) return false;
        long now = level.getGameTime();
        if (lastStrike != Long.MIN_VALUE && now - lastStrike < 4) return false;
        var input = new SingleRecipeInput(gem);
        var recipe = server.recipeAccess().getRecipeFor(ModRecipes.GEMCUTTING_TYPE, input, server).orElse(null);
        if (recipe == null) return false;
        ItemStack result = recipe.value().assemble(input);
        if (result.isEmpty()) return false;
        lastStrike = now;
        held.hurtAndBreak(1, player, hand);
        sound(SoundEvents.STONE_HIT, 0.7F, 1.4F + strikes * 0.035F);
        sound(SoundEvents.ANVIL_HIT, 0.15F, 1.8F);
        if (++strikes == REQUIRED_STRIKES) {
            ItemEntity drop = new ItemEntity(level, worldPosition.getX() + 0.5,
                    worldPosition.getY() + 1.4, worldPosition.getZ() + 0.5, result);
            drop.setDefaultPickUpDelay();
            level.addFreshEntity(drop);
            gem = ItemStack.EMPTY;
            strikes = 0;
            sound(SoundEvents.AMETHYST_BLOCK_BREAK, 0.7F, 1.2F);
        }
        sync();
        return true;
    }

    private void sound(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, volume, pitch);
    }
    private void sync() {
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, legacyItems);
        output.putInt("ManualCuttingVersion", 1);
        if (!gem.isEmpty()) output.store("Gem", ItemStack.CODEC, gem);
        output.putInt("Strikes", strikes);
        output.putInt("ClampTicks", clampTicks);
        output.putInt("GemKind", gemKind);
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        legacyItems.clear();
        ContainerHelper.loadAllItems(input, legacyItems);
        gem = input.read("Gem", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        if (input.getIntOr("ManualCuttingVersion", 0) == 0 && isGem(legacyItems.get(0))) {
            gem = legacyItems.get(0).split(1);
        }
        strikes = gem.isEmpty() ? 0 : Math.clamp(input.getIntOr("Strikes", 0), 0, 9);
        previousClampTicks = clampTicks = Math.clamp(input.getIntOr("ClampTicks", 0), 0, CLAMP_TICKS);
        gemKind = gem.isEmpty() ? Math.clamp(input.getIntOr("GemKind", 0), 0, 4)
                : gem.is(Items.DIAMOND) ? 0 : gem.is(ModItems.GARNET) ? 1
                : gem.is(ModItems.RUBY) ? 2 : gem.is(ModItems.SAPPHIRE) ? 3 : 4;
    }
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !level.isClientSide()) {
            Block.popResource(level, pos, gem);
            gem = ItemStack.EMPTY;
            for (var item : legacyItems) Block.popResource(level, pos, item);
            legacyItems.clear();
        }
        super.preRemoveSideEffects(pos, state);
    }
}
