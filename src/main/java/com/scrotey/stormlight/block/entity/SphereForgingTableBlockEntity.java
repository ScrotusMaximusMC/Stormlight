package com.scrotey.stormlight.block.entity;

import com.scrotey.stormlight.block.custom.SphereForgingTableBlock;
import com.scrotey.stormlight.recipe.ModRecipes;
import com.scrotey.stormlight.recipe.SphereForgingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.util.Optional;

public class SphereForgingTableBlockEntity extends BlockEntity {
    /** Molten glass is measured in glass blocks; this is the crucible's total volume. */
    public static final int CAPACITY = 16;
    public static final int MAX_UNMELTED = 6;
    public static final int MELT_TICKS = 80;
    public static final int MAX_BURN = 24000;
    public static final int POUR_TICKS = 40;
    public static final int COOL_TICKS = 60;
    public static final int VALVE_TICKS = 8;

    private int burnTime, unmelted, meltProgress, molten, pourTicks, pourCost, coolTicks, valveTicks, previousValveTicks;
    private ItemStack mould = ItemStack.EMPTY;
    private ItemStack result = ItemStack.EMPTY;

    public SphereForgingTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPHERE_FORGING_TABLE, pos, state);
    }

    public boolean isLit() { return burnTime > 0; }
    public int getBurnTime() { return burnTime; }
    public int getUnmelted() { return unmelted; }
    public int getMolten() { return molten; }
    public float getMeltProgress() { return meltProgress / (float) MELT_TICKS; }
    public boolean isPouring() { return pourTicks > 0; }
    public int getPourCost() { return pourCost; }
    public float getPour(float partialTick) {
        return isPouring() ? Math.min(1, (pourTicks - 1 + partialTick) / POUR_TICKS) : 0;
    }
    /** 1 when freshly cast, fading to 0 once the sphere can be handled. */
    public float getHeat(float partialTick) {
        return coolTicks <= 0 ? 0 : Math.max(0, (coolTicks - partialTick) / COOL_TICKS);
    }
    public float getValve(float partialTick) {
        return (previousValveTicks + (valveTicks - previousValveTicks) * partialTick) / VALVE_TICKS;
    }
    public ItemStack getMould() { return mould.copy(); }
    public ItemStack getResult() { return result.copy(); }

    public static void tick(Level level, BlockPos pos, BlockState state, SphereForgingTableBlockEntity forge) {
        forge.previousValveTicks = forge.valveTicks;
        int valveTarget = forge.isPouring() ? VALVE_TICKS : 0;
        if (forge.valveTicks != valveTarget) forge.valveTicks += Integer.signum(valveTarget - forge.valveTicks);
        // The client advances the same timers so animations stay smooth between syncs.
        if (forge.isPouring() && forge.pourTicks < POUR_TICKS) forge.pourTicks++;
        if (forge.coolTicks > 0) forge.coolTicks--;
        if (forge.isLit() && forge.unmelted > 0 && forge.meltProgress < MELT_TICKS) forge.meltProgress++;
        if (level.isClientSide()) {
            if (forge.burnTime > 0) forge.burnTime--;
            return;
        }
        if (forge.burnTime > 0) {
            forge.burnTime--;
            forge.setChanged();
        }
        boolean changed = level.getGameTime() % 4 == 0 && forge.catchThrownGlass((ServerLevel) level, state);
        if (forge.meltProgress >= MELT_TICKS) {
            forge.unmelted--;
            forge.molten++;
            forge.meltProgress = 0;
            forge.sound(SoundEvents.LAVA_POP, 0.6F, 1.1F);
            changed = true;
        }
        if (forge.isPouring() && forge.pourTicks >= POUR_TICKS) {
            forge.finishPour((ServerLevel) level);
            changed = true;
        }
        if (forge.coolTicks == 1) forge.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 1.4F);
        if (state.getValue(SphereForgingTableBlock.LIT) != forge.isLit()) {
            SphereForgingTableBlock.setLit(level, pos, state, forge.isLit());
            if (!forge.isLit()) forge.sound(SoundEvents.FIRE_EXTINGUISH, 0.5F, 0.8F);
            changed = true;
        }
        if (changed) forge.sync();
    }

    /** Glass dropped into the crucible from the steps is drawn in as if it had been placed by hand. */
    private boolean catchThrownGlass(ServerLevel server, BlockState state) {
        var vat = SphereForgingTableBlock.vatInterior(worldPosition, state.getValue(SphereForgingTableBlock.FACING));
        boolean caught = false;
        for (ItemEntity drop : server.getEntitiesOfClass(ItemEntity.class, vat, e -> e.getItem().is(Items.GLASS))) {
            ItemStack stack = drop.getItem();
            while (!stack.isEmpty() && canAcceptGlass()) {
                stack.shrink(1);
                unmelted++;
                caught = true;
            }
            if (stack.isEmpty()) drop.discard();
            else drop.setItem(stack);
        }
        if (caught) sound(isLit() ? SoundEvents.LAVA_POP : SoundEvents.GLASS_PLACE, 0.8F, 0.9F);
        if (isLit()) {
            // The crucible is no place to stand.
            for (LivingEntity entity : server.getEntitiesOfClass(LivingEntity.class, vat)) entity.igniteForSeconds(4);
        }
        return caught;
    }

    private boolean canAcceptGlass() {
        return unmelted < MAX_UNMELTED && unmelted + molten < CAPACITY;
    }

    /** {@code atTap} is true when the player used the casting bench, where the tap and mould are. */
    public boolean interact(Player player, InteractionHand hand, boolean atTap) {
        if (!(level instanceof ServerLevel server)) return false;
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            if (player.isShiftKeyDown()) return takeBack(player);
            if (!atTap && result.isEmpty()) {
                tell(player, "use_tap");
                return false;
            }
            return useEmptyHand(player, server);
        }
        if (held.is(Items.GLASS)) return addGlass(player, held);
        int fuel = server.fuelValues().burnDuration(held);
        if (fuel > 0) return addFuel(player, hand, held, fuel);
        return insertGem(player, held, server);
    }

    private boolean addGlass(Player player, ItemStack held) {
        if (!canAcceptGlass()) {
            tell(player, "full");
            return false;
        }
        unmelted++;
        if (!player.isCreative()) held.shrink(1);
        sound(SoundEvents.GLASS_PLACE, 0.8F, 0.9F);
        if (!isLit()) tell(player, "cold");
        sync();
        return true;
    }

    private boolean addFuel(Player player, InteractionHand hand, ItemStack held, int fuel) {
        if (burnTime + fuel > MAX_BURN) return false;
        boolean lava = held.is(Items.LAVA_BUCKET);
        if (!player.isCreative()) {
            if (lava) player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, new ItemStack(Items.BUCKET)));
            else held.shrink(1);
        }
        burnTime += fuel;
        sound(lava ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.FIRECHARGE_USE, 0.8F, 1.0F);
        setChanged();
        return true;
    }

    private boolean insertGem(Player player, ItemStack held, ServerLevel server) {
        if (!mould.isEmpty() || !result.isEmpty() || isPouring()) return false;
        if (recipeFor(held, server).isEmpty()) return false;
        mould = held.copyWithCount(1);
        if (!player.isCreative()) held.shrink(1);
        sound(SoundEvents.STONE_PLACE, 0.6F, 1.5F);
        sync();
        return true;
    }

    private boolean useEmptyHand(Player player, ServerLevel server) {
        if (!result.isEmpty()) {
            if (coolTicks > 0) {
                tell(player, "hot");
                return false;
            }
            player.getInventory().placeItemBackInInventory(result);
            result = ItemStack.EMPTY;
            sound(SoundEvents.ITEM_PICKUP, 0.4F, 1.0F);
            sync();
            return true;
        }
        if (isPouring()) return false;
        if (mould.isEmpty()) {
            tell(player, "empty_mould");
            return false;
        }
        var recipe = recipeFor(mould, server).orElse(null);
        if (recipe == null) return false;
        int cost = recipe.value().glass();
        if (!isLit()) {
            tell(player, "cold");
            return false;
        }
        if (molten < cost) {
            player.sendOverlayMessage(Component.translatable("message.stormlight.sphere_forge.glass", cost, molten));
            return false;
        }
        molten -= cost;
        pourCost = cost;
        pourTicks = 1;
        sound(SoundEvents.IRON_TRAPDOOR_OPEN, 0.5F, 0.7F);
        sound(SoundEvents.POINTED_DRIPSTONE_DRIP_LAVA_INTO_CAULDRON, 1.0F, 0.8F);
        sync();
        return true;
    }

    private boolean takeBack(Player player) {
        if (isPouring()) return false;
        if (!result.isEmpty()) return useEmptyHand(player, (ServerLevel) level);
        if (mould.isEmpty()) return false;
        player.getInventory().placeItemBackInInventory(mould);
        mould = ItemStack.EMPTY;
        sync();
        return true;
    }

    private void finishPour(ServerLevel server) {
        var recipe = recipeFor(mould, server);
        result = recipe.map(r -> r.value().assemble(new SingleRecipeInput(mould))).orElse(ItemStack.EMPTY);
        // A recipe removed mid-pour by a datapack reload returns the gem rather than eating it.
        if (result.isEmpty()) result = mould;
        mould = ItemStack.EMPTY;
        pourTicks = 0;
        pourCost = 0;
        coolTicks = COOL_TICKS;
        sound(SoundEvents.LAVA_EXTINGUISH, 0.6F, 1.3F);
        var steam = SphereForgingTableBlock.local(worldPosition, getBlockState().getValue(SphereForgingTableBlock.FACING),
                39, 10, 16);
        server.sendParticles(ParticleTypes.CLOUD, steam.x, steam.y, steam.z, 6, 0.1, 0.05, 0.1, 0.01);
    }

    private static Optional<RecipeHolder<SphereForgingRecipe>> recipeFor(ItemStack stack, ServerLevel server) {
        return server.recipeAccess().getRecipeFor(ModRecipes.SPHERE_FORGING_TYPE, new SingleRecipeInput(stack), server);
    }

    private static void tell(Player player, String key) {
        player.sendOverlayMessage(Component.translatable("message.stormlight.sphere_forge." + key));
    }

    private void sound(SoundEvent sound, float volume, float pitch) {
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
        if (!mould.isEmpty()) output.store("Mould", ItemStack.CODEC, mould);
        if (!result.isEmpty()) output.store("Result", ItemStack.CODEC, result);
        output.putInt("BurnTime", burnTime);
        output.putInt("Unmelted", unmelted);
        output.putInt("MeltProgress", meltProgress);
        output.putInt("Molten", molten);
        output.putInt("PourTicks", pourTicks);
        output.putInt("PourCost", pourCost);
        output.putInt("CoolTicks", coolTicks);
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        mould = input.read("Mould", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        result = input.read("Result", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        burnTime = Math.clamp(input.getIntOr("BurnTime", 0), 0, MAX_BURN);
        unmelted = Math.clamp(input.getIntOr("Unmelted", 0), 0, MAX_UNMELTED);
        meltProgress = Math.clamp(input.getIntOr("MeltProgress", 0), 0, MELT_TICKS);
        molten = Math.clamp(input.getIntOr("Molten", 0), 0, CAPACITY - unmelted);
        pourTicks = mould.isEmpty() ? 0 : Math.clamp(input.getIntOr("PourTicks", 0), 0, POUR_TICKS);
        pourCost = pourTicks == 0 ? 0 : Math.clamp(input.getIntOr("PourCost", 0), 0, CAPACITY);
        coolTicks = result.isEmpty() ? 0 : Math.clamp(input.getIntOr("CoolTicks", 0), 0, COOL_TICKS);
        previousValveTicks = valveTicks = pourTicks > 0 ? VALVE_TICKS : 0;
    }
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !level.isClientSide()) {
            // Molten glass and fuel are lost with the forge; solid items come back.
            Block.popResource(level, pos, mould);
            Block.popResource(level, pos, result);
            if (unmelted > 0) Block.popResource(level, pos, new ItemStack(Items.GLASS, unmelted));
            mould = result = ItemStack.EMPTY;
            unmelted = 0;
        }
        super.preRemoveSideEffects(pos, state);
    }
}
