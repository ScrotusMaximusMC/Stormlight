package com.scrotey.stormlight.block.entity;

import com.scrotey.stormlight.block.custom.SphereJarBlock;
import com.scrotey.stormlight.item.ModItemTags;
import com.scrotey.stormlight.item.SphereItem;
import com.scrotey.stormlight.screen.SphereJarMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class SphereJarBlockEntity
        extends BlockEntity
        implements ImplementedContainer, MenuProvider {

    public static final int SLOTS_PER_COMPARTMENT = 16;
    public static final int TOTAL_SLOTS =
            SLOTS_PER_COMPARTMENT * 3;

    public static final int MARK_SLOT_START = 16;
    public static final int BROAM_SLOT_START = 32;

    private final NonNullList<ItemStack> items =
            NonNullList.withSize(
                    TOTAL_SLOTS,
                    ItemStack.EMPTY
            );

    public SphereJarBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                ModBlockEntities.SPHERE_JAR_BLOCK_ENTITY,
                pos,
                state
        );
    }

    @Override
    public void setChanged() {
        super.setChanged();
        updateBlockState();
    }

    private void updateBlockState() {
        if (level == null || level.isClientSide()) {
            return;
        }

        BlockState state = getBlockState();

        if (!state.hasProperty(SphereJarBlock.FILL_LEVEL)
                || !state.hasProperty(SphereJarBlock.LIGHT_LEVEL)) {
            return;
        }

        int occupiedSlots = 0;
        int totalCharge = 0;

        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                occupiedSlots++;
            }

            if (stack.getItem() instanceof SphereItem sphere) {
                totalCharge += sphere.getCharge(stack);
            }
        }

        int newFillLevel = occupiedSlots == 0
                ? 0
                : Math.min(
                4,
                (occupiedSlots + 11) / 12
        );

        int newLightLevel = lightLevelForCharge(totalCharge);

        if (state.getValue(SphereJarBlock.FILL_LEVEL) != newFillLevel
                || state.getValue(SphereJarBlock.LIGHT_LEVEL) != newLightLevel) {

            level.setBlockAndUpdate(
                    worldPosition,
                    state.setValue(
                                    SphereJarBlock.FILL_LEVEL,
                                    newFillLevel
                            )
                            .setValue(
                                    SphereJarBlock.LIGHT_LEVEL,
                                    newLightLevel
                            )
            );
        }
    }

    /**
     * Maps the jar's total stored Stormlight (summed across every
     * sphere inside, regardless of how many spheres or their capacity)
     * to a block light level - more Stormlight glows brighter and
     * further, independent of how full the jar looks.
     */
    private static int lightLevelForCharge(int totalCharge) {
        if (totalCharge <= 0) {
            return 0;
        }

        if (totalCharge < 10) {
            return 2;
        }

        if (totalCharge < 25) {
            return 4;
        }

        if (totalCharge < 50) {
            return 6;
        }

        if (totalCharge < 100) {
            return 8;
        }

        if (totalCharge < 250) {
            return 10;
        }

        if (totalCharge < 500) {
            return 12;
        }

        if (totalCharge < 1000) {
            return 14;
        }

        return 15;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(
                "block.stormlight.sphere_jar"
        );
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(
            int containerId,
            Inventory inventory,
            Player player
    ) {
        return new SphereJarMenu(
                containerId,
                inventory,
                this
        );
    }

    @Override
    public NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(
            int slot,
            ItemStack stack
    ) {
        if (slot < 0 || slot >= TOTAL_SLOTS) {
            return false;
        }

        if (!(stack.getItem() instanceof SphereItem)) {
            return false;
        }

        if (slot < MARK_SLOT_START) {
            return stack.is(ModItemTags.CHIPS);
        }

        if (slot < BROAM_SLOT_START) {
            return stack.is(ModItemTags.MARKS);
        }

        return stack.is(ModItemTags.BROAMS);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(
                this,
                player
        );
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        ContainerHelper.loadAllItems(
                input,
                items
        );
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        ContainerHelper.saveAllItems(
                output,
                items
        );

        super.saveAdditional(output);
    }
}