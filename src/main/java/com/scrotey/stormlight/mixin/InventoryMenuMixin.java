package com.scrotey.stormlight.mixin;

import com.scrotey.stormlight.attachment.ModAttachments;
import com.scrotey.stormlight.item.SphereItem;
import com.scrotey.stormlight.item.SpherePouchItem;
import com.scrotey.stormlight.screen.SpherePouchContainer;
import com.scrotey.stormlight.screen.SpherePouchInventoryAccess;
import com.scrotey.stormlight.screen.SpherePouchInventoryLayout;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin
        extends AbstractCraftingMenu
        implements SpherePouchInventoryAccess {

    @Unique
    private static final int STORMLIGHT$VANILLA_SLOT_COUNT = 46;

    @Unique
    private static final int STORMLIGHT$PLAYER_SLOT_START = 9;

    @Unique
    private static final int STORMLIGHT$PLAYER_SLOT_END = 45;

    @Unique
    private static final int STORMLIGHT$POUCH_SLOT_START =
            STORMLIGHT$VANILLA_SLOT_COUNT;

    @Unique
    private static final int STORMLIGHT$POUCH_SLOT_END =
            STORMLIGHT$POUCH_SLOT_START
                    + SpherePouchItem.SLOT_COUNT;

    @Unique
    private Container stormlight$pouchContainer;

    protected InventoryMenuMixin(
            MenuType<?> menuType,
            int containerId,
            int width,
            int height
    ) {
        super(menuType, containerId, width, height);
    }

    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    private void stormlight$addSpherePouchSlots(
            Inventory inventory,
            boolean active,
            Player owner,
            CallbackInfo callbackInfo
    ) {
        stormlight$pouchContainer =
                owner instanceof ServerPlayer serverPlayer
                        ? new SpherePouchContainer(serverPlayer)
                        : new SimpleContainer(
                                SpherePouchItem.SLOT_COUNT
                        );

        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                int slotIndex = column + row * 4;

                addSlot(
                        new Slot(
                                stormlight$pouchContainer,
                                slotIndex,
                                SpherePouchInventoryLayout
                                        .SLOT_START_X
                                        + column * 18,
                                SpherePouchInventoryLayout
                                        .SLOT_START_Y
                                        + row * 18
                        ) {
                            @Override
                            public boolean isActive() {
                                return ModAttachments
                                        .hasEquippedPouch(owner);
                            }

                            @Override
                            public boolean mayPlace(
                                    ItemStack stack
                            ) {
                                return isActive()
                                        && stack.getItem()
                                        instanceof SphereItem;
                            }

                            @Override
                            public boolean mayPickup(
                                    Player player
                            ) {
                                return isActive()
                                        && super.mayPickup(player);
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

    @Inject(
            method = "quickMoveStack",
            at = @At("HEAD"),
            cancellable = true
    )
    private void stormlight$quickMoveSpherePouchStack(
            Player player,
            int slotIndex,
            CallbackInfoReturnable<ItemStack> callbackInfo
    ) {
        boolean slotIsInPouch =
                slotIndex >= STORMLIGHT$POUCH_SLOT_START
                        && slotIndex < STORMLIGHT$POUCH_SLOT_END;

        if (slotIsInPouch
                && !ModAttachments.hasEquippedPouch(player)) {

            callbackInfo.setReturnValue(ItemStack.EMPTY);
            return;
        }

        boolean movingFromPouch = slotIsInPouch;

        boolean equippingSpherePouch =
                slotIndex >= STORMLIGHT$PLAYER_SLOT_START
                        && slotIndex < STORMLIGHT$PLAYER_SLOT_END
                        && slots.get(slotIndex).getItem()
                        .getItem() instanceof SpherePouchItem;

        if (equippingSpherePouch) {
            stormlight$equipSpherePouch(
                    player,
                    slotIndex,
                    callbackInfo
            );

            return;
        }

        boolean movingSphereIntoPouch =
                slotIndex >= STORMLIGHT$PLAYER_SLOT_START
                        && slotIndex
                        < STORMLIGHT$VANILLA_SLOT_COUNT
                        && ModAttachments.hasEquippedPouch(player)
                        && slots.get(slotIndex).getItem()
                        .getItem() instanceof SphereItem;

        if (!movingFromPouch && !movingSphereIntoPouch) {
            return;
        }

        Slot slot = slots.get(slotIndex);

        if (!slot.hasItem()) {
            callbackInfo.setReturnValue(ItemStack.EMPTY);
            return;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        boolean moved;

        if (movingFromPouch) {
            moved = moveItemStackTo(
                    stack,
                    STORMLIGHT$PLAYER_SLOT_START,
                    STORMLIGHT$PLAYER_SLOT_END,
                    true
            );
        } else {
            moved = moveItemStackTo(
                    stack,
                    STORMLIGHT$POUCH_SLOT_START,
                    STORMLIGHT$POUCH_SLOT_END,
                    false
            );
        }

        if (!moved) {
            callbackInfo.setReturnValue(ItemStack.EMPTY);
            return;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY, original);
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == original.getCount()) {
            callbackInfo.setReturnValue(ItemStack.EMPTY);
            return;
        }

        slot.onTake(player, stack);
        stormlight$pouchContainer.setChanged();

        callbackInfo.setReturnValue(original);
    }

    @Unique
    private void stormlight$equipSpherePouch(
            Player player,
            int slotIndex,
            CallbackInfoReturnable<ItemStack> callbackInfo
    ) {
        Slot slot = slots.get(slotIndex);

        if (!slot.hasItem()) {
            callbackInfo.setReturnValue(ItemStack.EMPTY);
            return;
        }

        ItemStack original = slot.getItem().copy();
        ItemStack pouchToEquip = original.copy();
        pouchToEquip.setCount(1);

        ItemStack previouslyEquipped =
                ModAttachments.getEquippedPouch(player);

        ModAttachments.setEquippedPouch(
                player,
                pouchToEquip
        );

        if (previouslyEquipped.isEmpty()) {
            slot.setByPlayer(
                    ItemStack.EMPTY,
                    original
            );

            player.sendOverlayMessage(
                    Component.translatable(
                            "message.stormlight."
                                    + "sphere_pouch.equipped"
                    )
            );
        } else {
            slot.setByPlayer(
                    previouslyEquipped,
                    original
            );

            player.sendOverlayMessage(
                    Component.translatable(
                            "message.stormlight."
                                    + "sphere_pouch.swapped"
                    )
            );
        }

        slot.setChanged();
        stormlight$pouchContainer.setChanged();
        broadcastChanges();

        callbackInfo.setReturnValue(original);
    }

    @Override
    public void stormlight$refreshSphereStorage() {
        if (stormlight$pouchContainer
                instanceof SpherePouchContainer pouchContainer) {

            pouchContainer.refreshFromPlayerStorage();
        }
    }
}
