package com.scrotey.stormlight.client.spren;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public final class SprenRenderState extends LivingEntityRenderState {
    /** Current entity motion, copied during render-state extraction. */
    public float motionX;
    public float motionY;
    public float motionZ;

    /** Horizontal movement speed in blocks/tick. */
    public float horizontalSpeed;

    /** Stable offset used to desynchronise procedural personality animation. */
    public float whimsyOffset;

    /** Alternates the direction of occasional ballerina twirls. */
    public float twirlDirection = 1.0F;
}
