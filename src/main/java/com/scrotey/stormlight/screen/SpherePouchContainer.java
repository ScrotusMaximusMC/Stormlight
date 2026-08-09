package com.scrotey.stormlight.screen;

import com.scrotey.stormlight.attachment.ModAttachments;
import com.scrotey.stormlight.item.SpherePouchItem;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

public final class SpherePouchContainer
        extends SimpleContainer {

    private final ServerPlayer owner;
    private boolean loading;
    private ItemContainerContents lastLoadedContents =
            ItemContainerContents.EMPTY;

    public SpherePouchContainer(
            ServerPlayer owner
    ) {
        super(SpherePouchItem.SLOT_COUNT);

        this.owner = owner;

        refreshFromPlayerStorage();
    }

    public void refreshFromPlayerStorage() {
        ItemContainerContents currentContents =
                ModAttachments.getSphereContents(owner);

        var storedItems =
                ModAttachments.getSphereItems(
                        owner
                );

        loading = true;

        for (int slot = 0;
             slot < SpherePouchItem.SLOT_COUNT;
             slot++) {

            setItem(
                    slot,
                    storedItems.get(slot)
            );
        }

        loading = false;
        lastLoadedContents = currentContents;
    }

    private void refreshIfPlayerStorageChanged() {
        if (!loading
                && !lastLoadedContents.equals(
                ModAttachments.getSphereContents(owner)
        )) {

            refreshFromPlayerStorage();
        }
    }

    private void saveToPlayerStorage() {
        ModAttachments.setSphereItems(
                owner,
                getItems()
        );

        lastLoadedContents =
                ModAttachments.getSphereContents(owner);
    }

    @Override
    public ItemStack getItem(int slot) {
        refreshIfPlayerStorageChanged();
        return super.getItem(slot);
    }

    @Override
    public ItemStack removeItem(
            int slot,
            int amount
    ) {
        refreshIfPlayerStorageChanged();
        return super.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        refreshIfPlayerStorageChanged();
        return super.removeItemNoUpdate(slot);
    }

    @Override
    public void setChanged() {
        super.setChanged();

        if (!loading) {
            saveToPlayerStorage();
        }
    }

    @Override
    public boolean stillValid(
            Player player
    ) {
        return player == owner
                && owner.isAlive()
                && ModAttachments
                .hasEquippedPouch(owner);
    }
}
