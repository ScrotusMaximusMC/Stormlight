package com.scrotey.stormlight.screen;

import com.scrotey.stormlight.attachment.ModAttachments;
import com.scrotey.stormlight.item.SpherePouchItem;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

public final class SpherePouchContainer
        extends SimpleContainer {

    private final ServerPlayer owner;
    private boolean loading;

    public SpherePouchContainer(ServerPlayer owner) {
        super(SpherePouchItem.SLOT_COUNT);
        this.owner = owner;

        loadFromEquippedPouch();
    }

    private void loadFromEquippedPouch() {
        ItemStack pouch =
                ModAttachments.getEquippedPouch(owner);

        if (!(pouch.getItem()
                instanceof SpherePouchItem pouchItem)) {
            return;
        }

        NonNullList<ItemStack> loadedItems =
                NonNullList.withSize(
                        SpherePouchItem.SLOT_COUNT,
                        ItemStack.EMPTY
                );

        pouchItem.getContents(pouch)
                .copyInto(loadedItems);

        loading = true;

        for (int slot = 0;
             slot < SpherePouchItem.SLOT_COUNT;
             slot++) {

            setItem(
                    slot,
                    loadedItems.get(slot)
            );
        }

        loading = false;
    }

    private void saveToEquippedPouch() {
        ItemStack pouch =
                ModAttachments.getEquippedPouch(owner);

        if (!(pouch.getItem()
                instanceof SpherePouchItem pouchItem)) {
            return;
        }

        List<ItemStack> savedItems =
                new ArrayList<>(
                        SpherePouchItem.SLOT_COUNT
                );

        for (int slot = 0;
             slot < SpherePouchItem.SLOT_COUNT;
             slot++) {

            savedItems.add(
                    getItem(slot).copy()
            );
        }

        pouchItem.setContents(
                pouch,
                ItemContainerContents.fromItems(
                        savedItems
                )
        );

        ModAttachments.setEquippedPouch(
                owner,
                pouch
        );
    }

    @Override
    public void setChanged() {
        super.setChanged();

        if (!loading) {
            saveToEquippedPouch();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return player == owner
                && owner.isAlive()
                && ModAttachments.hasEquippedPouch(owner);
    }

}