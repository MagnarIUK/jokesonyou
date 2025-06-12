package com.magnariuk.abstracts;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;

public interface PlayerPickupItemCallback {
    Event<PlayerPickupItemCallback> EVENT = EventFactory.createArrayBacked(PlayerPickupItemCallback.class,
            (listeners) -> (player, entity, amount) -> {
                for (PlayerPickupItemCallback event : listeners) {
                    ActionResult result = event.interact(player, entity, amount);
                    if (result != ActionResult.PASS) return result;
                }
                return ActionResult.PASS;
            }
    );

    ActionResult interact(PlayerInventory playerPickingUpItems, int slot, ItemStack entityBeingPickedUp);
}