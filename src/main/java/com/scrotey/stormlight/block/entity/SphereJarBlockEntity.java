package com.scrotey.stormlight.block.entity;

import com.scrotey.stormlight.block.custom.SphereJarBlock;
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

    private static final int CHIP_CAPACITY = 10;
    private static final int MARK_CAPACITY = 50;
    private static final int BROAM_CAPACITY = 200;

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
        updateFillLevel();
    }

    private void updateFillLevel() {
        if (level == null || level.isClientSide()) {
            return;
        }

        int occupiedSlots = 0;

        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                occupiedSlots++;
            }
        }

        int newFillLevel = occupiedSlots == 0
                ? 0
                : Math.min(
                4,
                (occupiedSlots + 11) / 12
        );

        BlockState state = getBlockState();

        if (state.hasProperty(SphereJarBlock.FILL_LEVEL)
                && state.getValue(SphereJarBlock.FILL_LEVEL)
                != newFillLevel) {

            level.setBlockAndUpdate(
                    worldPosition,
                    state.setValue(
                            SphereJarBlock.FILL_LEVEL,
                            newFillLevel
                    )
            );
        }
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

        if (!(stack.getItem()
                instanceof SphereItem sphere)) {
            return false;
        }

        int capacity = sphere.getCapacity();

        if (slot < MARK_SLOT_START) {
            return capacity == CHIP_CAPACITY;
        }

        if (slot < BROAM_SLOT_START) {
            return capacity == MARK_CAPACITY;
        }

        return capacity == BROAM_CAPACITY;
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