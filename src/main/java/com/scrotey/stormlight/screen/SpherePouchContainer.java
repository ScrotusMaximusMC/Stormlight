package com.scrotey.stormlight.screen;

import com.scrotey.stormlight.attachment.ModAttachments;
import com.scrotey.stormlight.item.SpherePouchItem;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;

public final class SpherePouchContainer
        extends SimpleContainer {

    private final ServerPlayer owner;
    private boolean loading;

    public SpherePouchContainer(
            ServerPlayer owner
    ) {
        super(SpherePouchItem.SLOT_COUNT);

        this.owner = owner;

        refreshFromPlayerStorage();
    }

    public void refreshFromPlayerStorage() {
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
    }

    private void saveToPlayerStorage() {
        ModAttachments.setSphereItems(
                owner,
                getItems()
        );
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