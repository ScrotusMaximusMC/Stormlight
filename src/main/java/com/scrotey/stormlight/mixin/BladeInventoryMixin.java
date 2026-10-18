package com.scrotey.stormlight.mixin;

import com.scrotey.stormlight.item.ModItems;
import com.scrotey.stormlight.spren.BladeInventory;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.server.level.ServerPlayer;
import com.scrotey.stormlight.spren.ShardbladeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
public abstract class BladeInventoryMixin implements BladeInventory {
    @Shadow @Final public Player player;
    @Unique private boolean stormlight$bladeSelected;
    @Unique private boolean stormlight$bladeVisible;
    @Unique private ItemStack stormlight$bladeStack;

    @Override
    public void stormlight$setBladeSelected(boolean selected) {
        stormlight$bladeSelected = selected;
        if (!selected) stormlight$bladeVisible = false;
    }

    @Override
    public void stormlight$setBladeVisible(boolean visible) {
        stormlight$bladeVisible = visible;
    }

    @Inject(method = "getSelectedItem", at = @At("HEAD"), cancellable = true)
    private void stormlight$virtualBlade(CallbackInfoReturnable<ItemStack> cir) {
        if (!stormlight$bladeSelected) return;
        if (!stormlight$bladeVisible) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        if (stormlight$bladeStack == null) {
            stormlight$bladeStack = new ItemStack(ModItems.SHARDBLADE);
            ItemAttributeModifiers attributes = stormlight$bladeStack.get(
                    DataComponents.ATTRIBUTE_MODIFIERS);
            attributes = attributes.withModifierAdded(
                    Attributes.BLOCK_INTERACTION_RANGE,
                    new AttributeModifier(Identifier.fromNamespaceAndPath(
                            "stormlight", "shardblade_block_reach"),
                            Player.DEFAULT_BLOCK_INTERACTION_RANGE,
                            AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
            attributes = attributes.withModifierAdded(
                    Attributes.ENTITY_INTERACTION_RANGE,
                    new AttributeModifier(Identifier.fromNamespaceAndPath(
                            "stormlight", "shardblade_entity_reach"),
                            Player.DEFAULT_ENTITY_INTERACTION_RANGE,
                            AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
            stormlight$bladeStack.set(
                    DataComponents.ATTRIBUTE_MODIFIERS, attributes);
            var enchantments = player.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT);
            stormlight$bladeStack.enchant(
                    enchantments.getOrThrow(Enchantments.SHARPNESS), 5);
            stormlight$bladeStack.enchant(
                    enchantments.getOrThrow(Enchantments.SILK_TOUCH), 1);
        }
        cir.setReturnValue(stormlight$bladeStack);
    }

    @Inject(method = "setSelectedSlot", at = @At("HEAD"))
    private void stormlight$leaveBladeForNormalSlot(int slot,
            CallbackInfo ci) {
        if (stormlight$bladeSelected
                && player instanceof ServerPlayer serverPlayer) {
            ShardbladeManager.handleInput(serverPlayer, 2);
        }
    }

    @Inject(method = "removeFromSelected", at = @At("HEAD"),
            cancellable = true)
    private void stormlight$dismissInsteadOfDrop(boolean fullStack,
            CallbackInfoReturnable<ItemStack> cir) {
        if (!stormlight$bladeSelected) return;
        if (player instanceof ServerPlayer serverPlayer) {
            ShardbladeManager.dismiss(serverPlayer);
        }
        cir.setReturnValue(ItemStack.EMPTY);
    }

    @Inject(method = "setSelectedItem", at = @At("HEAD"),
            cancellable = true)
    private void stormlight$protectRealHotbarItem(ItemStack stack,
            CallbackInfoReturnable<ItemStack> cir) {
        if (stormlight$bladeSelected) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
