package com.scrotey.stormlight.client.spren;

import com.scrotey.stormlight.item.ModItems;
import com.scrotey.stormlight.network.ShardbladeInputPayload;
import com.scrotey.stormlight.network.ShardbladeStatePayload;
import com.scrotey.stormlight.spren.BladeInventory;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Local presentation and selection for the bound tenth hotbar slot. */
public final class ClientShardblade {
    private static final DustParticleOptions BLADE_MIST =
            new DustParticleOptions(0xADB2BB, 0.65F);
    private static boolean summoned;
    private static boolean selected;
    private static boolean ready;
    private static int mistTicks;
    private static LocalPlayer owner;

    private ClientShardblade() {
    }

    public static void update(Minecraft client,
            ShardbladeStatePayload state) {
        if (client.player == null) return;
        owner = client.player;
        boolean startingDraw = state.selected() && !selected;
        summoned = state.summoned();
        selected = state.selected();
        ready = state.ready();
        if (startingDraw) mistTicks = 0;
        if (!selected) mistTicks = 0;
        updateInventory();
    }

    public static boolean isSummoned() {
        return summoned;
    }

    public static boolean isSelected() {
        return selected;
    }

    public static void summon() {
        if (!summoned) {
            ClientPlayNetworking.send(new ShardbladeInputPayload(0));
        }
    }

    public static void dismiss() {
        if (!summoned) return;
        summoned = false;
        selected = false;
        ready = false;
        mistTicks = 0;
        updateInventory();
        ClientPlayNetworking.send(new ShardbladeInputPayload(2));
    }

    public static void tick(Minecraft client) {
        if (client.player == null || client.player != owner) {
            summoned = false;
            selected = false;
            ready = false;
            owner = client.player;
            return;
        }
        if (selected && !ready) {
            mistTicks++;
            Vec3 look = client.player.getLookAngle();
            double yaw = Math.toRadians(client.player.getYRot());
            Vec3 right = new Vec3(-Math.cos(yaw), 0,
                    -Math.sin(yaw));
            if (client.player.getMainArm() == HumanoidArm.LEFT) {
                right = right.scale(-1);
            }
            Vec3 hand = client.player.getEyePosition()
                    .add(look.scale(0.52))
                    .add(right.scale(0.32))
                    .add(0, -0.34, 0);
            Vec3 tip = hand.add(look.scale(1.25)).add(0, 0.48, 0);
            double formed = Math.min(1.0, mistTicks / 14.0);
            Vec3 towardHand = hand.subtract(tip).normalize().scale(0.025);
            // The condensation starts at the tip and grows toward the grip.
            for (int i = 0; i < 5; i++) {
                Vec3 point = tip.lerp(hand, formed * i / 4.0)
                        .add(client.player.getRandom().nextGaussian() * 0.035,
                                client.player.getRandom().nextGaussian() * 0.035,
                                client.player.getRandom().nextGaussian() * 0.035);
                client.level.addParticle(BLADE_MIST,
                        point.x, point.y, point.z,
                        towardHand.x, towardHand.y, towardHand.z);
            }
            if (mistTicks % 3 == 0) {
                Vec3 point = tip.lerp(hand, formed);
                client.level.addParticle(ParticleTypes.CLOUD,
                        point.x, point.y, point.z,
                        towardHand.x, towardHand.y + 0.01, towardHand.z);
            }
        }
    }

    public static void render(GuiGraphicsExtractor graphics,
            net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (!summoned || client.player == null) return;
        int centre = graphics.guiWidth() / 2;
        int x = client.player.getMainArm() == HumanoidArm.RIGHT
                ? centre + 94 : centre - 116;
        int y = graphics.guiHeight() - 22;
        graphics.fill(x, y, x + 22, y + 22, 0xD4102430);
        graphics.outline(x, y, 22, 22, 0xFF78DDEB);
        if (selected) {
            graphics.outline(x - 2, y - 2, 26, 26, 0xFFDAFAFF);
        }
        graphics.item(new ItemStack(ModItems.SHARDBLADE), x + 3, y + 3);
    }

    private static void updateInventory() {
        if (owner == null) return;
        BladeInventory inventory = (BladeInventory) owner.getInventory();
        inventory.stormlight$setBladeSelected(selected);
        inventory.stormlight$setBladeVisible(ready);
    }
}
