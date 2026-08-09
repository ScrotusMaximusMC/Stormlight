package com.scrotey.stormlight.screen;

/**
 * Implemented by the vanilla player inventory menu through a mixin.
 * It lets attachment updates refresh the server-side pouch slots
 * without replacing the player's current menu.
 */
public interface SpherePouchInventoryAccess {
    void stormlight$refreshSphereStorage();
}
