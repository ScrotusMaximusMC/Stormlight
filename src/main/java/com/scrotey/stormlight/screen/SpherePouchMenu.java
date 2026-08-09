package com.scrotey.stormlight.screen;

import com.scrotey.stormlight.item.SphereItem;
import com.scrotey.stormlight.item.SpherePouchItem;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SpherePouchMenu
        extends AbstractContainerMenu {

    private static final int POUCH_SLOT_COUNT =
            SpherePouchItem.SLOT_COUNT;

    private static final int PLAYER_SLOT_START =
            POUCH_SLOT_COUNT;

    private static final int PLAYER_SLOT_END =
            PLAYER_SLOT_START
                    + Inventory.INVENTORY_SIZE;

    private static final int POUCH_START_X = 53;
    private static final int POUCH_START_Y = 18;

    private static final int PLAYER_START_X = 8;
    private static final int PLAYER_START_Y = 108;

    private final Container container;

    // Client-side constructor
    public SpherePouchMenu(
            int containerId,
            Inventory inventory
    ) {
        this(
                containerId,
                inventory,
                new SimpleContainer(
                        POUCH_SLOT_COUNT
                )
        );
    }

    // Server-side constructor
    public SpherePouchMenu(
            int containerId,
            Inventory inventory,
            Container container
    ) {
        super(
                ModMenuTypes.SPHERE_POUCH,
                containerId
        );

        checkContainerSize(
                container,
                POUCH_SLOT_COUNT
        );

        this.container = container;
        container.startOpen(inventory.player);

        addPouchSlots();

        addStandardInventorySlots(
                inventory,
                PLAYER_START_X,
                PLAYER_START_Y
        );
    }

    private void addPouchSlots() {
        for (int row = 0; row < 4; row++) {
            for (int column = 0;
                 column < 4;
                 column++) {

                int slotIndex =
                        column + row * 4;

                addSlot(new Slot(
                        container,
                        slotIndex,
                        POUCH_START_X
                                + column * 18,
                        POUCH_START_Y
                                + row * 18
                ) {
                    @Override
                    public boolean mayPlace(
                            ItemStack stack
                    ) {
                        return stack.getItem()
                                instanceof SphereItem;
                    }

                    @Override
                    public int getMaxStackSize() {
                        return 1;
                    }
                });
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(
            Player player,
            int slotIndex
    ) {
        Slot slot = slots.get(slotIndex);

        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (slotIndex < POUCH_SLOT_COUNT) {
            if (!moveItemStackTo(
                    stack,
                    PLAYER_SLOT_START,
                    PLAYER_SLOT_END,
                    true
            )) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!(stack.getItem()
                    instanceof SphereItem)) {
                return ItemStack.EMPTY;
            }

            if (!moveItemStackTo(
                    stack,
                    0,
                    POUCH_SLOT_COUNT,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        container.setChanged();
        return original;
    }

    public void refreshSphereStorage() {
        if (container
                instanceof SpherePouchContainer
                sphereContainer) {

            sphereContainer
                    .refreshFromPlayerStorage();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}