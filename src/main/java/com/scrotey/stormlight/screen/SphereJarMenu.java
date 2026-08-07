package com.scrotey.stormlight.screen;

import com.scrotey.stormlight.block.entity.SphereJarBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SphereJarMenu extends AbstractContainerMenu {
    public static final int CHIP_TAB = 0;
    public static final int MARK_TAB = 1;
    public static final int BROAM_TAB = 2;

    private static final int JAR_SLOT_COUNT =
            SphereJarBlockEntity.TOTAL_SLOTS;

    private static final int PLAYER_SLOT_START =
            JAR_SLOT_COUNT;

    private static final int PLAYER_SLOT_END =
            PLAYER_SLOT_START + Inventory.INVENTORY_SIZE;

    private static final int JAR_START_X = 53;
    private static final int JAR_START_Y = 44;

    private static final int PLAYER_START_X = 8;
    private static final int PLAYER_START_Y = 139;

    private final Container container;
    private int selectedTab = CHIP_TAB;

    // Client-side constructor
    public SphereJarMenu(
            int containerId,
            Inventory inventory
    ) {
        this(
                containerId,
                inventory,
                new SimpleContainer(JAR_SLOT_COUNT)
        );
    }

    // Server-side constructor
    public SphereJarMenu(
            int containerId,
            Inventory inventory,
            Container container
    ) {
        super(
                ModMenuTypes.SPHERE_JAR,
                containerId
        );

        checkContainerSize(
                container,
                JAR_SLOT_COUNT
        );

        this.container = container;
        container.startOpen(inventory.player);

        addJarSlots();
        addStandardInventorySlots(
                inventory,
                PLAYER_START_X,
                PLAYER_START_Y
        );
    }

    private void addJarSlots() {
        for (int tab = 0; tab < 3; tab++) {
            final int tabIndex = tab;
            int compartmentStart =
                    tab * SphereJarBlockEntity
                            .SLOTS_PER_COMPARTMENT;

            for (int row = 0; row < 4; row++) {
                for (int column = 0;
                     column < 4;
                     column++) {

                    int compartmentSlot =
                            column + row * 4;

                    int actualSlot =
                            compartmentStart
                                    + compartmentSlot;

                    addSlot(new Slot(
                            container,
                            actualSlot,
                            JAR_START_X + column * 18,
                            JAR_START_Y + row * 18
                    ) {
                        @Override
                        public boolean isActive() {
                            return selectedTab == tabIndex;
                        }

                        @Override
                        public boolean mayPlace(
                                ItemStack stack
                        ) {
                            return container.canPlaceItem(
                                    actualSlot,
                                    stack
                            );
                        }

                        @Override
                        public boolean mayPickup(
                                Player player
                        ) {
                            return isActive()
                                    && super.mayPickup(player);
                        }
                    });
                }
            }
        }
    }

    public int getSelectedTab() {
        return selectedTab;
    }

    @Override
    public boolean clickMenuButton(
            Player player,
            int buttonId
    ) {
        if (buttonId < CHIP_TAB
                || buttonId > BROAM_TAB) {
            return false;
        }

        selectedTab = buttonId;
        return true;
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

        if (slotIndex < JAR_SLOT_COUNT) {
            if (!moveItemStackTo(
                    stack,
                    PLAYER_SLOT_START,
                    PLAYER_SLOT_END,
                    true
            )) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!moveItemStackTo(
                    stack,
                    0,
                    JAR_SLOT_COUNT,
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

        return original;
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