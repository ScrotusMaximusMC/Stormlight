package com.scrotey.stormlight.block.entity;

import com.scrotey.stormlight.block.custom.SphereLanternBlock;
import com.scrotey.stormlight.item.ModItems;
import com.scrotey.stormlight.item.SphereItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class SphereLanternBlockEntity
        extends BlockEntity
        implements ImplementedContainer {

    private final NonNullList<ItemStack> items =
            NonNullList.withSize(
                    1,
                    ItemStack.EMPTY
            );

    public SphereLanternBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                ModBlockEntities.SPHERE_LANTERN_BLOCK_ENTITY,
                pos,
                state
        );
    }

    public boolean insertSphere(
            ItemStack sourceStack
    ) {
        if (!items.getFirst().isEmpty()
                || !(sourceStack.getItem()
                instanceof SphereItem)) {

            return false;
        }

        setItem(
                0,
                sourceStack.copyWithCount(1)
        );

        return true;
    }

    public ItemStack removeSphere() {
        ItemStack result =
                removeItemNoUpdate(0);

        if (!result.isEmpty()) {
            setChanged();
        }

        return result;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        updateLanternState();
    }

    private void updateLanternState() {
        if (level == null || level.isClientSide()) {
            return;
        }

        BlockState state =
                getBlockState();

        if (!state.hasProperty(
                SphereLanternBlock.LIGHT_LEVEL
        )) {
            return;
        }

        ItemStack sphereStack =
                items.getFirst();

        int newLightLevel =
                lightLevelForSphere(sphereStack);

        int newGemstone =
                gemstoneForSphere(sphereStack);

        if (state.getValue(
                SphereLanternBlock.LIGHT_LEVEL
        ) == newLightLevel
                && state.getValue(
                SphereLanternBlock.GEMSTONE
        ) == newGemstone) {

            return;
        }

        level.setBlockAndUpdate(
                worldPosition,
                state.setValue(
                                SphereLanternBlock.LIGHT_LEVEL,
                                newLightLevel
                        )
                        .setValue(
                                SphereLanternBlock.GEMSTONE,
                                newGemstone
                        )
        );
    }

    private static int lightLevelForSphere(
            ItemStack stack
    ) {
        if (!(stack.getItem()
                instanceof SphereItem sphere)) {

            return 0;
        }

        int charge =
                sphere.getCharge(stack);

        if (charge <= 0) {
            return 0;
        }

        double percentage =
                (double) charge
                        / sphere.getCapacity();

        if (percentage <= 0.25) {
            return 6;
        }

        if (percentage <= 0.50) {
            return 9;
        }

        if (percentage <= 0.75) {
            return 12;
        }

        return 15;
    }

    private static int gemstoneForSphere(
            ItemStack stack
    ) {
        Item item =
                stack.getItem();

        if (item == ModItems.DIAMOND_CHIP
                || item == ModItems.DIAMOND_MARK
                || item == ModItems.DIAMOND_BROAM) {

            return 1;
        }

        if (item == ModItems.GARNET_CHIP
                || item == ModItems.GARNET_MARK
                || item == ModItems.GARNET_BROAM) {

            return 2;
        }

        if (item == ModItems.RUBY_CHIP
                || item == ModItems.RUBY_MARK
                || item == ModItems.RUBY_BROAM) {

            return 3;
        }

        if (item == ModItems.SAPPHIRE_CHIP
                || item == ModItems.SAPPHIRE_MARK
                || item == ModItems.SAPPHIRE_BROAM) {

            return 4;
        }

        if (item == ModItems.EMERALD_CHIP
                || item == ModItems.EMERALD_MARK
                || item == ModItems.EMERALD_BROAM) {

            return 5;
        }

        return 0;
    }

    /*
     * Drop the stored sphere whenever the block entity is removed.
     * This also covers the lantern losing its wall/ceiling support.
     */
    @Override
    public void preRemoveSideEffects(
            BlockPos pos,
            BlockState state
    ) {
        if (level != null
                && !level.isClientSide()) {

            ItemStack storedSphere =
                    removeItemNoUpdate(0);

            if (!storedSphere.isEmpty()) {
                Block.popResource(
                        level,
                        pos,
                        storedSphere
                );
            }
        }

        super.preRemoveSideEffects(
                pos,
                state
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
        return slot == 0
                && stack.getItem()
                instanceof SphereItem;
    }

    @Override
    protected void loadAdditional(
            ValueInput input
    ) {
        super.loadAdditional(input);

        ContainerHelper.loadAllItems(
                input,
                items
        );
    }

    @Override
    protected void saveAdditional(
            ValueOutput output
    ) {
        ContainerHelper.saveAllItems(
                output,
                items
        );

        super.saveAdditional(output);
    }
}