package com.scrotey.stormlight.screen;

import com.scrotey.stormlight.attachment.ModAttachments;
import com.scrotey.stormlight.block.entity.SphereJarBlockEntity;
import com.scrotey.stormlight.item.SphereItem;
import com.scrotey.stormlight.item.SpherePouchItem;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SphereJarMenu
        extends AbstractContainerMenu {

    public static final int CHIP_TAB = 0;
    public static final int MARK_TAB = 1;
    public static final int BROAM_TAB = 2;

    private static final int JAR_SLOT_COUNT =
            SphereJarBlockEntity.TOTAL_SLOTS;

    private static final int LOWER_SLOT_START =
            JAR_SLOT_COUNT;

    private static final int JAR_START_X = 53;
    private static final int JAR_START_Y = 44;

    private static final int PLAYER_START_X = 8;
    private static final int PLAYER_START_Y = 139;

    private static final int POUCH_START_X = 53;
    private static final int POUCH_START_Y = 139;

    private final Container jarContainer;
    private final Container lowerContainer;
    private final boolean showingPouch;

    private int selectedTab = CHIP_TAB;

    // Client-side constructor
    public SphereJarMenu(
            int containerId,
            Inventory inventory
    ) {
        this(
                containerId,
                inventory,
                new SimpleContainer(
                        JAR_SLOT_COUNT
                )
        );
    }

    // Server-side constructor
    public SphereJarMenu(
            int containerId,
            Inventory inventory,
            Container jarContainer
    ) {
        super(
                ModMenuTypes.SPHERE_JAR,
                containerId
        );

        this.showingPouch =
                ModAttachments.hasEquippedPouch(
                        inventory.player
                );

        this.jarContainer = jarContainer;
        this.lowerContainer =
                createLowerContainer(
                        inventory,
                        showingPouch
                );

        checkContainerSize(
                jarContainer,
                JAR_SLOT_COUNT
        );

        if (showingPouch) {
            checkContainerSize(
                    lowerContainer,
                    SpherePouchItem.SLOT_COUNT
            );
        }

        jarContainer.startOpen(
                inventory.player
        );

        lowerContainer.startOpen(
                inventory.player
        );

        addJarSlots();

        if (showingPouch) {
            addPouchSlots();
        } else {
            addStandardInventorySlots(
                    inventory,
                    PLAYER_START_X,
                    PLAYER_START_Y
            );
        }
    }

    private static Container createLowerContainer(
            Inventory inventory,
            boolean showingPouch
    ) {
        if (!showingPouch) {
            return inventory;
        }

        if (inventory.player
                instanceof ServerPlayer serverPlayer) {

            return new SpherePouchContainer(
                    serverPlayer
            );
        }

        /*
         * Client-side placeholder. The server synchronises the
         * actual pouch stacks into these menu slots.
         */
        return new SimpleContainer(
                SpherePouchItem.SLOT_COUNT
        );
    }

    private void addJarSlots() {
        for (int tab = 0; tab < 3; tab++) {
            final int tabIndex = tab;

            int compartmentStart =
                    tab
                            * SphereJarBlockEntity
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

                    addSlot(
                            new Slot(
                                    jarContainer,
                                    actualSlot,
                                    JAR_START_X
                                            + column * 18,
                                    JAR_START_Y
                                            + row * 18
                            ) {
                                @Override
                                public boolean isActive() {
                                    return selectedTab
                                            == tabIndex;
                                }

                                @Override
                                public boolean mayPlace(
                                        ItemStack stack
                                ) {
                                    return jarContainer
                                            .canPlaceItem(
                                                    actualSlot,
                                                    stack
                                            );
                                }

                                @Override
                                public boolean mayPickup(
                                        Player player
                                ) {
                                    return isActive()
                                            && super.mayPickup(
                                            player
                                    );
                                }
                            }
                    );
                }
            }
        }
    }

    private void addPouchSlots() {
        for (int row = 0; row < 4; row++) {
            for (int column = 0;
                 column < 4;
                 column++) {

                int slotIndex =
                        column + row * 4;

                addSlot(
                        new Slot(
                                lowerContainer,
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
                        }
                );
            }
        }
    }

    public boolean isShowingPouch() {
        return showingPouch;
    }

    public int getSelectedTab() {
        return selectedTab;
    }

    public void refreshSphereStorage() {
        if (lowerContainer
                instanceof SpherePouchContainer
                pouchContainer) {

            pouchContainer
                    .refreshFromPlayerStorage();
        }
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
                    LOWER_SLOT_START,
                    slots.size(),
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
            slot.setByPlayer(
                    ItemStack.EMPTY
            );
        } else {
            slot.setChanged();
        }

        lowerContainer.setChanged();
        jarContainer.setChanged();

        return original;
    }

    @Override
    public boolean stillValid(
            Player player
    ) {
        if (!jarContainer.stillValid(player)) {
            return false;
        }

        return !showingPouch
                || lowerContainer.stillValid(player);
    }

    @Override
    public void removed(
            Player player
    ) {
        super.removed(player);

        jarContainer.stopOpen(player);
        lowerContainer.stopOpen(player);
    }
}